package io.seqera.migtool

import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Timeout

import java.nio.file.Files
import java.nio.file.Path

import groovy.sql.Sql

@Timeout(30)
@Requires({ System.getenv('NATIVE_BINARY_PATH') })
class SqliteTest extends Specification {

    Path dbName;

    def setup() {
        dbName = Files.createTempFile(
                "db_${UUID.randomUUID().toString()}",
                "db"
        )
    }

    def 'should run native binary'() {
        given:
        def BIN = System.getenv('NATIVE_BINARY_PATH')
        def CLI = [BIN,
                   '-u', 'user',
                   '-p', 'pass',
                   '--driver', 'org.sqlite.JDBC',
                   '--url', "jdbc:sqlite:/$dbName".toString(),
                   '--pattern', '^V(\\d+)__(.+)',
                   '--location', 'file:src/nativeCliTest/resources/migrate-db/sqlite']

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
        Sql.newInstance(
                "jdbc:sqlite:/$dbName",
                "user",
                "pass"
        )
           .rows("SELECT count(*) from license").size() == 1
    }
}
