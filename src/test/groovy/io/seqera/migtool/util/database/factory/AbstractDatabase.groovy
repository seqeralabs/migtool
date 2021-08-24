package io.seqera.migtool.util.database.factory

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.Statement

import groovy.transform.CompileStatic
import io.seqera.migtool.util.database.DbConfig

@CompileStatic
abstract class AbstractDatabase implements Database {

    void cleanup() {
        DbConfig config = getConfig()

        try ( Connection conn = DriverManager.getConnection(config.url, config.user, config.password) ) {

            def tables = executeStatement(conn, listTablesDDL)

            String disableChecksStatement = getConstraintsCheckDDL(false)
            executeStatement(conn, disableChecksStatement)

            tables.each { row ->
                String dropTableStatement = getDropTableDDL(row.values().first())
                executeStatement(conn, dropTableStatement)
            }

            String enableChecksStatement = getConstraintsCheckDDL(true)
            executeStatement(conn, enableChecksStatement)

        }
    }

    protected String getDropTableDDL(String tableName) {
        return "DROP TABLE IF EXISTS ${tableName};"
    }

    protected abstract String getListTablesDDL()
    protected abstract String getConstraintsCheckDDL(boolean enable)

    private static List<Map<String, String>> executeStatement(Connection connection, String statement) {
        try (Statement stm = connection.createStatement() ) {
            stm.execute(statement)

            return extractResultSet(stm.resultSet)
        }
    }

    private static List<Map<String, String>> extractResultSet(ResultSet resultSet) {
        if (!resultSet)
            return []

        List<Map<String, String>> rows = []
        while (resultSet.next())
            rows << extractRow(resultSet)

        return rows
    }

    private static Map<String, String> extractRow(ResultSet resultSet) {
        ResultSetMetaData metadata = resultSet.metaData

        Map<String, String> row = [:]
        for (int i = 1; i <= metadata.columnCount; ++i) {
            String key = metadata.getColumnName(i)
            String value = resultSet.getString(i)

            row[key] = value
        }

        return row
    }

}
