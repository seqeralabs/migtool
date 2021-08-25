package io.seqera.migtool.executor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import io.seqera.migtool.extractor.ResultSetExtractor;
import io.seqera.migtool.extractor.RowSet;

/**
 * Result of a statement execution
 */
public class StatementResult {

    /**
     * The statement text.
     */
    private final String text;

    /**
     * Whether the statement was a query (SELECT) or an update statement (UPDATE, INSERT, ...).
     */
    private final boolean isQuery;

    /**
     * The set of rows in the result (if any)
     */
    private final RowSet rows;

    /**
     * The list of params in the case of a parameterized statement
     */
    private final List<Object> params;

    StatementResult(String text, ResultSet resultSet, List<Object> params) throws SQLException {
        this.text = text;
        this.isQuery = (resultSet != null);
        this.rows = ResultSetExtractor.extractRows(resultSet);
        this.params = params;
    }

    public RowSet getRowSet() {
        return rows;
    }

    public boolean isQuery() {
        return isQuery;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("StatementResult{")
               .append("\n   text='" + text + "'");

        if (params != null)
            builder.append("\n   params=" + params);

        builder.append("\n   type=" + getType());

        if (isQuery)
            builder.append("\n   rows=" + rows)
                   .append("\n}");

        return builder.toString();
    }

    private String getType() {
        return isQuery ? "SELECT" : "UPDATE";
    }

}
