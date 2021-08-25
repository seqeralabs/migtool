package io.seqera.migtool.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set of rows in a table.
 */
public class RowSet {

    private final List<Row> rows;

    RowSet() {
        rows = new ArrayList<>();
    }

    public List<Row> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public void add(Row row) {
        rows.add(row);
    }

    @Override
    public String toString() {
        return "RowSet{" +
                "rows=" + rows +
                '}';
    }
}
