package io.seqera.migtool

import java.nio.file.Files

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class MigRecordTest extends Specification {

    def 'should parse file entry' () {
        given:
        def folder = Files.createTempDirectory('test')
        folder.resolve('V01__file1.sql').text = 'create table XXX ( col1 varchar(1) ); '
        folder.resolve('V02__file2.sql').text = 'create table YYY ( col2 varchar(2) );; update table YYY; '
        folder.resolve('something_else.sql').text = 'create table ZZZ ( col3 varchar(3) ); '

        when:
        def entry1 = MigRecord.parseFilePath(folder.resolve('V01__file1.sql'))
        then:
        entry1.rank == 1
        entry1.script == 'V01__file1.sql'
        entry1.statements == ['create table XXX ( col1 varchar(1) );']
        entry1.checksum == '216c372949a7178ce6bd026bf9a51a89bfc23b4a7abf3b7b6548a478a2f3d761'

        when:
        def entry2 = MigRecord.parseFilePath(folder.resolve('V02__file2.sql'))
        then:
        entry2.rank == 2
        entry2.script == 'V02__file2.sql'
        entry2.statements == ['create table YYY ( col2 varchar(2) );', 'update table YYY;']
        entry2.checksum == 'cae9a40915b7ac44199077534cefe9a0fcd4c8b8065416b4789d8c76efbff943'

        when:
        def entry3 = MigRecord.parseFilePath(folder.resolve('something_else.sql'))
        then:
        entry3 == null
        
        
        cleanup:
        folder?.deleteDir();
    }


    def 'should parse resource file'() {

        when:
        def entry1 = MigRecord.parseResourcePath('/mariadb/V01__maria1.sql')
        then:
        entry1.rank == 1
        entry1.script == 'V01__maria1.sql'
        entry1.statements == ['select * from table1;']
        entry1.checksum == 'c86dca4b3e7ab5b9591e1352e92a0e867a50e7e8f127c6f58f19446174bc87f7'


        when:
        def entry2 = MigRecord.parseResourcePath('/mariadb/V02__maria2.sql')
        then:
        entry2.rank == 2
        entry2.script == 'V02__maria2.sql'
        entry2.statements == ['select * from table2;', 'update table table3;']
        entry2.checksum == 'b502e81e5f0b9199ff1ad78226aa528c81d271b3ef704a75d2fca00ce1c9767f'

    }
}
