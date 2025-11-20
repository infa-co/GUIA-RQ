package br.com.guiarq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "br.com.guiarq")
public class GuiaRQApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuiaRQApplication.class, args);
    }
}
