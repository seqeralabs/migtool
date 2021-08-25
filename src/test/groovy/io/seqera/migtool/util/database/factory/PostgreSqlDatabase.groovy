package io.seqera.migtool.util.database.factory

import io.seqera.migtool.Dialect
import io.seqera.migtool.util.database.DbConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

class PostgreSqlDatabase extends ContainerizedDatabase {

    private static final String user = 'test'
    private static final String password = 'test'
    private static final String schema = 'test'

    private static final Map postgreSqlConfig = [
            POSTGRES_USER: user,
            POSTGRES_PASSWORD: password,
            POSTGRES_DB: schema
    ]

    protected PostgreSqlDatabase() {
        container = new GenericContainer("postgres:12")
                .withExposedPorts(5432)
                .withEnv(postgreSqlConfig)
                .waitingFor(Wait.forListeningPort())
                .waitingFor(Wait.forLogMessage(/.*starting PostgreSQL.*/, 2))
                .waitingFor(Wait.forLogMessage(/.*PostgreSQL init process complete.*/, 1))
                .waitingFor(Wait.forLogMessage(/.*ready to accept connections.*/, 2))

        start()
    }

    @Override
    protected String getListTablesDDL() {
        return """
            select table_name from information_schema.tables
            where (table_schema='public') and
                  (table_type='BASE TABLE');
        """
    }

    @Override
    protected String getConstraintsCheckDDL(boolean enable) {
        return null
    }

    @Override
    protected DbConfig createConfig() {
        return new DbConfig(
                "jdbc:postgresql://localhost:${container.firstMappedPort}/${schema}",
                user,
                password,
                'org.postgresql.Driver',
                Dialect.postgresql
        )
    }

}
