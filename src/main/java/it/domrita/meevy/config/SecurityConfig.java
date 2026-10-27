
package it.domrita.meevy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/*
 * Configurazione della sicurezza JWT di Meevy.
 *
 * Login, registrazione e refresh sono pubblici.
 * Tutti gli altri endpoint richiedono autenticazione.
 *
 * Spring Security verifica automaticamente i JWT
 * ricevuti nell'header Authorization: Bearer <token>.
 */
@Configuration
@Profile("!dev")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                /*
                 * Disabilita CSRF per le API REST stateless.
                 * L'autenticazione usa token Bearer negli header
                 * e non cookie di sessione.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                // Disabilita i sistemi di login predefiniti.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                /*
                 * Non crea sessioni HTTP per autenticare
                 * gli utenti: ogni richiesta usa il JWT.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Definisce quali endpoint sono pubblici.
                .authorizeHttpRequests(auth -> auth

                        // Registrazione senza autenticazione.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/users/register"
                        ).permitAll()

                        // Login tramite email e password.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/login"
                        ).permitAll()

                        // Rinnovo e terminazione della sessione.
                        // Non richiedono un access JWT valido.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/refresh",
                                "/auth/logout"
                        ).permitAll()

                        // Permette di restituire gli errori HTTP.
                        .requestMatchers("/error").permitAll()

                        // Tutti gli altri endpoint richiedono JWT.
                        .anyRequest().authenticated()
                )

                /*
                 * Attiva il supporto JWT di Spring Security.
                 * Firma, scadenza ed issuer vengono verificati
                 * usando il JwtDecoder configurato in JwtConfig.
                 */
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                );

        return http.build();
    }
}
