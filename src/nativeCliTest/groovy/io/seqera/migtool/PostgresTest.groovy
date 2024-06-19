package io.seqera.migtool

import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Timeout

import groovy.sql.Sql
import org.testcontainers.containers.PostgreSQLContainer

@Timeout(30)
@Requires({
    System.getenv('NATIVE_BINARY_PATH')
})
class PostgresTest extends Specification {

    static PostgreSQLContainer container

    static {
        container = new PostgreSQLContainer("postgres:16-alpine")
        // start it -- note: it's stopped automatically
        // https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/
        container.start()
    }

    def 'should run native binary'() {
        given:
        def BIN = System.getenv('NATIVE_BINARY_PATH')
        def CLI = [BIN,
                   '-u', container.username,
                   '-p', container.password,
                   '--url', container.jdbcUrl,
                   '--pattern', '^V(\\d+)__(.+)',
                   '--location', 'file:src/nativeCliTest/resources/migrate-db/postgres']

        when:
        println "Running: ${CLI.join()}"
        def proc = new ProcessBuilder()
                .command(CLI)
                .redirectErrorStream(true)
                .start()
        and:
        def result = proc.waitFor()
        if (result != 0) {
            System.err.println(proc.text)
        }

        then:
        result == 0

        and:
        Sql.newInstance(container.jdbcUrl, container.username, container.password)
           .rows("SELECT table_name FROM information_schema.tables where table_name='license'")
    }
}
