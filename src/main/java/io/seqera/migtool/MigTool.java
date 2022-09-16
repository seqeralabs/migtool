/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package io.seqera.migtool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.regex.Pattern;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.sql.Sql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement a simple migration tool inspired to Flyway
 *
 * @author Paolo Di Tommaso
 */

public class MigTool {

    static private final String SOURCE_CLASSPATH = "classpath:";
    static private final String SOURCE_FILE = "file:";

    public static int BACK_OFF_BASE = 3;
    public static int BACK_OFF_DELAY = 250;

    private static final Logger log = LoggerFactory.getLogger(MigTool.class);

    static final String MIGTOOL_TABLE = "MIGTOOL_HISTORY";

    static final String[] DIALECTS = {"h2", "mysql", "mariadb","sqlite"};

    String driver;
    String url;
    String user;
    String password;
    String dialect;
    String locations;
    ClassLoader classLoader;
    Pattern pattern;
    String schema;
    String catalog;

    private List<MigRecord> migrationEntries;

    public MigTool withDriver(String driver) {
        this.driver = driver;
        return this;
    }

    public MigTool withUrl(String url) {
        this.url = url;
        return this;
    }

    public MigTool withUser(String user) {
        this.user = user;
        return this;
    }

    public MigTool withPassword(String password) {
        this.password = password;
        return this;
    }

    public MigTool withDialect(String dialect) {
        this.dialect = dialect;
        return this;
    }

    public MigTool withLocations(String locations) {
        this.locations = locations;
        return this;
    }

    public MigTool withClassLoader(ClassLoader loader) {
        this.classLoader = loader;
        return this;
    }

    public MigTool withPattern(String pattern) {
        if(pattern!=null && !pattern.equals(""))
            this.pattern = Pattern.compile(pattern);
        return this;
    }

    /**
     * Main application entry point
     */
    public MigTool run() {
        init();
        ClassLoader previous = null;
        if( classLoader!=null ) {
            previous = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        try {
            createIfNotExists();
            scanMigrations();
            apply();
        }
        finally {
            if( previous!=null ) {
                Thread.currentThread().setContextClassLoader(previous);
            }
        }
        return this;
    }

    protected Connection getConnection() throws SQLException {
        return getConnection(0);
    }

    protected Connection getConnection(int maxRetries) throws SQLException {
        int errorCount=0;
        while( true ) {
            try {
                return DriverManager.getConnection(url, user, password);
            }
            catch (SQLException e) {
                if( errorCount++ >= maxRetries )
                    throw e;

                long delay = Math.round(Math.pow(BACK_OFF_BASE, errorCount)) * BACK_OFF_DELAY;
                log.debug("Got connection error={} - Waiting {}ms and retry (errorCount={})", e, delay, errorCount);
                try {Thread.sleep(delay);} catch (InterruptedException t) { log.debug("Got InterruptedException: {} - Ignoring it",e.getMessage()); }
            }
        }
    }

    List<MigRecord> getMigrationEntries() {
        return migrationEntries;
    }

    /**
     * Validate the expected input params and open the connection with the DB
     */
    protected void init() {
        if( dialect==null || dialect.isEmpty() )
            throw new IllegalStateException("Missing 'dialect' attribute");
        if( url==null || url.isEmpty() )
            throw new IllegalStateException("Missing 'url' attribute");
        if( driver==null || driver.isEmpty() )
            throw new IllegalStateException("Missing 'driver' attribute");
        if( user==null || user.isEmpty() )
            throw new IllegalStateException("Missing 'user' attribute");
        if( password==null )
            throw new IllegalStateException("Missing 'password' attribute");
        if( !Arrays.asList(DIALECTS).contains(dialect) )
            throw new IllegalStateException("Unsupported dialect: " + dialect);
        if( locations==null )
            throw new IllegalStateException("Missing 'locations' attribute");

        try {
            // load driver
            Class.forName(driver);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to find driver class: " + driver, e);
        }

        try( Connection conn = getConnection() ) {
            if( conn == null )
                throw new IllegalStateException("Unable to aquire DB connection");

            // retrieve the database schema
            if( schema==null || schema.isEmpty() ) {
                schema = conn.getSchema();
            }
            if( catalog==null || catalog.isEmpty() ) {
                catalog = conn.getCatalog();
            }
            if ( catalog==null && schema==null ) {
                log.warn("Unable to determine current DB catalog and schema attributes");
            }
        }
        catch (SQLException e) {
            throw new IllegalStateException("Unable to connect DB instance: " + url, e);
        }
    }

    protected boolean existTable(Connection connection, String tableName) throws SQLException {
        ResultSet res = connection
                .getMetaData()
                .getTables(catalog, schema, tableName, new String[] {"TABLE"});
        boolean result = res.next();
        log.debug("Checking existence of DB table={}; catalog={}; schema={}; exist={} rs={}", tableName, catalog, schema, result, dumpResultSet(result, res));
        return result;
    }

    protected String dumpResultSet(boolean hasData, ResultSet rs)  {
        if( !hasData )
            return "n/a";

        StringBuilder result = new StringBuilder();

        try {
            result.append( "{cols:");
            final int cols=rs.getMetaData().getColumnCount();
            for( int i=1; i<=cols; i++ ) {
                if( i>1 ) result.append(",");
                result.append( String.valueOf(rs.getMetaData().getColumnName(i)) );
            }
            result.append( "}; ");

            int row=0;
            do {
                row++;
                result.append( "{row"+row+":");
                for( int i=1; i<=cols; i++ ) {
                    if( i>1 ) result.append(",");
                    result.append( String.valueOf(rs.getObject(i)).replaceAll("\n"," ") );
                }
                result.append( "}; ");
            } while( rs.next() );
            
            return result.toString();
        }
        catch (Exception e) {
            log.warn("Unable to dump result set", e);
            return null;
        }
    }

    /**
     * Create the support Migtool DB table if does not exists
     */
    protected void createIfNotExists() {
        try (Connection conn = getConnection()) {
            if( !existTable(conn, MIGTOOL_TABLE) ) {
                log.info("Creating MigTool schema using dialect: " + dialect);
                String schema = Helper.getResourceAsString("/schema/" + dialect + ".sql");
                try ( Statement stm = conn.createStatement() ) {
                    stm.execute(schema);
                }
            }
        }
        catch (SQLException e) {
            throw new IllegalStateException("Unable to create MigTool schema -- cause: " + e.getMessage(), e);
        }
    }

    /**
     * Look for the migration file in the specified locations, either
     * a file system directory or classpath resources
     */
    protected void scanMigrations() {

        if( locations.startsWith(SOURCE_CLASSPATH) ) {
            String path = locations.substring(SOURCE_CLASSPATH.length());
            Set<String> files = Helper.getResourceFiles(path);
            List<MigRecord> entries = new ArrayList<>(files.size());
            for( String it : files ) {
                MigRecord entry = MigRecord.parseResourcePath(it, pattern);
                if( entry==null ) {
                    log.warn("Invalid migration source file: " + it);
                }
                else {
                    entries.add(entry);
                }
            }
            // sort
            Collections.sort(entries);
            this.migrationEntries = entries;
        }
        else if( locations.startsWith(SOURCE_FILE)) {
            String path = locations.substring(SOURCE_FILE.length());
            try {
                List<MigRecord> entries = new ArrayList<>();
                Iterator<Path> itr = Files.newDirectoryStream(Paths.get(path)).iterator();

                while( itr.hasNext() ) {
                    Path it = itr.next();
                    MigRecord entry = MigRecord.parseFilePath(it, pattern);
                    if( entry==null ) {
                        log.warn("Invalid migration source file: " + it);
                    }
                    else {
                        entries.add(entry);
                    }
                }
                Collections.sort(entries);
                this.migrationEntries = entries;
            }
            catch (IOException e ) {
                throw new IllegalArgumentException("Unable to list files from location: " + locations + " -- cause: " + e.getMessage());
            }
        }
        else {
            throw new IllegalArgumentException("Invalid locations prefix: " + locations);
        }
    }

    /**
     * Apply the migration files
     */
    protected void apply() {
        if( migrationEntries.size()==0 ) {
            log.info("No DB migrations found");
        }

        try {
            for( MigRecord it : migrationEntries) {
                applyMigration(it);
            }
        }
        catch (SQLException e) {
            throw new IllegalStateException("Unable perform migration -- cause: "+e.getMessage(), e);
        }
    }

    protected void checkRank(MigRecord entry) {
        try(Connection conn=getConnection(); Statement stm = conn.createStatement()) {
            ResultSet rs = stm.executeQuery("select max(`rank`) from "+MIGTOOL_TABLE);
            int last = rs.next() ? rs.getInt(1) : 0;
            int expected = last+1;
            if( entry.rank != expected) {
                throw new IllegalStateException(String.format("Invalid migration -- Expected: %d; current rank: %d; migration script: %s", expected, entry.rank, entry.script));
            }
        }
        catch (SQLException e) {
            throw new IllegalStateException("Unable perform migration -- cause: "+e.getMessage(), e);
        }
    }


    protected void applyMigration(MigRecord entry) throws SQLException {
        if( checkMigrated(entry) ) {
            log.info("DB migration already applied: {} {}", entry.rank, entry.script);
            return;
        }

        checkRank(entry);

        log.info("DB migration {} {} ..", entry.rank, entry.script);

        long now = System.currentTimeMillis();

        if (entry.language == MigRecord.Language.groovy) {
            runGroovyMigration(entry);
        } else {
            runSqlMigration(entry);
        }

        // compute the delta
        int delta = (int)(System.currentTimeMillis()-now);

        // save the current migration
        final String insertSql = "insert into "+MIGTOOL_TABLE+" (`rank`,`script`,`checksum`,`created_on`,`execution_time`) values (?,?,?,?,?)";
        try (Connection conn=getConnection(); PreparedStatement insert = conn.prepareStatement(insertSql)) {
            insert.setInt(1, entry.rank);
            insert.setString(2, entry.script);
            insert.setString(3, entry.checksum);
            insert.setTimestamp(4, new Timestamp(now));
            insert.setInt(5, delta);
            insert.executeUpdate();
        }
        // just log
        log.info("DB migration performed: {} {} - execution time {}ms", entry.rank, entry.script, delta);

    }

    protected boolean checkMigrated(MigRecord entry) {
        String sql = "select `id`, `checksum` from " + MIGTOOL_TABLE + " where `rank` = ?";

        try (Connection conn=getConnection(); PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, entry.rank);

            ResultSet rs = stm.executeQuery();
            if( !rs.next() ) {
                return false;
            }
            // otherwise the checksum must match
            String checksum = rs.getString(2);
            if( checksum==null || !checksum.equals(entry.checksum) ) {
                throw new IllegalStateException("Checksum doesn't match for migration with name: " + entry.script) ;
            }
            return true;
        }
        catch (SQLException e) {
            throw new IllegalStateException("Unable validate migration -- cause: "+e.getMessage(), e);
        }
    }

    private void runSqlMigration(MigRecord entry) {
        // Apply all SQL migration statements
        for( String it : entry.statements ) {
            final long ts = System.currentTimeMillis();
            try (Connection conn = getConnection(5); Statement stm=conn.createStatement()) {
                stm.execute(it);
                log.debug("- Applied migration: {} elapsed time: {}ms", it, System.currentTimeMillis()-ts);
            }
            catch (SQLException e) {
                long delta = System.currentTimeMillis()-ts;
                String msg = "SQL MIGRATION FAILED - PLEASE RECOVER THE DATABASE FROM THE LAST BACKUP - Offending statement: "+it+" elapsed time: "+(delta)+"ms";
                throw new IllegalStateException(msg, e);
            }
        }
    }

    protected void runGroovyMigration(MigRecord entry) {
        final long ts = System.currentTimeMillis();

        try (Connection conn = getConnection()) {
            // Bind a `sql` variable, so it can be handled in scripts
            Sql sql = new Sql(conn);
            Binding binding = new Binding( Map.of("sql", sql) );
            GroovyShell shell = new GroovyShell(binding);

            // Run the script in a transaction in order to prevent inconsistent final states
            Closure<Object> closure = new Closure<Object>(null) {
                public Object doCall() {
                    return shell.evaluate(entry.statements.get(0));
                }
            };
            sql.withTransaction(closure);

        } catch (Exception e) {
            long delta = System.currentTimeMillis() - ts;
            String msg = "GROOVY MIGRATION FAILED - PLEASE RECOVER THE DATABASE FROM THE LAST BACKUP - Elapsed time: "+(delta)+"ms";
            throw new IllegalStateException(msg, e);
        }
    }

}
