package io.seqera.migtool.template;

/**
 * Implements a simple template pattern to provide specialised
 * version of required SQL statements depending on the specified SQL "dialect"
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public abstract class SqlTemplate {

    abstract public String selectMaxRank(String table);

    abstract public String insetMigration(String table);

    abstract public String selectMigration(String table);

    static public SqlTemplate from(String dialect) {
        if( "postgresql".equals(dialect) )
            return new PostgreSqlTemplate();
        else
            return new DefaultSqlTemplate();
    }

    public static SqlTemplate defaultTemplate() {
        return new DefaultSqlTemplate();
    }

}
