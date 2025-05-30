package com.variavel.sportsdataservice.controller;

import com.variavel.sportsdataservice.dto.FixtureDto;
import com.variavel.sportsdataservice.dto.LeagueDto;
import com.variavel.sportsdataservice.dto.TeamDto;
import com.variavel.sportsdataservice.service.SportsQueryService; // Injete o novo SportsQueryService
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sports") // Prefixo da API para o frontend
@RequiredArgsConstructor
public class SportsController {

    private final SportsQueryService sportsQueryService; // Injeta o serviço de query

    // --- Endpoints para Ligas ---
    @GetMapping("/leagues")
    public ResponseEntity<List<LeagueDto>> getAllLeagues() {
        List<LeagueDto> leagues = sportsQueryService.getAllLeagues();
        return ResponseEntity.ok(leagues);
    }

    @GetMapping("/leagues/{id}")
    public ResponseEntity<LeagueDto> getLeagueById(@PathVariable Long id) {
        return sportsQueryService.getLeagueById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Endpoints para Times ---
    @GetMapping("/teams")
    public ResponseEntity<List<TeamDto>> getAllTeams() {
        List<TeamDto> teams = sportsQueryService.getAllTeams();
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<TeamDto> getTeamById(@PathVariable Long id) {
        return sportsQueryService.getTeamById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Endpoints para Partidas (Fixtures) ---
    @GetMapping("/fixtures")
    public ResponseEntity<List<FixtureDto>> getAllFixtures() {
        List<FixtureDto> fixtures = sportsQueryService.getAllFixtures();
        return ResponseEntity.ok(fixtures);
    }

    @GetMapping("/fixtures/{id}")
    public ResponseEntity<FixtureDto> getFixtureById(@PathVariable Long id) {
        return sportsQueryService.getFixtureById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/fixtures/live")
    public ResponseEntity<List<FixtureDto>> getLiveFixtures() {
        List<FixtureDto> liveFixtures = sportsQueryService.getLiveFixtures();
        return ResponseEntity.ok(liveFixtures);
    }

    @GetMapping("/fixtures/by-date")
    public ResponseEntity<List<FixtureDto>> getFixturesByDate(@RequestParam("date") LocalDate date) {
        List<FixtureDto> fixtures = sportsQueryService.getFixturesByDate(date);
        return ResponseEntity.ok(fixtures);
    }
}