package io.seqera.migtool.strategy;

/**
 * Build the queries to execute, based on the dialect chosen.
 * This is needed because Postgres does not support backticks for column names (which is a non-standard adopted by
 * MySql), so we need to build the queries accordingly.
 * Since the other dialects supported allow the use of backticks, only two builders are added
 * {@link PostgresQueryBuilder} and {@link NotPostgresQueryBuilder}.
 **/
public abstract class DialectQueryBuilder {

    private final String tableName;

    public DialectQueryBuilder(String tableName) {
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

    protected abstract String id();

    protected abstract String rank();

    protected abstract String script();

    protected abstract String checksum();

    protected abstract String createdOn();

    protected abstract String executionTime();

}
