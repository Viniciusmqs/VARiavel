package com.variavel.sportsdataservice.controller;

import com.variavel.sportsdataservice.api.FootballApiClient;
import com.variavel.sportsdataservice.service.DataIngestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/data-ingestion") // Novo prefixo para este controller
public class TestController { // Renomeie para DataIngestionController se quiser, mas TestController funciona

    private final FootballApiClient footballApiClient;
    private final DataIngestionService dataIngestionService; // Injete o novo serviço

    public TestController(FootballApiClient footballApiClient, DataIngestionService dataIngestionService) {
        this.footballApiClient = footballApiClient;
        this.dataIngestionService = dataIngestionService;
    }

    @GetMapping("/leagues")
    public Mono<String> getLeaguesFromApi() {
        // Este endpoint ainda retorna o JSON bruto, para testes rápidos da API
        return footballApiClient.getLeagues();
    }

    @GetMapping("/ingest-leagues")
    public Mono<String> ingestLeagues() {
        return dataIngestionService.ingestLeagues()
                .thenReturn("Ingestão de ligas iniciada com sucesso! Verifique os logs para mais detalhes.");
    }

    @GetMapping("/ingest-teams")
    public Mono<String> ingestTeams(@RequestParam Integer leagueId, @RequestParam Integer season) {
        return dataIngestionService.ingestTeamsForLeague(leagueId, season)
                .thenReturn(String.format("Ingestão de times para a liga %d temporada %d iniciada. Verifique os logs.", leagueId, season));
    }

    @GetMapping("/ingest-fixtures")
    public Mono<String> ingestFixtures(@RequestParam Integer leagueId, @RequestParam Integer season) {
        return dataIngestionService.ingestFixturesForLeagueAndSeason(leagueId, season)
                .thenReturn(String.format("Ingestão de partidas para a liga %d temporada %d iniciada. Verifique os logs.", leagueId, season));
    }

    @GetMapping("/live-matches") // Mantenha este se quiser continuar testando partidas ao vivo diretamente
    public Mono<String> getLiveMatchesFromApi() {
        return footballApiClient.getLiveMatches();
    }
}