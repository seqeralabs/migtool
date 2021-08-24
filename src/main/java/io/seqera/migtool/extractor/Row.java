package io.seqera.migtool.extractor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Row in a table.
 */
public class Row {

    private final Map<String, String> columns;

    Row() {
        columns = new LinkedHashMap<>();
    }

    public void addColumn(String name, Object value) {
        columns.put( name, Objects.toString(value) );
    }

    @Override
    public String toString() {
        return "Row{" +
                "columns=" + columns +
                '}';
    }
}
