package br.com.guiarq;

import java.beans.Transient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = br.com.guiarq.GuiaRQApplication.class,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
            "spring.jpa.hibernate.ddl-auto=update"
        })
class GuiaRQApplicationTests {

    @Test
    void contextLoads() {
    }
}
