package br.com.guiarq;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "br.com.guiarq")
public class GuiaRQApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuiaRQApplication.class, args);
    }
    @PostConstruct
    public void debugEnv() {
        System.out.println("====== VARIÁVEIS CAPTURADAS PELO SPRING ======");
        System.out.println("JDBC_DATABASE_URL = " + System.getenv("JDBC_DATABASE_URL"));
        System.out.println("DATABASE_URL = " + System.getenv("DATABASE_URL"));
        System.out.println("SPRING_PROFILES_ACTIVE = " + System.getenv("SPRING_PROFILES_ACTIVE"));
        System.out.println("===============================================");
    }

}
