/*
 * Copyright (c) 2019-2020, Seqera Labs.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 */
package io.seqera.migtool


import spock.lang.Specification


class SqliteTest extends Specification {

    String dbName;
    def setupSpec() {
        this.dbName = "db_${UUID.randomUUID().toString()}.db"
    }
    def 'should do something'  () {
        given:
        def tool = new MigTool()
                .withDriver('org.sqlite.JDBC')
                .withDialect('sqlite')
                .withUser("user")
                .withPassword("password")
                .withUrl("jdbc:sqlite:${this.dbName}")
                .withLocations('file:src/test/resources/migrate-db/sqlite')

        when:
        tool.run()

        then:
        tool.existTable(tool.getConnection(), 'organization')
        tool.existTable(tool.getConnection(), 'license')
        !tool.existTable(tool.getConnection(), 'foo')

    }


}
