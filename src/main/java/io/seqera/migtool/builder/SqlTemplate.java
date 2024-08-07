package io.seqera.migtool.builder;

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public abstract class SqlTemplate {

    abstract public String selectMaxRank(String table);

    abstract public String insetMigration(String table);

    abstract public String selectMigration(String table);

    static public SqlTemplate from(String dialect) {
        if( "postgresql".equals(dialect) )
            return new PostgresSqlTemplate();
        else
            return new DefaultSqlTemplate();
    }

}
