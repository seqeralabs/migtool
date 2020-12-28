package io.seqera.migtool

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class HelperTest extends Specification {

    def 'should load resources' () {

        expect:
        Helper.getResourceFiles('/db/mysql') == ['/db/mysql/file1.sql']
        Helper.getResourceFiles('/db/mariadb') == ['/db/mariadb/V01__maria1.sql', '/db/mariadb/V02__maria2.sql']
        and:
        Helper.getResourceFiles('/db/mysql/') == ['/db/mysql/file1.sql']
        Helper.getResourceFiles('/db/mariadb/') == ['/db/mariadb/V01__maria1.sql', '/db/mariadb/V02__maria2.sql']
        and:
        Helper.getResourceFiles('db/mysql') == ['db/mysql/file1.sql']
        Helper.getResourceFiles('db/mariadb') == ['db/mariadb/V01__maria1.sql', 'db/mariadb/V02__maria2.sql']
    }

    def 'should read resource file' () {

        expect:
        Helper.getResourceAsString('db/mysql/file1.sql').trim() == 'select * from my-table;'
        Helper.getResourceAsString('db/mariadb/V01__maria1.sql') == 'select * from table1;\n'
        Helper.getResourceAsString('db/mariadb/V02__maria2.sql') == 'select * from table2;\n\nupdate table table3;;\n\n'
        and:
        Helper.getResourceAsString('/db/mysql/file1.sql').trim() == 'select * from my-table;'
        Helper.getResourceAsString('/db/mariadb/V01__maria1.sql') == 'select * from table1;\n'
        Helper.getResourceAsString('/db/mariadb/V02__maria2.sql') == 'select * from table2;\n\nupdate table table3;;\n\n'

    }

    def 'should compute hash' () {
        expect:
        Helper.computeSha256('Hello world') == '64ec88ca00b268e5ba1a35678a1b5316d212f4f366b2477232534a8aeca37f3c'
        Helper.computeSha256('Hola mundo' ) == 'ca8f60b2cc7f05837d98b208b57fb6481553fc5f1219d59618fd025002a66f5c'
    }

}
