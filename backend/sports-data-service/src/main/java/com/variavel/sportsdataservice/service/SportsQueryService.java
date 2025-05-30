package com.variavel.sportsdataservice.service;

import com.variavel.sportsdataservice.domain.Fixture;
import com.variavel.sportsdataservice.domain.League;
import com.variavel.sportsdataservice.domain.Team;
import com.variavel.sportsdataservice.dto.FixtureDto;
import com.variavel.sportsdataservice.dto.LeagueDto;
import com.variavel.sportsdataservice.dto.TeamDto;
import com.variavel.sportsdataservice.repository.FixtureRepository;
import com.variavel.sportsdataservice.repository.LeagueRepository;
import com.variavel.sportsdataservice.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importe esta anotação

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Gera construtor com todos os campos final
@Transactional(readOnly = true) // Transações de leitura otimizadas
public class SportsQueryService {

    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    // --- Métodos para Ligas ---
    public List<LeagueDto> getAllLeagues() {
        return leagueRepository.findAll().stream()
                .map(this::mapToLeagueDto)
                .collect(Collectors.toList());
    }

    public Optional<LeagueDto> getLeagueById(Long id) {
        return leagueRepository.findById(id).map(this::mapToLeagueDto);
    }

    // --- Métodos para Times ---
    public List<TeamDto> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::mapToTeamDto)
                .collect(Collectors.toList());
    }

    public Optional<TeamDto> getTeamById(Long id) {
        return teamRepository.findById(id).map(this::mapToTeamDto);
    }

    // --- Métodos para Partidas (Fixtures) ---
    public List<FixtureDto> getAllFixtures() {
        return fixtureRepository.findAll().stream()
                .map(this::mapToFixtureDto)
                .collect(Collectors.toList());
    }

    public Optional<FixtureDto> getFixtureById(Long id) {
        return fixtureRepository.findById(id).map(this::mapToFixtureDto);
    }

    public List<FixtureDto> getLiveFixtures() {
        // A API-Football usa "Match Finished", "Live", "Not Started", "Match Postponed", etc.
        // Consideramos "Live" para jogos ao vivo.
        return fixtureRepository.findByStatus("Live").stream()
                .map(this::mapToFixtureDto)
                .collect(Collectors.toList());
    }

    public List<FixtureDto> getFixturesByDate(LocalDate date) {
        // Precisa converter LocalDate para Instant para comparar com a coluna 'date' (Instant) na entidade Fixture.
        // Isso pode ser um pouco complexo para garantir a precisão da data (do início ao fim do dia).
        // Para simplificar, faremos uma busca por todos os jogos e filtramos na memória por data,
        // ou você pode adicionar um método mais sofisticado no FixtureRepository que usa "BETWEEN" datas.
        // Por enquanto, uma busca simples:
        return fixtureRepository.findAll().stream()
                .filter(f -> f.getDate().atZone(ZoneId.systemDefault()).toLocalDate().isEqual(date))
                .map(this::mapToFixtureDto)
                .collect(Collectors.toList());
    }


    // --- Métodos de Mapeamento de Entidade para DTO ---
    private LeagueDto mapToLeagueDto(League league) {
        return LeagueDto.builder()
                .id(league.getId())
                .apiId(league.getApiId())
                .name(league.getName())
                .type(league.getType())
                .country(league.getCountry())
                .logoUrl(league.getLogoUrl())
                .build();
    }

    private TeamDto mapToTeamDto(Team team) {
        return TeamDto.builder()
                .id(team.getId())
                .apiId(team.getApiId())
                .name(team.getName())
                .code(team.getCode())
                .country(team.getCountry())
                .logoUrl(team.getLogoUrl())
                .build();
    }

    private FixtureDto mapToFixtureDto(Fixture fixture) {
        // Mapeia também dados relacionados (League, Home Team, Away Team)
        return FixtureDto.builder()
                .id(fixture.getId())
                .apiId(fixture.getApiId())
                .date(fixture.getDate())
                .timezone(fixture.getTimezone())
                .timestamp(fixture.getTimestamp())
                .status(fixture.getStatus())
                .elapsed(fixture.getElapsed())
                .homeGoals(fixture.getHomeGoals())
                .awayGoals(fixture.getAwayGoals())
                .homeHalfTimeGoals(fixture.getHomeHalfTimeGoals())
                .awayHalfTimeGoals(fixture.getAwayHalfTimeGoals())
                .homeExtraTimeGoals(fixture.getHomeExtraTimeGoals())
                .awayExtraTimeGoals(fixture.getAwayExtraTimeGoals())
                .homePenaltyGoals(fixture.getHomePenaltyGoals())
                .awayPenaltyGoals(fixture.getAwayPenaltyGoals())
                .venueName(fixture.getVenueName())
                .venueCity(fixture.getVenueCity())
                .referee(fixture.getReferee())
                // Dados da liga e times relacionados (verificar se não são nulos antes de acessar)
                .leagueName(fixture.getLeague() != null ? fixture.getLeague().getName() : null)
                .leagueLogoUrl(fixture.getLeague() != null ? fixture.getLeague().getLogoUrl() : null)
                .homeTeamName(fixture.getHomeTeam() != null ? fixture.getHomeTeam().getName() : null)
                .homeTeamLogoUrl(fixture.getHomeTeam() != null ? fixture.getHomeTeam().getLogoUrl() : null)
                .awayTeamName(fixture.getAwayTeam() != null ? fixture.getAwayTeam().getName() : null)
                .awayTeamLogoUrl(fixture.getAwayTeam() != null ? fixture.getAwayTeam().getLogoUrl() : null)
                .build();
    }
}