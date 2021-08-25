/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package io.seqera.migtool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import io.seqera.migtool.exception.MigToolException;
import io.seqera.migtool.executor.StatementExecutor;
import io.seqera.migtool.executor.StatementResult;
import io.seqera.migtool.extractor.Row;
import io.seqera.migtool.extractor.RowSet;
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

    String driver;
    String url;
    String user;
    String password;
    String dialect;
    String locations;
    ClassLoader classLoader;
    Pattern pattern;

    private StatementExecutor executor;

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
        if( Dialect.getByString(dialect) == null )
            throw new IllegalStateException("Unsupported dialect: " + dialect);
        if( locations==null )
            throw new IllegalStateException("Missing 'locations' attribute");

        try {
            StatementExecutor.loadDriver(driver);
            executor = new StatementExecutor(url, user, password);
        } catch (MigToolException e) {
            throw new IllegalStateException("Initialization error occurred", e);
        }
    }

    protected boolean existTable(String tableName) {
        return executor.existTable(tableName);
    }

    /**
     * Create the support Migtool DB table if does not exists
     */
    protected void createIfNotExists() {
        if (existTable(MIGTOOL_TABLE))
            return;

        log.info("Creating MigTool schema using dialect: " + dialect);
        String schema = Helper.getResourceAsString("/schema/" + dialect + ".sql");
        executor.execute(schema);
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
        if (migrationEntries.size() == 0)
            log.info("No DB migrations found");

        for (MigRecord it : migrationEntries)
            applyMigration(it);
    }

    protected void checkRank(MigRecord entry) {
        String sql = "select max(rank) from " + MIGTOOL_TABLE;

        StatementResult result = executor.execute(sql);
        Row firstRow = result.getRowSet().getRows().get(0);

        String value = firstRow.getFirstValue();
        int last = (value == null) ? 0 : Integer.parseInt(value);
        int expected = last + 1;
        if (entry.rank != expected)
            throw new IllegalStateException(String.format("Invalid migration -- Expected: %d; current rank: %d; migration script: %s", expected, entry.rank, entry.script));
    }

    protected void applyMigration(MigRecord entry) {
        if (checkMigrated(entry)) {
            log.info("DB migration already applied: {} {}", entry.rank, entry.script);
            return;
        }

        checkRank(entry);

        log.info("DB migration {} {} ..", entry.rank, entry.script);

        long now = System.currentTimeMillis();
        applyStatements(entry.statements);
        int delta = (int) (System.currentTimeMillis() - now);

        saveMigration(now, delta, entry);
        log.info("DB migration performed: {} {} - execution time {}ms", entry.rank, entry.script, delta);
    }

    protected boolean checkMigrated(MigRecord entry) {
        String sql = "select checksum from " + MIGTOOL_TABLE + " where rank = ?";

        StatementResult result = executor.executeParameterized(sql, Collections.singletonList(entry.rank));
        RowSet rowSet = result.getRowSet();

        if (rowSet.isEmpty())
            return false;

        // otherwise, the checksum must match
        Row firstRow = rowSet.getRows().get(0);
        String checksum = firstRow.getFirstValue();

        if (checksum == null || !checksum.equals(entry.checksum))
            throw new IllegalStateException("Checksum doesn't match for migration with name: " + entry.script);

        return true;
    }

    private void applyStatements(List<String> statements) {
        for (String it : statements)
            executor.execute(it);
    }

    private void saveMigration(long nowMillis, int deltaMillis, MigRecord entry) {
        final String insertSql = "insert into " + MIGTOOL_TABLE + " (rank, script, checksum, created_on, execution_time) values (?,?,?,?,?)";
        executor.executeParameterized(insertSql, Arrays.asList(entry.rank, entry.script, entry.checksum, new Timestamp(nowMillis), deltaMillis));
    }

    /**
     * @deprecated There's no need to close the tool (there are no closeable resources anymore)
     */
    @Deprecated
    public void close() {

    }

}
