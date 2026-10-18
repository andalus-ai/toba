package com.dremioplatform.governance.service.dto;

import com.dremioplatform.governance.domain.MaskingPolicy;
import com.dremioplatform.governance.domain.MaskingRuleType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MaskingPolicyResponse(
        UUID id, String name, String scopePath, MaskingRuleType ruleType, String columnName,
        String expression, Set<UUID> exemptRoleIds, boolean enabled, Instant createdAt
) {
    public static MaskingPolicyResponse from(MaskingPolicy p) {
        return new MaskingPolicyResponse(p.id, p.name, p.scopePath, p.ruleType, p.columnName,
                p.expression, p.exemptRoleIds, p.enabled, p.createdAt);
    }
}
