/*
 * This Spock specification was generated by the Gradle 'init' task.
 */
package io.seqera.migtool

import java.nio.file.Files
import io.seqera.migtool.resources.ClassFromJarWithResources

import spock.lang.Specification

class MigToolTest extends Specification {


    def 'should init migtool' () {
        given:
        def tool = new MigTool()
            .withDriver('org.h2.Driver')
            .withDialect('h2')
            .withUrl('jdbc:h2:mem:test')
            .withUser('sa')
            .withPassword('')
            .withLocations('classpath:test')

        when:
        tool.init()
        then:
        tool.connection != null
        and:
        !tool.existTable(MigTool.MIGTOOL_TABLE)

        when:
        tool.createIfNotExists()
        then:
        tool.existTable(MigTool.MIGTOOL_TABLE)
    }


    def 'should apply local file migration' () {
        given:
        def folder = Files.createTempDirectory('test')
        folder.resolve('V01__file1.sql').text = 'create table XXX ( col1 varchar(1) ); '
        folder.resolve('V02__file2.sql').text = 'create table YYY ( col2 varchar(2) ); create table ZZZ ( col3 varchar(3) );'
        folder.resolve('x03__xyz.txt').text = 'This field should be ignored'
        and:

        def tool = new MigTool()
                .withDriver('org.h2.Driver')
                .withDialect('h2')
                .withUrl('jdbc:h2:mem:test')
                .withUser('sa')
                .withPassword('')
                .withLocations("file:$folder")

        when:
        tool.init()
        and:
        tool.scanMigrations()
        then:
        tool.migrationEntries.size()==2
        and:
        with(tool.migrationEntries[0]) {
            rank == 1
            script == 'V01__file1.sql'
            statements == ['create table XXX ( col1 varchar(1) );']
        }
        and:
        with(tool.migrationEntries[1]) {
            rank == 2
            script == 'V02__file2.sql'
            statements == ['create table YYY ( col2 varchar(2) );', 'create table ZZZ ( col3 varchar(3) );']
        }

        when:
        tool.createIfNotExists()
        tool.apply()
        then:
        tool.existTable('XXX')
        tool.existTable('YYY')
        tool.existTable('ZZZ')
        and:
        !tool.existTable('FOO')

        cleanup:
        folder?.deleteDir()
    }

    def 'should apply class path migration' () {
        given:
        def tool = new MigTool()
                .withDriver('org.h2.Driver')
                .withDialect('h2')
                .withUrl('jdbc:h2:mem:test')
                .withUser('sa')
                .withPassword('')
                .withLocations("classpath:db/mariadb")

        when:
        tool.init()
        and:
        tool.scanMigrations()
        then:
        tool.migrationEntries.size()==2
        and:
        with(tool.migrationEntries[0]) {
            rank == 1
            script == 'V01__maria1.sql'
            statements == ['create table XXX ( col1 varchar(1) );']
        }
        and:
        with(tool.migrationEntries[1]) {
            rank == 2
            script == 'V02__maria2.sql'
            statements == ['create table YYY ( col2 varchar(2) );', 'create table ZZZ ( col3 varchar(3) );']
        }

        when:
        tool.createIfNotExists()
        tool.apply()
        then:
        tool.existTable('XXX')
        tool.existTable('YYY')
        tool.existTable('ZZZ')
        and:
        !tool.existTable('FOO')
    }

    def 'should apply class path migration with custom pattern' () {
        given:
        def tool = new MigTool()
                .withDriver('org.h2.Driver')
                .withDialect('h2')
                .withUrl('jdbc:h2:mem:test')
                .withUser('sa')
                .withPassword('')
                .withLocations("classpath:db/mariadb")
                .withPattern(/v(\d\d)-.+/)

        when:
        tool.init()
        and:
        tool.scanMigrations()
        then:
        tool.migrationEntries.size()==1
        and:
        with(tool.migrationEntries[0]) {
            rank == 1
            script == 'v01-foo.txt'
            statements == ['create table CUSTOM ( col4 varchar(4) );']
        }
        
    }

    def 'should apply migration coming from a jar file' () {
        given:
        def tool = new MigTool()
                .withDriver('org.h2.Driver')
                .withDialect('h2')
                .withUrl('jdbc:h2:mem:test')
                .withUser('sa')
                .withPassword('')
                .withClassLoader(ClassFromJarWithResources.classLoader)
                .withLocations("classpath:db/migrations")

        when:
        tool.init()
        and:
        tool.scanMigrations()
        then:
        tool.migrationEntries.size()==2
        and:
        with(tool.migrationEntries[0]) {
            rank == 1
            script == 'V01__file1.sql'
            statements == ['create table XXX ( col1 varchar(1) );']
        }
        and:
        with(tool.migrationEntries[1]) {
            rank == 2
            script == 'V02__file2.sql'
            statements == ['create table YYY ( col2 varchar(2) );', 'create table ZZZ ( col3 varchar(3) );']
        }

        when:
        tool.createIfNotExists()
        tool.apply()
        then:
        tool.existTable('XXX')
        tool.existTable('YYY')
        tool.existTable('ZZZ')
        and:
        !tool.existTable('FOO')
    }

}
