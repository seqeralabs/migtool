package io.seqera.migtool

import org.testcontainers.containers.MySQLContainer
import spock.lang.IgnoreIf
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Timeout

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class MysqlTest extends Specification {

    private static final int PORT = 3306


    static MySQLContainer container

    static {
        container = new MySQLContainer("mysql:5.6")
        // start it -- note: it's stopped automatically
        // https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/
        container.start()
    }

    @IgnoreIf({System.getenv('NATIVE_BINARY_PATH')})
    def 'should do something'  () {
        given:
        def tool = new MigTool()
                .withDriver('com.mysql.cj.jdbc.Driver')
                .withDialect('mysql')
                .withUrl(container.getJdbcUrl())
                .withUser(container.getUsername())
                .withPassword(container.getPassword())
                .withLocations('file:src/test/resources/migrate-db/mysql')

        when:
        tool.run()

        then:
        tool.existTable(tool.getConnection(), 'organization')
        tool.existTable(tool.getConnection(), 'license')
        !tool.existTable(tool.getConnection(), 'foo')

    }

    @Timeout(30)
    @Requires({System.getenv('NATIVE_BINARY_PATH')})
    def 'should run native binary' () {
        given:
        def BIN = System.getenv('NATIVE_BINARY_PATH')
        def CLI = [BIN,
                '-u', container.getUsername(),
                '-p', container.getPassword(),
                '--url', container.getJdbcUrl(),
                '--driver', 'com.mysql.cj.jdbc.Driver',
                '--dialect', 'mysql',
                '--locations', 'file:src/test/resources/migrate-db/mysql' ]

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
    }
}
