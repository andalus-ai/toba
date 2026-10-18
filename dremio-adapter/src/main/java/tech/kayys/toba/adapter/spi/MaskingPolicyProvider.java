package com.dremioplatform.adapter.spi;

import java.util.List;

/**
 * VERIFIED against real dremio-oss source (see RECONCILIATION_NOTES.md). This
 * confirms — rather than just assumes — the plan's Section 9 approach for masking
 * is the right one for OSS specifically, for a reason worth stating plainly:
 *
 * <p><b>OSS parses masking DDL syntax but cannot execute it — the feature is
 * edition-gated at the handler level, not just "missing".</b> dremio-oss ships a
 * full SQL grammar for native masking policies:
 * <pre>{@code
 * CREATE [OR REPLACE] COLUMN POLICY <policy_name> AS (<field> <type>, ...)
 *   RETURN <type> <policy_sql_expression>
 * ALTER TABLE <table> MODIFY COLUMN <col> SET MASKING POLICY <policy_name>
 * }</pre>
 * (parsers: {@code SqlAlterTableSetColumnMasking}, {@code SqlAlterTableUnsetColumnMasking}
 * under {@code sabot/kernel/.../planner/sql/parser/}). But the DDL's
 * {@code toDirectHandler} method loads its actual execution logic via
 * {@code Class.forName("com.dremio.exec.planner.sql.handlers.EnterpriseAlterTableSetColumnMasking")}
 * — a class that does not exist in the OSS source tree at all. When that
 * {@code ClassNotFoundException} fires, OSS throws {@code UserException.unsupportedError()
 * .message("This command is not supported in this edition of Dremio.")}. There is no
 * native masking execution path in OSS to integrate with, even in principle.
 *
 * <p><b>Conclusion: the plan's external Calcite-plan-rewrite approach (Section 9:
 * "enforced by rewriting the logical plan ... before Dremio's optimizer runs —
 * implemented in the adapter layer, not the coordinator itself") is not an
 * approximation of what native Dremio does — it is the only viable approach for
 * OSS**, since the native path is architecturally absent, not just unused. This
 * upgrades that section of the plan from "reasonable-sounding design" to
 * "confirmed necessary given OSS's actual edition gating."
 *
 * <p>What this does NOT resolve: the actual Calcite {@code RelNode} rewrite still
 * needs Calcite's {@code RelBuilder} and Dremio's plan-building context to
 * implement for real — see this interface's original scope-boundary note, still
 * accurate: this stops at "resolve the applicable rules," not "rewrite the plan."
 */
public interface MaskingPolicyProvider {
    List<MaskingRule> resolveApplicableRules(String datasetPath);

    record MaskingRule(String ruleType, String columnName, String expression) {
    }
}
