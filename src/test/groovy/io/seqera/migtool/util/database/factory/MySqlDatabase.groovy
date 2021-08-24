package io.seqera.migtool.util.database.factory

import io.seqera.migtool.util.database.DbConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

class MySqlDatabase extends ContainerizedDatabase {

    private static final String user = 'test'
    private static final String password = 'test'
    private static final String schema = 'test'

    private static final Map mysqlConfig = [
        MYSQL_ROOT_PASSWORD: 'root',
        MYSQL_USER: user,
        MYSQL_PASSWORD: password,
        MYSQL_DATABASE: schema
    ]

    protected MySqlDatabase(String version) {
        container = new GenericContainer("mysql:${version}")
                .withExposedPorts(3306)
                .withEnv(mysqlConfig)
                .withTmpFs(['/var/lib/mysql':'rw,noexec,nosuid,size=1024m'])
                .waitingFor(Wait.forListeningPort())
                .waitingFor(Wait.forLogMessage(/.*mysqld: ready for connections.*/, 1))
                .waitingFor(Wait.forLogMessage(/.*; port: 3306.*/, 1))

        start()
    }

    @Override
    protected String getListTablesDDL() {
        return """
            SELECT t.table_name FROM information_schema.tables t
            WHERE (t.table_schema = '${schema}') AND
                  (t.table_name NOT LIKE '%_sequence') AND (t.table_type LIKE '%TABLE')
        """
    }

    @Override
    protected String getConstraintsCheckDDL(boolean enable) {
        return "SET FOREIGN_KEY_CHECKS=${enable ? 1 : 0};"
    }

    @Override
    protected DbConfig createConfig() {
        return new DbConfig(
                "jdbc:mysql://localhost:${container.firstMappedPort}/${schema}",
                user,
                password,
                'com.mysql.cj.jdbc.Driver',
                'mysql'
        )
    }

}
