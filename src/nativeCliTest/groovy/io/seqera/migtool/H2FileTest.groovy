package io.seqera.migtool

import groovy.sql.Sql
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Timeout

import java.nio.file.Files
import java.nio.file.Path

@Timeout(30)
@Requires({System.getenv('NATIVE_BINARY_PATH')})
class H2FileTest extends Specification {

    Path dbName;

    def setup() {
        dbName = Files.createTempFile("db_${UUID.randomUUID().toString()}","db")
    }

    def 'should run native binary' () {
        given:
        def BIN = System.getenv('NATIVE_BINARY_PATH')
        def CLI = [BIN,
                '-u', 'sa',
                '-p', '',
                '--url', "jdbc:h2:$dbName".toString(),
                '--pattern', '^V(\\d+)__(.+)',
                '--location', 'file:src/nativeCliTest/resources/migrate-db/h2' ]

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
        Sql.newInstance("jdbc:h2:$dbName", "sa", "")
                .rows("SELECT count(*) from license").size() == 1
    }
}
