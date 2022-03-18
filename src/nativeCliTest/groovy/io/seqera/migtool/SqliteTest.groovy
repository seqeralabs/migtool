package io.seqera.migtool

import groovy.sql.Sql
import org.testcontainers.containers.MySQLContainer
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Timeout


@Timeout(30)
@Requires({System.getenv('NATIVE_BINARY_PATH')})
class SqliteTest extends Specification {

    static MySQLContainer container

    String dbName;
    def setupSpec() {
        this.dbName = "db_${UUID.randomUUID().toString()}.db"
    }

    def 'should run native binary' () {
        given:
        def BIN = System.getenv('NATIVE_BINARY_PATH')
        def CLI = [BIN,
                '-u', 'user',
                '-p', 'pass',
                '--url', "jdbc:sqlite:${this.dbName}",
                '--pattern', '^V(\\d+)__(.+)',
                '--location', 'file:src/test/resources/migrate-db/sqlite' ]

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
                .rows("SELECT * from license").size() == 0
    }
}
