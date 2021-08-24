package io.seqera.migtool.extractor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Extracts arbitrary rows from a {@link ResultSet}.
 */
public final class ResultSetExtractor {

    private ResultSetExtractor() {}

    public static RowSet extractRows(ResultSet resultSet) throws SQLException {
        RowSet rows = new RowSet();

        if (resultSet == null)
            return rows;

        while (resultSet.next())
            rows.add(extractRow(resultSet));

        return rows;
    }

    private static Row extractRow(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();

        Row row = new Row();
        for (int i = 1; i <= metadata.getColumnCount(); ++i)
            row.addColumn(metadata.getColumnName(i), resultSet.getString(i));

        return row;
    }

}
