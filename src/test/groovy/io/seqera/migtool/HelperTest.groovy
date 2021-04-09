package io.seqera.migtool

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class HelperTest extends Specification {

    def 'should load resources' () {

        expect:
        Helper.getResourceFiles('/db/mysql') == ['/db/mysql/file1.sql'].toSet()
        Helper.getResourceFiles('/db/mariadb') == ['/db/mariadb/V01__maria1.sql', '/db/mariadb/V02__maria2.sql'].toSet()
        and:
        Helper.getResourceFiles('/db/mysql/') == ['/db/mysql/file1.sql'].toSet()
        Helper.getResourceFiles('/db/mariadb/') == ['/db/mariadb/V01__maria1.sql', '/db/mariadb/V02__maria2.sql'].toSet()
        and:
        Helper.getResourceFiles('db/mysql') == ['db/mysql/file1.sql'].toSet()
        Helper.getResourceFiles('db/mariadb') == ['db/mariadb/V01__maria1.sql', 'db/mariadb/V02__maria2.sql'].toSet()
    }

    def 'should read resource file' () {
        expect:
        Helper.getResourceAsString('db/mysql/file1.sql').trim() == 'select * from my-table;'
        Helper.getResourceAsString('db/mariadb/V01__maria1.sql') == 'create table XXX ( col1 varchar(1) );\n'
        Helper.getResourceAsString('db/mariadb/V02__maria2.sql') == 'create table YYY ( col2 varchar(2) );\n\ncreate table ZZZ ( col3 varchar(3) );;\n\n'
    }

    def 'should compute hash' () {
        expect:
        Helper.computeSha256('Hello world') == '64ec88ca00b268e5ba1a35678a1b5316d212f4f366b2477232534a8aeca37f3c'
        Helper.computeSha256('Hola mundo' ) == 'ca8f60b2cc7f05837d98b208b57fb6481553fc5f1219d59618fd025002a66f5c'
    }

}
