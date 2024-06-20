package io.seqera.migtool

import spock.lang.Specification

class DriverTest extends Specification {

    def 'should get driver from url'() {
        expect:
        Driver.fromUrl(null) == null
        Driver.fromUrl('jdbc:h2:file:./.db/h2/tower') == Driver.H2
        Driver.fromUrl('jdbc:mysql://foo.com:3306/licman') == Driver.MYSQL
        Driver.fromUrl('jdbc:postgresql://localhost:5432/test') == Driver.POSTGRES
    }
}
