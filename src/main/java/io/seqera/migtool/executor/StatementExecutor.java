package io.seqera.migtool.executor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import io.seqera.migtool.exception.ConnectionException;
import io.seqera.migtool.exception.InvalidDriverException;
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
            throw new InvalidDriverException(driver, e);
        }
    }

    public boolean existTable(String tableName) {
        log.debug("Checking existence of table '" + tableName + "'");

        try (Connection conn = connection()) {
            ResultSet res = conn
                    .getMetaData()
                    .getTables(null,null, tableName, new String[] {"TABLE"});

            RowSet rows = ResultSetExtractor.extractRows(res);
            StatementResult result = new StatementResult("Table metadata search", res, null);
            log.debug("Result: {}", result);

            return !rows.isEmpty();
        } catch (SQLException e) {
            throw new TableException(tableName, e);
        }
    }

    public StatementResult execute(String stmText) {
        try (Connection conn = connection(); Statement stm = conn.createStatement() ) {

            stm.execute(stmText);
            StatementResult result = new StatementResult(stmText, stm.getResultSet(), null);
            log.debug("Result: {}", result);

            return result;
        } catch (SQLException e) {
            throw new StatementException(stmText, e);
        }
    }

    public StatementResult executeParameterized(String stmText, List<Object> params) {
        try (Connection conn = connection(); PreparedStatement stm = conn.prepareStatement(stmText) ) {

            setStatementParams(stm, params);
            stm.execute();
            StatementResult result = new StatementResult(stmText, stm.getResultSet(), params);
            log.debug("Result: {}", result);

            return result;
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

    private void setStatementParams(PreparedStatement stm, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); ++i) {
            Object param = params.get(i);

            int stmIndex = i + 1;
            if (param.getClass().equals(String.class)) stm.setString(stmIndex, (String) param);
            if (param.getClass().equals(Integer.class)) stm.setInt(stmIndex, (Integer) param);
            if (param.getClass().equals(Timestamp.class)) stm.setTimestamp(stmIndex, (Timestamp) param);
        }
    }

}
