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

    /**
     * Splits the content of a SQL migration file into individual statements.
     *
     * Statements are separated by semicolons, but semicolons appearing inside a
     * line comment ({@code -- ...}), a block comment ({@code /* ... *&#47;}) or a
     * quoted string literal ({@code '...'} or {@code "..."}) are ignored so they
     * are not mistaken for statement separators.
     */
    private static List<String> getSqlStatements(String sql) {
        List<String> result = new ArrayList<>();
        boolean inLineComment = false;
        boolean inBlockComment = false;
        char quote = '\0'; // current open string delimiter, or '\0' when not in a string
        int start = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
            }
            else if (inBlockComment) {
                if (c == '*' && next == '/') inBlockComment = false;
            }
            else if (quote != '\0') {
                // inside a string literal: a doubled quote is an escaped quote, not the end
                if (c == quote) {
                    if (next == quote) i++;
                    else quote = '\0';
                }
            }
            else if (c == '-' && next == '-') {
                inLineComment = true;
            }
            else if (c == '/' && next == '*') {
                inBlockComment = true;
            }
            else if (c == '\'' || c == '"') {
                quote = c;
            }
            else if (c == ';') {
                addStatement(result, sql.substring(start, i));
                start = i + 1;
            }
        }

        // trailing content with no closing semicolon
        addStatement(result, sql.substring(start));

        return result;
    }

    private static void addStatement(List<String> statements, String statement) {
        String clean = statement.trim();
        if (!clean.isEmpty())
            statements.add(clean + ';');
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
