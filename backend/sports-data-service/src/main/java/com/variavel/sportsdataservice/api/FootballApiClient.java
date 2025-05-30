package com.variavel.sportsdataservice.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class FootballApiClient {

    private final WebClient webClient;

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
                .uri("/leagues")
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getTeamsByLeagueAndSeason(Integer leagueId, Integer season) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    // Este método é NOVO para buscar fixtures por liga, temporada e DATA
    public Mono<String> getFixturesByLeagueSeasonAndDate(Integer leagueId, Integer season, String date) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getLiveMatches() {
        return webClient.get()
                .uri("/fixtures?live=all")
                .retrieve()
                .bodyToMono(String.class);
    }
}