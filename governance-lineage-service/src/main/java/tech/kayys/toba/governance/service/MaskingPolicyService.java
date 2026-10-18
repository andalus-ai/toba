package com.dremioplatform.governance.service;

import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.common.error.ValidationException;
import com.dremioplatform.governance.domain.MaskingPolicy;
import com.dremioplatform.governance.domain.MaskingRuleType;
import com.dremioplatform.governance.repository.MaskingPolicyRepository;
import com.dremioplatform.governance.service.dto.CreateMaskingPolicyRequest;
import com.dremioplatform.governance.service.dto.MaskingPolicyResponse;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class MaskingPolicyService {

    /**
     * Deliberately conservative denylist, not a full SQL parser — this service never
     * executes `expression` itself, only stores it for the dremio-adapter to splice
     * into a Calcite rewrite. Full validation that the fragment is well-formed SQL
     * happens adapter-side (it has the Calcite parser available); this check exists
     * only to reject obviously malicious content (statement injection, DDL/DML
     * keywords that have no business in a value/predicate expression) before it's
     * ever persisted.
     */
    private static final List<String> DENYLIST = List.of(
            ";", "--", "/*", "drop ", "delete ", "update ", "insert ", "exec ", "execute ", "grant ", "revoke ");

    @Inject MaskingPolicyRepository maskingPolicyRepository;

    @WithTransaction
    public Uni<MaskingPolicyResponse> create(CreateMaskingPolicyRequest request, UUID actorUserId) {
        if (request.ruleType() == MaskingRuleType.COLUMN_MASK && request.columnName() == null) {
            return Uni.createFrom().failure(new ValidationException(
                    "GOVERNANCE-400-COLUMN-REQUIRED", "columnName is required for COLUMN_MASK policies"));
        }
        String lowered = request.expression().toLowerCase(Locale.ROOT);
        for (String forbidden : DENYLIST) {
            if (lowered.contains(forbidden)) {
                return Uni.createFrom().failure(new ValidationException(
                        "GOVERNANCE-400-UNSAFE-EXPRESSION",
                        "Expression contains a disallowed token: '" + forbidden.trim() + "'"));
            }
        }

        MaskingPolicy policy = new MaskingPolicy();
        policy.id = UUID.randomUUID();
        policy.name = request.name();
        policy.scopePath = request.scopePath();
        policy.ruleType = request.ruleType();
        policy.columnName = request.columnName();
        policy.expression = request.expression();
        policy.exemptRoleIds = request.exemptRoleIds() == null ? Set.of() : Set.copyOf(request.exemptRoleIds());
        policy.createdByUserId = actorUserId;

        return maskingPolicyRepository.persist(policy).map(MaskingPolicyResponse::from);
    }

    @WithTransaction
    public Uni<MaskingPolicyResponse> disable(UUID id) {
        return maskingPolicyRepository.findById(id)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "GOVERNANCE-404-POLICY-NOT-FOUND", "MaskingPolicy", id))
                .map(policy -> {
                    policy.enabled = false;
                    return policy;
                })
                .map(MaskingPolicyResponse::from);
    }

    /** Called by the (forthcoming) gRPC lookup and, meanwhile, directly by the REST
     *  endpoint the dremio-adapter can poll/cache with a short TTL. */
    public Uni<List<MaskingPolicyResponse>> findApplicable(String requestedPath) {
        return maskingPolicyRepository.findApplicable(requestedPath)
                .map(list -> list.stream().map(MaskingPolicyResponse::from).toList());
    }

    public Uni<List<MaskingPolicyResponse>> listAll() {
        return maskingPolicyRepository.listAll()
                .map(list -> list.stream().map(MaskingPolicyResponse::from).toList());
    }
}
