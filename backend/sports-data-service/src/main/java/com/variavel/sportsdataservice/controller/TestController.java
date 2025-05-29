package com.variavel.sportsdataservice.controller;

import com.variavel.sportsdataservice.api.FootballApiClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/test")
public class TestController {

    private final FootballApiClient footballApiClient;

    public TestController(FootballApiClient footballApiClient) {
        this.footballApiClient = footballApiClient;
    }

    @GetMapping("/leagues")
    public Mono<String> getLeaguesFromApi() {
        return footballApiClient.getLeagues();
    }

    @GetMapping("/live-matches")
    public Mono<String> getLiveMatchesFromApi() {
        return footballApiClient.getLiveMatches();
    }
}