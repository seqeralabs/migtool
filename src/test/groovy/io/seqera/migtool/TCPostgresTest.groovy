package io.seqera.migtool


import spock.lang.Specification

import org.postgresql.util.PSQLException
import org.testcontainers.containers.PostgreSQLContainer

/**
 * This class aims to test the driver used to connect to a Postgres database when we use an url of the type
 * `jdbc:tc:postgresql:16-alpine:///database`, which should be similar to the normal Postgres driver.
 * More information about this can be found in the [Testcontainers documentation](https://java.testcontainers.org/modules/databases/jdbc/).
 * **/
class TCPostgresTest extends Specification {

    static PostgreSQLContainer container

    static {
        container = new PostgreSQLContainer("postgres:16-alpine")
        // start it -- note: it's stopped automatically
        // https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/
        container.start()
    }

    MigTool tool

    def setup() {
        tool = new MigTool()
                .withDriver(Driver.TCPOSTGRES.toString())
                .withDialect(Dialect.TCPOSTGRES.toString())
                .withUrl(container.getJdbcUrl())
                .withUser(container.getUsername())
                .withPassword(container.getPassword())
                .withLocations('file:src/test/resources/migrate-db/postgres')
    }

    def 'should do something'() {
        when:
        tool.run()

        then:
        tool.existTable(tool.getConnection(), 'organization')
        tool.existTable(tool.getConnection(), 'license')
        !tool.existTable(tool.getConnection(), 'foo')
    }


    def 'should run a successful Groovy script'() {
        given: 'set up the initial tables'
        tool.run()

        when: 'run a script that inserts some data'
        def insertScript = '''
            import java.sql.Timestamp

            def now = new Timestamp(System.currentTimeMillis())
            def newOrgs = [
                ["1", "C", "C", "e@e.com", now, now],
                ["2", "C", "C", "e@e.com", now, now],
            ]

            newOrgs.each { o ->
                sql.executeInsert(
                   "INSERT INTO organization(id, company, contact, email, date_created, last_updated) VALUES (?, ?, ?, ?, ?, ?)",
                   o.toArray()
                )
            }
        '''
        def insertRecord = new MigRecord(
                rank: 2,
                script: 'V02__insert-data.groovy',
                checksum: 'checksum2',
                statements: [insertScript]
        )
        tool.runGroovyMigration(insertRecord)

        then: 'the script ran successfully'
        noExceptionThrown()

        when: 'run another script to check whether the data is present'
        def checkScript = '''
            def expectedOrgIds = ["1", "2"]

            def orgs = sql.rows("SELECT * FROM organization")
            orgs.each { o ->
                assert o.id in expectedOrgIds
            }
        '''
        def checkRecord = new MigRecord(
                rank: 3,
                script: 'V03__check-data.groovy',
                checksum: 'checksum3',
                statements: [checkScript]
        )
        tool.runGroovyMigration(checkRecord)

        then: 'the script ran successfully (the new records are present)'
        noExceptionThrown()
    }

    def 'should run a failing Groovy script'() {
        given: 'set up the initial tables'
        tool.run()

        when: 'run a script that inserts some data, but fails at some point'
        def insertScript = '''
            import java.sql.Timestamp

            def now = new Timestamp(System.currentTimeMillis())
            def newOrgs = [
                ["3", "C", "C", "e@e.com", now, now],
                ["4", "C", "C", "e@e.com", now, now],
                ["3", "C", "C", "e@e.com", now, now], // Duplicated id: will fail
            ]

            newOrgs.each { o ->
                sql.executeInsert(
                   "INSERT INTO organization(id, company, contact, email, date_created, last_updated) VALUES (?, ?, ?, ?, ?, ?)",
                   o.toArray()
                )
            }
        '''
        def insertRecord = new MigRecord(
                rank: 2,
                script: 'V02__insert-data.groovy',
                checksum: 'checksum2',
                statements: [insertScript]
        )
        tool.runGroovyMigration(insertRecord)

        then: 'an exception is thrown'
        def e = thrown(IllegalStateException)
        e.message.startsWith('GROOVY MIGRATION FAILED')

        and: 'the root cause is present and the stack trace contains the expected offending line number'
        e.cause.class == PSQLException
        e.cause.stackTrace.any {t -> t.toString() ==~ /.+\.groovy:\d+.+/
        }

        when: 'run another script to check whether the data is present'
        def checkScript = '''
            def expectedMissingOrgIds = ["3", "4"]

            def orgs = sql.rows("SELECT * FROM organization")
            orgs.each { o ->
                assert o.id !in expectedMissingOrgIds
            }
        '''
        def checkRecord = new MigRecord(
                rank: 3,
                script: 'V03__check-data.groovy',
                checksum: 'checksum3',
                statements: [checkScript]
        )
        tool.runGroovyMigration(checkRecord)

        then: 'the script ran successfully (no records were persisted: the transaction rolled back)'
        noExceptionThrown()
    }

}
