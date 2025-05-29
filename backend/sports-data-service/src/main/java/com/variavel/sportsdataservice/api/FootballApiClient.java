package com.variavel.sportsdataservice.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class FootballApiClient {

    private final WebClient webClient;

    // Injeta as propriedades do application.properties no construtor
    public FootballApiClient(@Value("${api.football.base-url}") String baseUrl,
                             @Value("${api.football.api-key}") String apiKey,
                             WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("x-rapidapi-key", apiKey)
                .defaultHeader("x-rapidapi-host", "v3.football.api-sports.io") // Host da API-Football
                .build();
    }

    public Mono<String> getLeagues() {
        return webClient.get()
                .uri("/leagues") // Endpoint para buscar ligas
                .retrieve()
                .bodyToMono(String.class); // Retorna a resposta como String (por enquanto)
    }

    public Mono<String> getLiveMatches() {
        return webClient.get()
                .uri("/fixtures?live=all") // Endpoint para buscar todos os jogos ao vivo
                .retrieve()
                .bodyToMono(String.class);
    }
}