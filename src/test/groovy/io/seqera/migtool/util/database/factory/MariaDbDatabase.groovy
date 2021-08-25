package io.seqera.migtool.util.database.factory

import io.seqera.migtool.Dialect
import io.seqera.migtool.util.database.DbConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

class MariaDbDatabase extends ContainerizedDatabase {

    private static final String user = 'test'
    private static final String password = 'test'
    private static final String schema = 'test'

    private static final Map mariadbConfig = [
        MYSQL_ROOT_PASSWORD: 'root',
        MYSQL_USER: user,
        MYSQL_PASSWORD: password,
        MYSQL_DATABASE: schema
    ]

    protected MariaDbDatabase() {
        container = new GenericContainer("mariadb:10")
                .withExposedPorts(3306)
                .withEnv(mariadbConfig)
                .waitingFor(Wait.forListeningPort())
                .waitingFor(Wait.forLogMessage(/.*ready for connections.*/, 1))
                .waitingFor(Wait.forLogMessage(/.*3306\s+mariadb.org binary distribution.*/, 1))

        start()
    }

    @Override
    String getListTablesDDL() {
        return """
            SELECT t.table_name FROM information_schema.tables t
            WHERE (t.table_schema = '${schema}') AND
                  (t.table_name NOT LIKE '%_sequence') AND (t.table_type LIKE '%TABLE')
        """
    }

    @Override
    String getConstraintsCheckDDL(boolean enable) {
        return "SET FOREIGN_KEY_CHECKS=${enable ? 1 : 0};"
    }

    @Override
    protected DbConfig createConfig() {
        return new DbConfig(
                "jdbc:mariadb://localhost:${container.firstMappedPort}/${schema}",
                user,
                password,
                'org.mariadb.jdbc.Driver',
                Dialect.mariadb
        )
    }
}
