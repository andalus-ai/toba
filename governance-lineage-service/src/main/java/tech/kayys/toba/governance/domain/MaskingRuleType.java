package com.dremioplatform.governance.domain;

public enum MaskingRuleType {
    /** Replaces a single column's value for principals who aren't exempt. */
    COLUMN_MASK,
    /** Filters out entire rows that don't match a predicate, for non-exempt principals. */
    ROW_FILTER
}
