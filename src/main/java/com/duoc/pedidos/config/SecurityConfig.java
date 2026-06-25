package com.duoc.pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitamos CSRF ya que las APIs REST con tokens JWT son stateless
            .csrf(csrf -> csrf.disable())
            // Indicamos que cualquier petición a los endpoints debe estar autenticada
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            // Configuramos la aplicación como un Servidor de Recursos OAuth2 para validar JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );
            
        return http.build();
    }
}
