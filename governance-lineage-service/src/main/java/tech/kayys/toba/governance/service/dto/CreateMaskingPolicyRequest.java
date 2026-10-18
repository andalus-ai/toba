package com.dremioplatform.governance.service.dto;

import com.dremioplatform.governance.domain.MaskingRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record CreateMaskingPolicyRequest(
        @NotBlank String name,
        @NotBlank String scopePath,
        @NotNull MaskingRuleType ruleType,
        String columnName,
        @NotBlank String expression,
        Set<UUID> exemptRoleIds
) {
}
