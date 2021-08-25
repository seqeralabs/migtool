package io.seqera.migtool.extractor

import io.seqera.migtool.exception.ConnectionException
import io.seqera.migtool.exception.InvalidDriverException
import io.seqera.migtool.exception.StatementException
import io.seqera.migtool.executor.StatementExecutor
import io.seqera.migtool.util.database.DatabaseSpecification

class StatementExecutorTest extends DatabaseSpecification {

    void "check if a table exists"() {
        given: 'a statement executor'
        def executor = executor()

        and: 'a table'
        final tableName = 'MY_TABLE'
        executor.execute("CREATE TABLE ${tableName} ( col1 VARCHAR(1) );")

        expect: 'the table exists'
        executor.existTable(tableName)

        and: 'no other table exist'
        !executor.existTable('OTHER_TABLE')
    }

    void "execute a set of simple statements"() {
        given: 'a statement executor'
        def executor = executor()

        when: 'create a table with a primary key'
        final tableName = 'MY_TABLE'
        def result = executor.execute("CREATE TABLE ${tableName} ( id VARCHAR(1) NOT NULL, PRIMARY KEY (id) );")

        then: 'the result is as expected'
        !result.isQuery()

        when: 'insert a row'
        result = executor.execute("INSERT INTO ${tableName} (id) VALUES ('A')")

        then: 'the result is as expected'
        !result.isQuery()

        when: 'query the table'
        result = executor.execute("SELECT * FROM ${tableName}")

        then: 'the result is as expected'
        result.isQuery()
        !result.rowSet.isEmpty()

        when: 'try to insert a row with the same primary key again'
        executor.execute("INSERT INTO ${tableName} (id) VALUES ('A')")

        then: 'an exception is thrown'
        thrown(StatementException)
    }

    void "execute a set of parameterized statements"() {
        given: 'a statement executor'
        def executor = executor()

        and: 'create a table with a primary key'
        final tableName = 'MY_TABLE'
        executor.execute("CREATE TABLE ${tableName} ( id INT NOT NULL, name VARCHAR(1), PRIMARY KEY (id) );")

        when: 'insert a row'
        def result = executor.executeParameterized("INSERT INTO ${tableName} (id, name) VALUES (?, ?)", [1, 'A'])

        then: 'the result is as expected'
        !result.isQuery()

        when: 'query the table'
        result = executor.executeParameterized("SELECT * FROM ${tableName} WHERE id = ? AND name = ?", [1, 'A'])

        then: 'the result is as expected'
        result.isQuery()
        !result.rowSet.isEmpty()

        when: 'try to insert a row with the same primary key again'
        executor.executeParameterized("INSERT INTO ${tableName} (id, name) VALUES (?, ?)", [1, 'A'])

        then: 'an exception is thrown'
        thrown(StatementException)
    }

    void "try to load a driver which is not in the classpath"() {
        when: 'load a driver'
        StatementExecutor.loadDriver("com.ibm.db2.jcc.DB2Driver")

        then: 'the driver was not found'
        thrown(InvalidDriverException)
    }

    void "try to create an executor for a database with an invalid URL"() {
        given: 'load a valid driver'
        StatementExecutor.loadDriver('org.h2.Driver')

        when: 'create a new instance of an executor with an invalid URL'
        def executor = new StatementExecutor('jdbc:h2:wrong:url', 'sa', '')

        then: 'the connection could not be established'
        thrown(ConnectionException)
    }

    private static StatementExecutor executor() {
        StatementExecutor.loadDriver(database.config.driver)
        return new StatementExecutor(database.config.url, database.config.user, database.config.password)
    }

}
