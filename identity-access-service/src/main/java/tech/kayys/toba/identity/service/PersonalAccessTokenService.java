package com.dremioplatform.identity.service;

import com.dremioplatform.common.error.ConflictException;
import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.identity.domain.PersonalAccessToken;
import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.domain.TokenStatus;
import com.dremioplatform.identity.event.EventPublisher;
import com.dremioplatform.identity.event.PersonalAccessTokenEvent;
import com.dremioplatform.identity.repository.PersonalAccessTokenRepository;
import com.dremioplatform.identity.service.dto.CreatePatRequest;
import com.dremioplatform.identity.service.dto.CreatePatResponse;
import com.dremioplatform.identity.service.dto.PatResponse;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PersonalAccessTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN_PREFIX = "pat_";

    @Inject PersonalAccessTokenRepository patRepository;
    @Inject EventPublisher eventPublisher;

    @WithTransaction
    public Uni<CreatePatResponse> issue(CreatePatRequest request) {
        String rawSecret = generateRawSecret();
        String rawToken = TOKEN_PREFIX + rawSecret;
        String displayPrefix = TOKEN_PREFIX + rawSecret.substring(0, Math.min(8, rawSecret.length()));

        PersonalAccessToken pat = new PersonalAccessToken();
        pat.id = UUID.randomUUID();
        pat.principalType = request.principalType();
        pat.principalId = request.principalId();
        pat.label = request.label();
        pat.tokenHash = sha256Hex(rawToken);
        pat.displayPrefix = displayPrefix;
        pat.expiresAt = request.expiresAt();

        return patRepository.persist(pat)
                .call(p -> eventPublisher.publish(PersonalAccessTokenEvent.of(
                        p.id, p.principalType, p.principalId, PersonalAccessTokenEvent.Transition.ISSUED)))
                .map(p -> new CreatePatResponse(p.id, p.label, rawToken, p.displayPrefix, p.expiresAt));
    }

    @WithTransaction
    public Uni<Void> revokeOwned(UUID tokenId, UUID actorId) {
        return patRepository.findById(tokenId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException("IDENTITY-404-PAT-NOT-FOUND", "PersonalAccessToken", tokenId))
                .chain(pat -> {
                    if (pat.principalType != PrincipalType.USER || !actorId.equals(pat.principalId)) {
                        return Uni.createFrom().failure(new jakarta.ws.rs.WebApplicationException("Forbidden", 403));
                    }
                    if (pat.status == TokenStatus.REVOKED) {
                        return Uni.createFrom().failure(new ConflictException(
                                "IDENTITY-409-PAT-ALREADY-REVOKED", "Token is already revoked"));
                    }
                    pat.status = TokenStatus.REVOKED;
                    pat.revokedAt = Instant.now();
                    return eventPublisher.publish(PersonalAccessTokenEvent.of(
                            pat.id, pat.principalType, pat.principalId, PersonalAccessTokenEvent.Transition.REVOKED));
                });
    }

    /**
     * Called by the (forthcoming) gRPC AuthCheckService and by the API Gateway on
     * every request bearing a PAT instead of a JWT. Constant-ish time via hash
     * comparison (we compare the SHA-256 digest, never the raw secret) and checks
     * status/expiry inline so a revoked or expired token fails closed immediately.
     */
    @WithTransaction
    public Uni<PatResponse> validateAndTouch(String rawToken) {
        return patRepository.findByTokenHash(sha256Hex(rawToken))
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "IDENTITY-404-PAT-NOT-FOUND", "PersonalAccessToken", "<redacted>"))
                .chain(pat -> {
                    boolean expired = pat.expiresAt != null && pat.expiresAt.isBefore(Instant.now());
                    if (pat.status != TokenStatus.ACTIVE || expired) {
                        return Uni.createFrom().failure(new ConflictException(
                                "IDENTITY-409-PAT-NOT-ACTIVE", "Token is not active"));
                    }
                    pat.lastUsedAt = Instant.now();
                    return Uni.createFrom().item(pat);
                })
                .map(PatResponse::from);
    }

    public Uni<List<PatResponse>> listForPrincipal(PrincipalType principalType, UUID principalId) {
        return patRepository.findByPrincipal(principalType, principalId)
                .map(list -> list.stream().map(PatResponse::from).toList());
    }

    private static String generateRawSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
