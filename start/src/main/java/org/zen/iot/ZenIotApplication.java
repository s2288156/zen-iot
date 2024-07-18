package org.zen.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author Wu.Chunyang
 */
@EnableJpaRepositories("org.zen.iot.dao.repository")
@SpringBootApplication
public class ZenIotApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZenIotApplication.class, args);
    }
}
