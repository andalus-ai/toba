package com.dremioplatform.toba.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EngineResultPage(List<Column> columns, List<Map<String, Object>> rows,
                               int offset, boolean hasMore) {
    public record Column(String name, String type) {
        public Column {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("column name required");
            if (type == null || type.isBlank()) throw new IllegalArgumentException("column type required");
        }
    }

    public EngineResultPage {
        columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
        Objects.requireNonNull(rows, "rows");
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        List<Map<String, Object>> safeRows = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            safeRows.add(Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(row))));
        }
        rows = List.copyOf(safeRows);
    }
}
