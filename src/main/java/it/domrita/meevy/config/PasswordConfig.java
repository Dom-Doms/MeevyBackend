
package it.domrita.meevy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/*
 * Configurazione del sistema di codifica delle password.
 *
 * Permette di salvare le password in forma di hash,
 * evitando di memorizzarle in chiaro nel database.
 */
@Configuration
public class PasswordConfig {

    /*
     * Crea un Bean PasswordEncoder gestito da Spring.
     *
     * Il Bean viene utilizzato dal UserService durante
     * la registrazione per generare l'hash della password
     * e, successivamente, durante il login per verificarla.git 
     *
     * Il DelegatingPasswordEncoder utilizza bcrypt come
     * algoritmo predefinito per i nuovi hash e permette
     * di supportare algoritmi differenti in futuro.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
