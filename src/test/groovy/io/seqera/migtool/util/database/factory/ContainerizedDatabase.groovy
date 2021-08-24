package io.seqera.migtool.util.database.factory

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import io.seqera.migtool.util.database.DbConfig
import org.testcontainers.containers.GenericContainer

@Slf4j
@CompileStatic
abstract class ContainerizedDatabase extends AbstractDatabase {

    protected GenericContainer container

    @Override
    @Memoized
    DbConfig getConfig() {
        if (!container?.running)
            throw new IllegalStateException('Container still not running')

        return createConfig()
    }

    protected void start() {
        log.debug("Starting container")
        container.start()
    }

    protected abstract DbConfig createConfig()

}
