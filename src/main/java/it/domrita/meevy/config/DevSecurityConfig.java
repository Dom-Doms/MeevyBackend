package it.domrita.meevy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/*
 * Configurazione temporanea per i test locali.
 *
 * Permette di provare Subject senza JWT.
 * Non deve essere utilizzata sul server pubblico.
 */
@Configuration
@Profile("dev")

public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
        /*
         * Configurazione temporanea per i test locali.
         *
         * Permette di provare Subject senza JWT.
         * Non deve essere utilizzata sul server pubblico.
         */
                .csrf(csrf -> csrf.ignoringRequestMatchers( //protezione di Spring Security, escludiamo gli endpoint di prova
                        "/api/subjects",
                        "/api/subjects/**",
                        "/api/users/register"
                ))
        // Disabilita i login predefiniti di Spring.
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/subjects",
                                "/api/subjects/**"
                        ).permitAll() //le richieste con "permitAll() non richiedono autenticazione.

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/register"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        .anyRequest().denyAll()
                );

        return http.build();
    }

}
