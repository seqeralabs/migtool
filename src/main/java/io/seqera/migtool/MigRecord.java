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
    static final Pattern OVERRIDE_PATTERN = Pattern.compile("\\.override");
    static final Pattern PATCH_PATTERN = Pattern.compile("\\.patch");

    int rank;
    String script;
    Language language;
    String checksum;
    List<String> statements;

    boolean isPatch;
    boolean isOverride;

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

        // We do not allow files with double fix or amend, ex. V01__organisation.fixed.fixed.sql
        if (countMatch(fileName, OVERRIDE_PATTERN) > 1) {
            return null;
        }
        if (countMatch(fileName, PATCH_PATTERN) > 1) {
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

        // We do not allow files with double fix or amend, ex. V01__organisation.fixed.fixed.sql
        if (countMatch(fileName, OVERRIDE_PATTERN) > 1) {
            return null;
        }
        if (countMatch(fileName, PATCH_PATTERN) > 1) {
            return null;
        }

        String content = readContent(path);
        int rank = Integer.parseInt(m.group(1));

        return createRecord(fileName, rank, content);
    }

    private static long countMatch(String name, Pattern pattern) {
        return pattern.matcher(name).results().count();
    }

    protected boolean isPatch() {
        return this.isPatch;
    }

    protected boolean isOverride() {
        return this.isOverride;
    }

    private static MigRecord createRecord(String fileName, int rank, String content) {
        MigRecord entry = new MigRecord();

        entry.language = Language.from(fileName);
        entry.rank = rank;
        entry.script = fileName;
        // Groovy scripts are not split in multiple statements; they will be executed as a one single big statement
        entry.statements = (entry.language == Language.GROOVY) ? List.of(content) : getSqlStatements(content);

        entry.checksum = Helper.computeSha256(join(entry.statements));

        entry.isPatch = countMatch(fileName, PATCH_PATTERN) == 1;
        entry.isOverride = countMatch(fileName, OVERRIDE_PATTERN) == 1;

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
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int len = sql.length();
        int i = 0;

        while (i < len) {
            char c = sql.charAt(i);

            // -- line comment: skip until end of line
            if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
                int end = sql.indexOf('\n', i);
                if (end == -1) end = len;
                else end += 1; // include the newline
                current.append(sql, i, end);
                i = end;
                continue;
            }

            // /* block comment */: skip until */
            if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                int end = sql.indexOf("*/", i + 2);
                if (end == -1) end = len;
                else end += 2; // include the closing */
                current.append(sql, i, end);
                i = end;
                continue;
            }

            // quoted string: skip until matching closing quote (handles '' escapes)
            if (c == '\'' || c == '"') {
                char quote = c;
                current.append(c);
                i++;
                while (i < len) {
                    char qc = sql.charAt(i);
                    current.append(qc);
                    i++;
                    if (qc == quote) {
                        // handle doubled-quote escape
                        if (i < len && sql.charAt(i) == quote) {
                            current.append(sql.charAt(i));
                            i++;
                        } else {
                            break;
                        }
                    }
                }
                continue;
            }

            if (c == ';') {
                String clean = current.toString().trim();
                if (!clean.isEmpty())
                    result.add(clean + ';');
                current.setLength(0);
                i++;
                continue;
            }

            current.append(c);
            i++;
        }

        // trailing content with no closing semicolon
        String clean = current.toString().trim();
        if (!clean.isEmpty())
            result.add(clean + ';');

        return result;
    }

    private static String join(List<String> items) {
        StringBuilder result = new StringBuilder();
        for( String it : items ) {
            result.append(it);
        }
        return result.toString();
    }

    /**
     * Returns file name without extension (if present).
     * @return base file name. Ex. file.sql -> file, file.groovy -> file, file -> file
     */
    String getFileNameWithoutExtension() {
        String[] fileNameGroups = this.script.split("\\.");
        String extension = fileNameGroups[fileNameGroups.length - 1];
        if (fileNameGroups.length == 1 || extension.equals("patch") || extension.equals("override")) {
            return this.script;
        }
        return this.script.substring(0, this.script.length() - (extension.length() + 1));
    }
}
