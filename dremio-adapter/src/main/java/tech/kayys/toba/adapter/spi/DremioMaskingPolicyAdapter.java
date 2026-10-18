package com.dremioplatform.adapter.spi;

import com.dremioplatform.adapter.http.MaskingPolicyHttpClient;
import java.util.List;

/**
 * See {@link MaskingPolicyProvider}'s Javadoc for the scope boundary of this
 * increment (resolves rules; does not itself perform the Calcite RelNode rewrite).
 *
 * Unlike {@link DremioAccessControlAdapter}, this class does NOT fail closed to
 * "no rules" on a lookup error — {@link MaskingPolicyHttpClient} throws rather than
 * returning an empty list precisely so this adapter is forced to propagate the
 * failure (which the real integration must turn into "block the query" at the
 * Calcite plan-building layer, not "proceed without masking"). See that client's
 * Javadoc for the fail-direction rationale.
 */
public class DremioMaskingPolicyAdapter implements MaskingPolicyProvider {

    private final MaskingPolicyHttpClient maskingPolicyHttpClient;

    public DremioMaskingPolicyAdapter(MaskingPolicyHttpClient maskingPolicyHttpClient) {
        this.maskingPolicyHttpClient = maskingPolicyHttpClient;
    }

    @Override
    public List<MaskingRule> resolveApplicableRules(String datasetPath) {
        return maskingPolicyHttpClient.findApplicable(datasetPath).stream()
                .map(r -> new MaskingRule(r.ruleType(), r.columnName(), r.expression()))
                .toList();
    }
}
