package com.variavel.sportsdataservice.controller;

import com.variavel.sportsdataservice.api.FootballApiClient;
import com.variavel.sportsdataservice.service.DataIngestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/data-ingestion")
public class TestController {

    private final FootballApiClient footballApiClient;
    private final DataIngestionService dataIngestionService;

    public TestController(FootballApiClient footballApiClient, DataIngestionService dataIngestionService) {
        this.footballApiClient = footballApiClient;
        this.dataIngestionService = dataIngestionService;
    }

    // Endpoint para testar diretamente a API de ligas (sem salvar no DB)
    @GetMapping("/leagues-raw") // Renomeado para evitar conflito com o agendador e ser mais descritivo
    public Mono<String> getLeaguesFromApiRaw() {
        return footballApiClient.getLeagues();
    }

    // Endpoint para testar diretamente a API de partidas ao vivo (sem salvar no DB)
    @GetMapping("/live-matches-raw") // Renomeado para ser mais descritivo
    public Mono<String> getLiveMatchesFromApiRaw() {
        return footballApiClient.getLiveMatches();
    }

    // --- Endpoints para Disparar Ingestão Manualmente (para testes ou necessidades pontuais) ---

    @GetMapping("/ingest-leagues-manual")
    public Mono<String> ingestLeaguesManual() {
        // Dispara o método de serviço que busca e salva/atualiza ligas
        dataIngestionService.ingestLeagues().subscribe(); // Não bloqueia, apenas dispara
        return Mono.just("Ingestão manual de ligas iniciada. Verifique os logs da aplicação.");
    }

    @GetMapping("/ingest-teams-manual")
    public Mono<String> ingestTeamsManual(@RequestParam Integer leagueId, @RequestParam Integer season) {
        // Dispara o método de serviço que busca e salva/atualiza times para uma liga/temporada
        dataIngestionService.ingestTeamsForLeague(leagueId, season).subscribe();
        return Mono.just(String.format("Ingestão manual de times para a liga %d na temporada %d iniciada. Verifique os logs.", leagueId, season));
    }

    @GetMapping("/ingest-fixtures-manual")
    public Mono<String> ingestFixturesManual(@RequestParam Integer leagueId, @RequestParam Integer season, @RequestParam(required = false) String date) {
        // Dispara o método de serviço que busca e salva/atualiza partidas para uma liga/temporada/data
        String dateToUse = (date != null && !date.isEmpty()) ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        dataIngestionService.ingestFixturesForDate(leagueId, dateToUse, season).subscribe();
        return Mono.just(String.format("Ingestão manual de partidas para a liga %d na temporada %d e data %s iniciada. Verifique os logs.", leagueId, season, dateToUse));
    }

    @GetMapping("/ingest-live-fixtures-manual") // Novo endpoint para testar o agendador de live
    public Mono<String> ingestLiveFixturesManual() {
        dataIngestionService.scheduledIngestLiveFixtures(); // Chama diretamente o método agendado
        return Mono.just("Ingestão manual de partidas ao vivo iniciada. Verifique os logs.");
    }
}