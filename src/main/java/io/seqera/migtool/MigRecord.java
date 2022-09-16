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

    enum Language {
        SQL,
        GROOVY;

        static Language from(String fileName) {
            if (fileName.endsWith(".groovy")) return GROOVY;

            // Fall back to SQL in any other case
            return SQL;
        }
    }

    static final Pattern DEFAULT_PATTERN = Pattern.compile("^V(\\d+)__(.+)");

    int rank;
    String script;
    Language language;
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
        if( pattern == null ) pattern = DEFAULT_PATTERN;

        int p = path.lastIndexOf("/");
        String fileName = p==-1 ? path : path.substring(p+1);
        
        Matcher m = pattern.matcher(fileName);
        if( !m.matches() ) {
            return null;
        }

        String content = readContent(path);
        int rank = Integer.parseInt(m.group(1));

        return createRecord(fileName, rank, content);
    }

    static MigRecord parseFilePath(Path path, Pattern pattern) {
        if( pattern == null ) pattern = DEFAULT_PATTERN;

        String fileName = path.getFileName().toString();

        Matcher m = pattern.matcher(fileName);
        if( !m.matches() ) {
            return null;
        }

        String content = readContent(path);
        int rank = Integer.parseInt(m.group(1));

        return createRecord(fileName, rank, content);
    }

    private static MigRecord createRecord(String fileName, int rank, String content) {
        MigRecord entry = new MigRecord();

        entry.language = Language.from(fileName);
        entry.rank = rank;
        entry.script = fileName;
        // Groovy scripts are not split in multiple statements; they will be executed as a one single big statement
        entry.statements = (entry.language == Language.GROOVY) ? List.of(content) : getSqlStatements(content);

        entry.checksum = Helper.computeSha256(join(entry.statements));

        return entry;
    }

    private static String readContent(String resourcePath) {
        return Helper.getResourceAsString(resourcePath);
    }

    private static String readContent(Path filePath) {
        try {
            return new String(Files.readAllBytes(filePath));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read migration file: "+filePath+" -- cause: " + e.getMessage(), e);
        }
    }

    private static List<String> getSqlStatements(String sql) {
        String[] tokens = sql.split(";");
        List<String> result = new ArrayList<>(tokens.length);

        for( int i=0; i<tokens.length; i++) {
            String clean = tokens[i].trim();
            if( clean.length()>0 )
                result.add( clean + ';' ) ;
        }

        return result;
    }

    private static String join(List<String> items) {
        StringBuilder result = new StringBuilder();
        for( String it : items ) {
            result.append(it);
        }
        return result.toString();
    }
}
