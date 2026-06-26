package com.duoc.pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitamos CSRF ya que las APIs REST con tokens JWT son stateless
            .csrf(csrf -> csrf.disable())
            
            // Definición estricta de accesos basados en roles/claims
            .authorizeHttpRequests(auth -> auth
                // 1. Requerimiento: El rol 'consulta' SOLO puede usar el endpoint de Descargar guias (GET /api/pedidos/{id})
                .requestMatchers(HttpMethod.GET, "/api/pedidos/{id}").hasAnyAuthority("ROLE_consulta", "ROLE_gestion")
                
                // 2. Requerimiento: El rol 'gestion' tiene permitido el uso del resto de endpoints
                .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAuthority("ROLE_gestion")
                .requestMatchers(HttpMethod.GET, "/api/pedidos").hasAuthority("ROLE_gestion")
                .requestMatchers(HttpMethod.PUT, "/api/pedidos/{id}").hasAuthority("ROLE_gestion")
                .requestMatchers(HttpMethod.DELETE, "/api/pedidos/{id}").hasAuthority("ROLE_gestion")
                
                // Cualquier otra ruta no especificada requerirá autenticación general
                .anyRequest().authenticated()
            )
            
            // Configuramos la aplicación como un Servidor de Recursos OAuth2 para validar JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
            
        return http.build();
    }

    /**
     * Convertidor personalizado para mapear el Custom Claim de Azure AD B2C "extension_Role"
     * hacia las GrantedAuthorities de Spring Security con el prefijo estándar "ROLE_".
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extraemos el valor del atributo personalizado en Azure Portal
            String roleAttribute = jwt.getClaimAsString("extension_Role");
            
            if (roleAttribute != null && !roleAttribute.isEmpty()) {
                // Retornamos la autoridad formateada (Ej: "ROLE_consulta" o "ROLE_gestion")
                return List.of(new SimpleGrantedAuthority("ROLE_" + roleAttribute));
            }
            
            return Collections.emptyList();
        });
        return converter;
    }
}
