package br.com.guiarq.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // ROTAS QUE NÃO PRECISAM DE LOGIN
                        .requestMatchers(
                                "/actuator/**",
                                "/api/auth/**",
                                "/api/stripe/**",
                                "/api/email/**",
                                "/api/public/**"
                        ).permitAll()

                        // ROTAS PROTEGIDAS: COMERCIANTE PRECISA ESTAR AUTENTICADO
                        .requestMatchers(
                                "/api/tickets/**"
                        ).authenticated()

                        // QUALQUER OUTRA ROTA: LIBERADO
                        .anyRequest().permitAll()
                )

                // SEM SESSÃO STATELESS POR ENQUANTO (PODEMOS ADICIONAR JWT DEPOIS)
                .httpBasic(basic -> {})
                .formLogin(login -> login.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
