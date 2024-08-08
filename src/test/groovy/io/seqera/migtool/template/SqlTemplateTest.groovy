package io.seqera.migtool.template

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class SqlTemplateTest extends Specification{

    def 'should validate default template' () {
        given:
        def t = SqlTemplate.from('anything')

        expect:
        t.selectMaxRank('FOO')  == 'select max(`rank`) from FOO'
        t.insetMigration('FOO') == 'insert into FOO (`rank`,`script`,`checksum`,`created_on`,`execution_time`) values (?,?,?,?,?)'
        t.selectMigration('FOO') == 'select `id`, `checksum`, `script` from FOO where `rank` = ? and `script` = ?'
    }

    def 'should validate postgre template' () {
        given:
        def t = SqlTemplate.from('postgresql')

        expect:
        t.selectMaxRank('FOO')  == 'select max(rank) from FOO'
        t.insetMigration('FOO') == 'insert into FOO (rank,script,checksum,created_on,execution_time) values (?,?,?,?,?)'
        t.selectMigration('FOO') == 'select id, checksum, script from FOO where rank = ? and script = ?'
    }
}
