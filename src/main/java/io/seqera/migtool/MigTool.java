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

    private static final Logger log = LoggerFactory.getLogger(MigTool.class);

    static final String MIGTOOL_TABLE = "MIGTOOL_HISTORY";

    static final String[] DIALECTS = {"h2", "mysql", "mariadb"};

    String driver;
    String url;
    String user;
    String password;
    String dialect;
    String locations;
    ClassLoader classLoader;

    private Connection connection;
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

    protected Connection getConnection() {
        return connection;
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
            // connect db
            connection = DriverManager.getConnection(url, user, password);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to find driver class: " + driver, e);
        }
        catch (SQLException e) {
            throw new IllegalStateException("Unable to connect DB instance: " + url, e);
        }
    }

    protected boolean existTable(String tableName) {
        try {
            ResultSet res = connection
                    .getMetaData()
                    .getTables(null,null, tableName, new String[] {"TABLE"});
            return res.next();
        }
        catch (SQLException e) {
            throw new IllegalStateException("Unable to connect DB", e);
        }
    }

    /**
     * Create the support Migtool DB table if does not exists
     */
    protected void createIfNotExists() {
        try {
            if( !existTable(MIGTOOL_TABLE) ) {
                log.info("Creating MigTool schema using dialect: " + dialect);
                String schema = Helper.getResourceAsString("/schema/" + dialect + ".sql");
                try ( Statement stm = connection.createStatement() ) {
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
            List<String> files = Helper.getResourceFiles(path);
            List<MigRecord> entries = new ArrayList<>(files.size());
            for( String it : files ) {
                MigRecord entry = MigRecord.parseResourcePath(it);
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
                    MigRecord entry = MigRecord.parseFilePath(it);
                    if( entry==null ) {
                        log.warn("Invalid migration source file: " + itr);
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
            connection.setAutoCommit(false);
            for( MigRecord it : migrationEntries) {
                applyMigration(it);
            }
            connection.commit();
        }
        catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException err) { log.warn("Unable to rollback transaction -- cause: "+e.getMessage()); }
            throw new IllegalStateException("Unable perform migration -- cause: "+e.getMessage(), e);
        }
    }

    protected void checkRank(MigRecord entry) {
        try(Statement stm = connection.createStatement()) {
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

        // apply all migration statements
        long now = System.currentTimeMillis();

        try ( Statement stm = connection.createStatement() ) {
            for( String it : entry.statements ) {
                stm.addBatch(it);
            }
            stm.executeBatch();
         }

        // compute the delta
        int delta = (int)(System.currentTimeMillis()-now);

        // save the current migration
        final String insertSql = "insert into "+MIGTOOL_TABLE+" (`rank`,`script`,`checksum`,`created_on`,`execution_time`) values (?,?,?,?,?)";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
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

        try (PreparedStatement stm = connection.prepareStatement(sql)) {
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

    public void close() {
        Helper.tryClose(connection);
    }

}