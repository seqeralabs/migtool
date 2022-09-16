package io.seqera.migtool

import org.testcontainers.containers.MySQLContainer

import spock.lang.Specification

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

    def 'should run a failing Groovy script' () {
        given:
        def tool = new MigTool()
                .withDriver('com.mysql.cj.jdbc.Driver')
                .withDialect('mysql')
                .withUrl(container.getJdbcUrl())
                .withUser(container.getUsername())
                .withPassword(container.getPassword())
                .withLocations('file:src/test/resources/migrate-db/mysql')

        and:
        def script = '''
             // Some valid statements
             def a = 1
             def b = 'hello world'
             String c = null
             // A failing statement at line 7:
             c.size()
        '''
        def record = new MigRecord(rank: 2, script: 'V02__groovy-script.groovy', checksum: 'whatever', statements: [script])

        when:
        tool.run()
        tool.runGroovyMigration(record)

        then: 'an exception is thrown'
        def e = thrown(IllegalStateException)
        e.message.startsWith('GROOVY MIGRATION FAILED')

        and: 'the root cause is present and the stack trace contains the expected offending line number'
        e.cause.class == NullPointerException
        e.cause.message == 'Cannot invoke method size() on null object'
        e.cause.stackTrace.join('\n').contains('.groovy:7')
    }

}
