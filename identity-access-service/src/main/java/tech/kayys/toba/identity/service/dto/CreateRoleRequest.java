package com.dremioplatform.identity.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateRoleRequest(
        @NotBlank String name,
        String description,
        @NotEmpty List<String> permissionKeys
) {
}
