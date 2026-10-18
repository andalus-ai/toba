package com.dremioplatform.adapter.spi;

/**
 * MIRROR INTERFACE — see {@link DremioAccessControlChecker}'s Javadoc for the same
 * reconciliation caveat, though this one is different in an important way: unlike
 * that interface, the REAL mechanism this mirrors — {@code com.dremio.exec.expr.fn.PluggableFunctionRegistry}
 * — has been VERIFIED as a genuine, working classpath-scanned plugin extension
 * point (see RECONCILIATION_NOTES.md, Finding 3). What's mirrored/simplified here
 * is not "does this exist" (confirmed: yes) but the actual per-row execution
 * contract, which needs Calcite's {@code SqlOperator}/{@code AbstractFunctionHolder}
 * types and Dremio's {@code @FunctionTemplate}-based codegen to implement for real.
 *
 * <p><b>Read RECONCILIATION_NOTES.md's Finding 3 before building against this.</b>
 * The real {@code SimpleFunction.eval()} contract is synchronous, per-row, and runs
 * on the query execution thread with no batching or async hook — making a blocking
 * network call per row inside it is architecturally sound (nothing stops it) but
 * operationally dangerous at any real result-set scale. This mirror and
 * {@link com.dremioplatform.adapter.ai.AiFunctionHttpClient}'s response cache exist
 * to make small/interactive use viable, not to pretend the scale problem is solved.
 */
public interface DremioAiFunctionRegistry {

    /** Backs a real `SimpleFunction`'s eval() for `AI_SUMMARIZE(text)`. Blocking —
     *  see class Javadoc. */
    String summarize(String inputText);

    /** Backs a real `SimpleFunction`'s eval() for `AI_CLASSIFY(text, categories)`.
     *  Blocking — see class Javadoc. */
    String classify(String inputText, java.util.List<String> categories);
}
