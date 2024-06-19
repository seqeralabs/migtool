package io.seqera.migtool

import spock.lang.Specification

class HelperTest extends Specification  {

    def 'should load resources' () {
        expect:
        Helper.getResourceFiles('/db/mysql') == ['/db/mysql/file1.sql'].toSet()
        Helper.getResourceFiles('/db/mariadb') == ['/db/mariadb/V01__maria1.sql', '/db/mariadb/V02__maria2.sql', '/db/mariadb/v01-foo.txt'].toSet()
        Helper.getResourceFiles('/db/postgres') == ['/db/postgres/V01__postgres.sql'].toSet()
    }

    def 'should read resource file' () {
        expect:
        Helper.getResourceAsString('db/mysql/file1.sql').trim() == 'select * from my-table;'
        Helper.getResourceAsString('db/mariadb/V01__maria1.sql') == 'create table XXX ( col1 varchar(1) );\n'
        Helper.getResourceAsString('db/postgres/V01__postgres.sql').trim() == 'select * from my-table;'
    }
}
