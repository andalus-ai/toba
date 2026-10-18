package com.dremioplatform.identity.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * identityProviderId + externalSubject are both null for a purely local user, in
 * which case localPassword is required. If identityProviderId is set, localPassword
 * is ignored (federated users authenticate against the external IdP, never locally).
 */
public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 255) String username,
        @NotBlank @Email String email,
        String displayName,
        UUID identityProviderId,
        String externalSubject,
        @Size(min = 8) String localPassword
) {
}
