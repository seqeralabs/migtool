package io.seqera.migtool.strategy;

public class PostgresQueryBuilder extends DialectQueryBuilder {

    public PostgresQueryBuilder(String tableName) {
        super(tableName);
    }

    @Override
    protected String id() {
        return "id";
    }

    @Override
    protected String rank() {
        return "rank";
    }

    @Override
    protected String script() {
        return "script";
    }

    @Override
    protected String checksum() {
        return "checksum";
    }

    @Override
    protected String createdOn() {
        return "created_on";
    }

    @Override
    protected String executionTime() {
        return "execution_time";
    }
}
