package com.dremioplatform.workload.service;

import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.workload.domain.WlmQueueRule;
import com.dremioplatform.workload.repository.EngineRepository;
import com.dremioplatform.workload.repository.WlmQueueRuleRepository;
import com.dremioplatform.workload.service.dto.CreateWlmQueueRuleRequest;
import com.dremioplatform.workload.service.dto.WlmQueueRuleResponse;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class WlmQueueRuleService {

    @Inject WlmQueueRuleRepository ruleRepository;
    @Inject EngineRepository engineRepository;

    @WithTransaction
    public Uni<WlmQueueRuleResponse> create(CreateWlmQueueRuleRequest request) {
        return engineRepository.findById(request.engineId())
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "WORKLOAD-404-ENGINE-NOT-FOUND", "Engine", request.engineId()))
                .chain(engine -> {
                    WlmQueueRule rule = new WlmQueueRule();
                    rule.id = UUID.randomUUID();
                    rule.name = request.name();
                    rule.engineId = request.engineId();
                    rule.priority = request.priority();
                    rule.queryTagPattern = request.queryTagPattern();
                    rule.maxConcurrentQueries = request.maxConcurrentQueries() > 0 ? request.maxConcurrentQueries() : 50;
                    rule.queueTimeoutSeconds = request.queueTimeoutSeconds() > 0 ? request.queueTimeoutSeconds() : 300;
                    return ruleRepository.persist(rule).map(WlmQueueRuleResponse::from);
                });
    }

    public Uni<List<WlmQueueRuleResponse>> listAll() {
        return ruleRepository.listAll().map(list -> list.stream().map(WlmQueueRuleResponse::from).toList());
    }

    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return ruleRepository.findById(id)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "WORKLOAD-404-RULE-NOT-FOUND", "WlmQueueRule", id))
                .chain(ruleRepository::delete);
    }

    /**
     * The routing decision the not-yet-built job-submission hook calls (plan
     * Section 5.4: "intercepting job submission ... and routing to the right engine
     * pool"). Rules are evaluated in descending priority order (the repository query
     * already sorts this way); the first whose {@code queryTagPattern} glob-matches
     * `queryTag` wins. Returns empty if no rule matches — the caller's responsibility
     * to fall back to a default engine in that case, not this service's.
     */
    public Uni<Optional<UUID>> resolveEngineFor(String queryTag) {
        return ruleRepository.findEnabledOrderedByPriority()
                .map(rules -> rules.stream()
                        .filter(rule -> globToRegex(rule.queryTagPattern).matcher(queryTag).matches())
                        .map(rule -> rule.engineId)
                        .findFirst());
    }

    /** Simple glob (`*` = any sequence, `?` = any single char) — deliberately not a
     *  full regex engine so rule authoring stays approachable for admins in the
     *  future UI, per the class's plan-section reference above. */
    private static Pattern globToRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                case '.', '(', ')', '+', '|', '^', '$', '@', '%' -> regex.append('\\').append(c);
                default -> regex.append(c);
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString());
    }
}
