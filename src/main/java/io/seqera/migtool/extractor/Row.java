package io.seqera.migtool.extractor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Row in a table.
 */
public class Row {

    private final Map<String, String> columns;

    Row() {
        columns = new LinkedHashMap<>();
    }

    public Map<String, String> getColumns() {
        return Collections.unmodifiableMap(columns);
    }

    public void addColumn(String name, Object value) {
        columns.put( name, (value == null) ? null : value.toString() );
    }

    public String getFirstValue() {
        return columns.values().iterator().next();
    }

    @Override
    public String toString() {
        return "Row{" +
                "columns=" + columns +
                '}';
    }
}
