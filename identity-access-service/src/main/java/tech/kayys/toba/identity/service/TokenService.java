package com.dremioplatform.identity.service;

import com.dremioplatform.common.error.UnauthorizedException;
import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.domain.User;
import com.dremioplatform.identity.repository.UserRepository;
import com.dremioplatform.identity.service.dto.AccessTokenResponse;
import com.dremioplatform.identity.service.dto.LoginRequest;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Issues and verifies the short-lived signed JWTs described in the architecture
 * plan Section 9 ("AuthN: ... Dremio coordinator trusts JWTs signed by the Identity
 * Service"). Deliberately keeps the token payload minimal — sub (principal id), a
 * custom `principalType` claim, and `upn` for display — rather than embedding
 * resolved permissions/roles in the token. Permissions are looked up fresh on every
 * request via the gRPC AuthCheckService instead of trusted from a stale claim, so a
 * role revoked mid-session takes effect immediately rather than waiting for token
 * expiry.
 */
@ApplicationScoped
public class TokenService {

    @Inject UserRepository userRepository;
    @Inject JWTParser jwtParser;

    @ConfigProperty(name = "smallrye.jwt.new-token.issuer")
    String issuer;

    @ConfigProperty(name = "smallrye.jwt.new-token.lifespan")
    long lifespanSeconds;

    /** Local-credential login. Federated (OIDC/LDAP) login is a separate flow, added
     *  alongside the OIDC callback resource — not yet built (see module README). */
    @WithTransaction
    public Uni<AccessTokenResponse> loginLocal(LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .chain(user -> {
                    boolean valid = user != null
                            && user.localPasswordHash != null
                            && user.status == User.UserStatus.ACTIVE
                            && BcryptUtil.matches(request.password(), user.localPasswordHash);
                    if (!valid) {
                        return Uni.createFrom().failure(new UnauthorizedException(
                                "IDENTITY-401-BAD-CREDENTIALS", "Invalid username or password"));
                    }
                    user.lastLoginAt = Instant.now();
                    return Uni.createFrom().item(issueFor(user.id, PrincipalType.USER, user.username));
                });
    }

    public AccessTokenResponse issueFor(UUID principalId, PrincipalType principalType, String displayName) {
        String token = Jwt.issuer(issuer)
                .subject(principalId.toString())
                .upn(displayName)
                .claim("principalType", principalType.name())
                .groups(Set.of()) // resolved dynamically via AuthCheckService, not embedded here
                .expiresIn(Duration.ofSeconds(lifespanSeconds))
                .sign();
        return AccessTokenResponse.bearer(token, lifespanSeconds);
    }

    /** Verifies signature/issuer/expiry and extracts the principal. Used by
     *  AuthCheckServiceImpl.resolvePrincipal's JWT branch. */
    public Uni<VerifiedPrincipal> verify(String rawToken) {
        return Uni.createFrom().item(() -> {
            try {
                JsonWebToken jwt = jwtParser.parse(rawToken);
                PrincipalType type = PrincipalType.valueOf(jwt.getClaim("principalType"));
                UUID principalId = UUID.fromString(jwt.getSubject());
                String displayName = jwt.getClaim("upn");
                return new VerifiedPrincipal(type, principalId, displayName);
            } catch (Exception e) {
                throw new UnauthorizedException("IDENTITY-401-INVALID-TOKEN", "Token could not be verified");
            }
        });
    }

    public record VerifiedPrincipal(PrincipalType principalType, UUID principalId, String displayName) {
    }
}
