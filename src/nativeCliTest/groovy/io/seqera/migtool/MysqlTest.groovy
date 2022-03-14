package io.seqera.migtool

import groovy.sql.Sql
import org.testcontainers.containers.MySQLContainer
import spock.lang.Specification
import spock.lang.Timeout

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class MysqlTest extends Specification {

    static MySQLContainer container

    static {
        container = new MySQLContainer("mysql:5.6")
        // start it -- note: it's stopped automatically
        // https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/
        container.start()
    }

    @Timeout(30)
    def 'should run native binary' () {
        given:
        def BIN = System.getenv('NATIVE_BINARY_PATH')
        def CLI = [BIN,
                '-u', container.username,
                '-p', container.password,
                '--url', container.jdbcUrl,
                '--location', 'file:src/test/resources/migrate-db/mysql' ]

        when:
        println "Running: ${CLI.join( )}"
        def proc = new ProcessBuilder()
                .command(CLI)
                .redirectErrorStream(true)
                .start()
        and:
        def result = proc.waitFor()
        if( result!=0 )
            System.err.println(proc.text)
        
        then:
        result == 0

        and:
        Sql.newInstance(container.jdbcUrl, container.username, container.password)
                .rows("SELECT table_name FROM information_schema.tables where table_name='license'")
    }
}
