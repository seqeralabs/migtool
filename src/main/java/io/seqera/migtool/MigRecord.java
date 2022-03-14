package io.seqera.migtool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model a migration record
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class MigRecord implements Comparable<MigRecord> {

    static final Pattern DEFAULT_PATTERN = Pattern.compile("^V(\\d+)__(.+)");

    int rank;
    String script;
    String checksum;
    List<String> statements;

    @Override
    public int compareTo(MigRecord other) {
        return this.rank - other.rank;
    }

    @Override
    public String toString() {
        return String.format("MigRecord(rank=%d; script=%s; checksum=%s; statements=%s)", rank, script, checksum, join(statements));
    }

    static MigRecord parseResourcePath(String path, Pattern pattern) {
        int p = path.lastIndexOf("/");
        String fileName = p==-1 ? path : path.substring(p+1);

        if( pattern == null )
            pattern = DEFAULT_PATTERN;
        
        Matcher m = pattern.matcher(fileName);
        if( !m.matches() ) {
            return null;
        }

        MigRecord entry = new MigRecord();
        entry.rank = Integer.parseInt(m.group(1));
        entry.script = fileName;
        entry.statements = getStatements( Helper.getResourceAsString(path) );
        entry.checksum = Helper.computeSha256(join(entry.statements));
        return entry;
    }

    static MigRecord parseFilePath(Path path, Pattern pattern) {
        if( pattern == null )
            pattern = DEFAULT_PATTERN;

        Matcher m = pattern.matcher(path.getFileName().toString());
        if( !m.matches() ) {
            return null;
        }

        MigRecord entry = new MigRecord();
        entry.rank = Integer.parseInt(m.group(1));
        entry.script = path.getFileName().toString();
        entry.statements = getStatements(path);
        entry.checksum = Helper.computeSha256(join(entry.statements));
        return entry;
    }

    static List<String> getStatements(Path path) {
        try {
            String sql = new String(Files.readAllBytes(path));
            return getStatements(sql);
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to read migration file: "+path+" -- cause: " + e.getMessage(), e);
        }
    }

    static List<String> getStatements(String sql) {
        String[] tokens = sql.split(";");
        List<String> result = new ArrayList<>(tokens.length);

        for( int i=0; i<tokens.length; i++) {
            String clean = tokens[i].trim();
            if( clean.length()>0 )
                result.add( clean + ';' ) ;
        }

        return result;
    }

    static String join(List<String> items) {
        StringBuilder result = new StringBuilder();
        for( String it : items ) {
            result.append(it);
        }
        return result.toString();
    }
}
