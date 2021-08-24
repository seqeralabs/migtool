package io.seqera.migtool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import io.seqera.migtool.exception.ConnectionException;
import io.seqera.migtool.exception.StatementException;
import io.seqera.migtool.exception.TableException;
import io.seqera.migtool.extractor.ResultSetExtractor;
import io.seqera.migtool.extractor.RowSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute SQL statements
 */
public class StatementExecutor {

    private static final Logger log = LoggerFactory.getLogger(StatementExecutor.class);
    private static Class<?> driverClass;

    private final String url;
    private final String user;
    private final String password;


    public StatementExecutor(String url, String user, String password) {
        if (driverClass == null)
            throw new IllegalStateException("Make sure a driver is loaded first");

        this.url = url;
        this.user = user;
        this.password = password;

        testConnection();
    }

    /**
     * Loads a driver given its class name by retrieving the class instance.
     * Although it's no longer necessary in modern versions of JDBC and driver implementations, this guarantees that
     * the given driver is found among the allowed drivers.
     * @param driver The driver class name
     * @see DriverManager
     */
    public static void loadDriver(String driver) {
        try {
            driverClass = Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to find driver class: " + driver, e);
        }
    }

    public boolean existTable(String tableName) {
        log.debug("Checking existence of table '" + tableName + "'");

        try (Connection conn = connection()) {
            ResultSet res = conn
                    .getMetaData()
                    .getTables(null,null, tableName, new String[] {"TABLE"});

            RowSet rows = ResultSetExtractor.extractRows(res);
            logRows(rows);

            return !rows.isEmpty();
        } catch (SQLException e) {
            throw new TableException(tableName, e);
        }
    }

    public void execute(String stmText) {
        try (Connection conn = connection(); Statement stm = conn.createStatement() ) {
            stm.execute(stmText);

            logStatementResult(stm, stmText);
        } catch (SQLException e) {
            throw new StatementException(stmText, e);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void testConnection() {
        try (Connection conn = connection()) {
            log.debug("Checking DB connection: " + url);
        } catch (SQLException e) {
            throw new ConnectionException(url, e);
        }
    }

    private void logStatementResult(Statement stm, String text) throws SQLException {
        if (!log.isDebugEnabled())
            return;

        ResultSet resultSet = stm.getResultSet();
        boolean isUpdate = (resultSet == null);

        log.debug("Statement: {}", text);
        log.debug("Type: {}", isUpdate ? "UPDATE" : "SELECT");

        if (!isUpdate)
            logRows(ResultSetExtractor.extractRows(resultSet));
    }

    private void logRows(RowSet rows) {
        log.debug("Rows: {}", rows);
    }

}
