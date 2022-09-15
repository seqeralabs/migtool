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
        folder.resolve('m03--file3.sql').text = 'create table ZZZ ( col3 varchar(3) ); '
        folder.resolve('something_else.sql').text = 'create table QQQ ( col4 varchar(4) ); '

        when:
        def entry1 = MigRecord.parseFilePath(folder.resolve('V01__file1.sql'), null)
        then:
        entry1.rank == 1
        entry1.script == 'V01__file1.sql'
        entry1.statements == ['create table XXX ( col1 varchar(1) );']
        entry1.checksum == '216c372949a7178ce6bd026bf9a51a89bfc23b4a7abf3b7b6548a478a2f3d761'

        when:
        def entry2 = MigRecord.parseFilePath(folder.resolve('V02__file2.sql'), null)
        then:
        entry2.rank == 2
        entry2.script == 'V02__file2.sql'
        entry2.statements == ['create table YYY ( col2 varchar(2) );', 'update table YYY;']
        entry2.checksum == 'cae9a40915b7ac44199077534cefe9a0fcd4c8b8065416b4789d8c76efbff943'

        when:
        def entry3 = MigRecord.parseFilePath(folder.resolve('m03--file3.sql'), ~/m(\d\d)-.*/)
        then:
        entry3.rank == 3
        entry3.script == 'm03--file3.sql'
        entry3.statements == ['create table ZZZ ( col3 varchar(3) );']
        entry3.checksum == 'af2efc81de168cd1d4ee5a870adc186c48ad3d957ffe43990d43844018d53748'

        when:
        def entry4 = MigRecord.parseFilePath(folder.resolve('something_else.sql'), null)
        then:
        entry4 == null

        cleanup:
        folder?.deleteDir();
    }


    def 'should parse resource file'() {

        when:
        def entry1 = MigRecord.parseResourcePath('/db/mariadb/V01__maria1.sql', null)
        then:
        entry1.rank == 1
        entry1.script == 'V01__maria1.sql'
        entry1.statements == ['create table XXX ( col1 varchar(1) );']
        entry1.checksum == '216c372949a7178ce6bd026bf9a51a89bfc23b4a7abf3b7b6548a478a2f3d761'


        when:
        def entry2 = MigRecord.parseResourcePath('/db/mariadb/V02__maria2.sql', null)
        then:
        entry2.rank == 2
        entry2.script == 'V02__maria2.sql'
        entry2.statements == ['create table YYY ( col2 varchar(2) );', 'create table ZZZ ( col3 varchar(3) );']
        entry2.checksum == 'db6400814ac7eb5398c0276d547fba32deada7e2933849053b5b67b754bc2c05'

        when:
        def entry3 = MigRecord.parseResourcePath('/db/mariadb/v01-foo.txt', ~/v(\d\d)-.*/)
        then:
        entry3.rank == 1
        entry3.script == 'v01-foo.txt'
        entry3.statements == ['create table CUSTOM ( col4 varchar(4) );']
        entry3.checksum == '739220206cd13dbfc6f86d2cee11c8d7a42d190bb67004c54a02f93bbc98ff77'
    }

    def 'compare records for files with same statements'() {
        given: 'some files with the same statements'
        def folder = Files.createTempDirectory('test')

        final referenceFile = folder.resolve('V01__file1.sql')
        referenceFile.text = 'create table XXX ( col1 varchar(1) ); alter table XXX add column col2 varchar(2);'
        final externalBlanksFile = folder.resolve('V02__file2.sql')
        externalBlanksFile.text = '     create table XXX ( col1 varchar(1) );  \n\t  alter table XXX add column col2 varchar(2);  \n  '
        final internalBlanksFile = folder.resolve('V03__file3.sql')
        internalBlanksFile.text = 'create table XXX  \n\t   ( col1 varchar(1) ); alter table XXX    add column col2 varchar(2);'
        final uppercaseFile = folder.resolve('V04__file4.sql')
        uppercaseFile.text = 'CREATE TABLE XXX ( col1 VARCHAR(1) ); ALTER TABLE XXX ADD COLUMN col2 VARCHAR(2);'
        final commentsFile = folder.resolve('V05__file5.sql')
        commentsFile.text = 'create table XXX ( col1 varchar(1) );\n -- A comment\n alter table XXX add column col2 varchar(2);'

        when: 'parse both files'
        def referenceEntry = MigRecord.parseFilePath(referenceFile, null)
        def externalBlanksEntry = MigRecord.parseFilePath(externalBlanksFile, null)
        def internalBlanksEntry = MigRecord.parseFilePath(internalBlanksFile, null)
        def uppercaseEntry = MigRecord.parseFilePath(uppercaseFile, null)
        def commentsEntry = MigRecord.parseFilePath(commentsFile, null)

        then: 'the record with blanks between statements is equal'
        referenceEntry.statements == externalBlanksEntry.statements
        referenceEntry.checksum == externalBlanksEntry.checksum

        and: 'the record with blanks within the statements is not equal'
        referenceEntry.statements != internalBlanksEntry.statements
        referenceEntry.checksum != internalBlanksEntry.checksum

        and: 'the record with different case is not equal'
        referenceEntry.statements != uppercaseEntry.statements
        referenceEntry.checksum != uppercaseEntry.checksum

        and: 'the record with comments is not equal'
        referenceEntry.statements != commentsEntry.statements
        referenceEntry.checksum != commentsEntry.checksum
    }

}
