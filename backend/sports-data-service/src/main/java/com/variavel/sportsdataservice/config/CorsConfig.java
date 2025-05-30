package com.variavel.sportsdataservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry; // Importe CorsRegistry do Reactive
import org.springframework.web.reactive.config.EnableWebFlux; // Use EnableWebFlux para WebClient/Reactive
import org.springframework.web.reactive.config.WebFluxConfigurer; // Use WebFluxConfigurer para Reactive

@Configuration
@EnableWebFlux // Habilita o suporte a WebFlux no Spring, necessário para WebClient e CORS reativo
public class CorsConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Permite CORS para todos os endpoints
                .allowedOrigins("http://localhost:4200") // Permite requisições do seu frontend Angular
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Métodos permitidos
                .allowedHeaders("*") // Permite todos os cabeçalhos
                .allowCredentials(true) // Permite credenciais (cookies, tokens de autenticação)
                .maxAge(3600); // Tempo de cache para preflight requests
    }
}