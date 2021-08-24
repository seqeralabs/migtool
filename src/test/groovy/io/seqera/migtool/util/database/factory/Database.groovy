
package io.seqera.migtool.util.database.factory

import groovy.transform.CompileStatic
import io.seqera.migtool.util.database.DbConfig

@CompileStatic
interface Database {

    DbConfig getConfig()

    void cleanup()

}
