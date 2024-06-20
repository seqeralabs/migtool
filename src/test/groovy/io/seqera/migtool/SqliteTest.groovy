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

import spock.lang.Shared
import spock.lang.Specification


class SqliteTest extends Specification {

    @Shared String dbName

    def setupSpec() {
        dbName = "db_${UUID.randomUUID().toString()}.db"
    }

    def cleanupSpec() {
        def dbFile = new File(dbName)
        if (dbFile.exists()) dbFile.delete()
    }

    def 'should do something'  () {
        given:
        def tool = new MigTool.Builder()
                .withDriver('org.sqlite.JDBC')
                .withDialect('sqlite')
                .withUser("user")
                .withPassword("password")
                .withUrl("jdbc:sqlite:${dbName}")
                .withLocations('file:src/test/resources/migrate-db/sqlite')
                .build()

        when:
        tool.run()

        then:
        tool.existTable(tool.getConnection(), 'organization')
        tool.existTable(tool.getConnection(), 'license')
        !tool.existTable(tool.getConnection(), 'foo')

    }


}
