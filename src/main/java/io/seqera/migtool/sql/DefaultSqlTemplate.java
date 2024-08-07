package io.seqera.migtool.sql;

public class DefaultSqlTemplate {

    private final String tableName;

    public DefaultSqlTemplate(String tableName) {
        this.tableName = tableName;
    }

    public String buildCheckMigratedQuery() {
        return String.format(
                "select %s, %s, %s from %s where %s = ? and %s = ?", id(), checksum(), script(), tableName, rank(),
                script()
        );
    }

    public String buildCheckRankQuery() {
        return String.format("select max(%s) from %s", rank(), tableName);
    }

    public String buildMigrateQuery() {
        return String.format(
                "insert into %s (%s, %s, %s, %s, %s) values (?,?,?,?,?)", tableName, rank(), script(), checksum(),
                createdOn(), executionTime()
        );
    }

    protected String id() {
        return "`id`";
    }

    protected String rank() {
        return "`rank`";
    }

    protected String script() {
        return "`script`";
    }

    protected String checksum() {
        return "`checksum`";
    }

    protected String createdOn() {
        return "`created_on`";
    }

    protected String executionTime() {
        return "`execution_time`";
    }

}
