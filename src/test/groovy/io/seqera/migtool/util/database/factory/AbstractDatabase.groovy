package io.seqera.migtool.util.database.factory

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

import groovy.transform.CompileStatic
import io.seqera.migtool.extractor.ResultSetExtractor
import io.seqera.migtool.extractor.Row
import io.seqera.migtool.extractor.RowSet
import io.seqera.migtool.util.database.DbConfig

@CompileStatic
abstract class AbstractDatabase implements Database {

    void cleanup() {
        DbConfig config = getConfig()

        try ( Connection conn = DriverManager.getConnection(config.url, config.user, config.password) ) {
            disableConstraints(conn)
            dropAllTables(conn)
            enableConstraints(conn)
        }
    }

    protected String getDropTableDDL(String tableName) {
        return "DROP TABLE IF EXISTS ${tableName};"
    }

    protected abstract String getListTablesDDL()
    protected abstract String getConstraintsCheckDDL(boolean enable)

    private void dropAllTables(Connection conn) {
        def tables = executeStatement(conn, listTablesDDL)
        for (Row row : tables.getRows()) {
            String dropTableStatement = getDropTableDDL(row.getFirstValue())
            executeStatement(conn, dropTableStatement)
        }
    }

    private void disableConstraints(Connection conn) {
        String disableChecksStatement = getConstraintsCheckDDL(false)
        executeStatement(conn, disableChecksStatement)
    }

    private void enableConstraints(Connection conn) {
        String enableChecksStatement = getConstraintsCheckDDL(true)
        executeStatement(conn, enableChecksStatement)
    }

    private static RowSet executeStatement(Connection connection, String statement) {
        try (Statement stm = connection.createStatement() ) {
            stm.execute(statement)

            return ResultSetExtractor.extractRows(stm.resultSet)
        }
    }

}
