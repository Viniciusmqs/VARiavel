package com.variavel.sportsdataservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.variavel.sportsdataservice.api.FootballApiClient;
import com.variavel.sportsdataservice.domain.Fixture;
import com.variavel.sportsdataservice.domain.League;
import com.variavel.sportsdataservice.domain.Team;
import com.variavel.sportsdataservice.repository.FixtureRepository;
import com.variavel.sportsdataservice.repository.LeagueRepository;
import com.variavel.sportsdataservice.repository.TeamRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled; // Importe esta anotação
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
public class DataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DataIngestionService.class);

    private final FootballApiClient footballApiClient;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;
    private final ObjectMapper objectMapper;

    public DataIngestionService(FootballApiClient footballApiClient,
                                LeagueRepository leagueRepository,
                                TeamRepository teamRepository,
                                FixtureRepository fixtureRepository,
                                ObjectMapper objectMapper) {
        this.footballApiClient = footballApiClient;
        this.leagueRepository = leagueRepository;
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
        this.objectMapper = objectMapper;
    }

    // Método agendado para ingestão de ligas (executa uma vez por dia, por exemplo)
    // A sintaxe cron é "segundos minutos horas diaDoMes mes diaDaSemana"
    // "0 0 1 * * *" significa: à 1 da manhã, todo dia, todo mês, todo dia da semana.
    @Scheduled(cron = "0 0 1 * * *") // Pode ajustar o cron conforme a necessidade, ex: a cada hora: "0 0 * * * *"
    @Transactional
    public void scheduledIngestLeagues() {
        log.info("Iniciando ingestão agendada de ligas...");
        ingestLeagues().subscribe(
                null, // onNext (não faz nada com o resultado vazio)
                error -> log.error("Erro na ingestão agendada de ligas: {}", error.getMessage()), // onError
                () -> log.info("Ingestão agendada de ligas concluída.") // onComplete
        );
    }

    // Método principal de ingestão de ligas (chamado pelo agendador ou manualmente)
    @Transactional
    public Mono<Void> ingestLeagues() {
        log.info("Processando ingestão de ligas da API-Football...");
        return footballApiClient.getLeagues()
                .flatMap(jsonString -> {
                    try {
                        JsonNode rootNode = objectMapper.readTree(jsonString);
                        JsonNode responseNode = rootNode.path("response");

                        if (responseNode.isArray()) {
                            for (JsonNode leagueJson : responseNode) {
                                JsonNode leagueDetails = leagueJson.path("league");
                                JsonNode countryDetails = leagueJson.path("country");

                                Integer apiId = leagueDetails.path("id").asInt();

                                Optional<League> existingLeague = leagueRepository.findByApiId(apiId);
                                if (existingLeague.isPresent()) {
                                    // Apenas atualiza informações se a liga já existe
                                    // ou ignora se preferir não atualizar dados estáticos de ligas com frequência
                                    // Por enquanto, vamos atualizar nome, logo, etc.
                                    League leagueToUpdate = existingLeague.get();
                                    leagueToUpdate.setName(leagueDetails.path("name").asText());
                                    leagueToUpdate.setType(leagueDetails.path("type").asText());
                                    leagueToUpdate.setCountry(countryDetails.path("name").asText());
                                    leagueToUpdate.setLogoUrl(leagueDetails.path("logo").asText());
                                    leagueRepository.save(leagueToUpdate); // Salva para atualizar
                                    log.debug("Liga com API ID {} atualizada: {}", apiId, leagueToUpdate.getName());
                                } else {
                                    League league = new League();
                                    league.setApiId(apiId);
                                    league.setName(leagueDetails.path("name").asText());
                                    league.setType(leagueDetails.path("type").asText());
                                    league.setCountry(countryDetails.path("name").asText());
                                    league.setLogoUrl(leagueDetails.path("logo").asText());
                                    leagueRepository.save(league);
                                    log.info("Liga salva: {}", league.getName());
                                }
                            }
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        log.error("Erro ao processar JSON de ligas: {}", e.getMessage(), e);
                        return Mono.error(new RuntimeException("Failed to ingest leagues", e));
                    }
                })
                .then();
    }

    // --- Ingestão de Times (exemplo simplificado) ---
    // Este método ainda não será agendado por enquanto, pois a API-Football tem limites.
    // Focaremos em agendar times/fixtures para ligas e temporadas específicas.
    @Transactional
    public Mono<Void> ingestTeamsForLeague(Integer leagueApiId, Integer seasonYear) {
        log.info("Processando ingestão de times para a liga API ID {} na temporada {}", leagueApiId, seasonYear);
        return footballApiClient.getTeamsByLeagueAndSeason(leagueApiId, seasonYear)
                .flatMap(jsonString -> {
                    try {
                        JsonNode rootNode = objectMapper.readTree(jsonString);
                        JsonNode responseNode = rootNode.path("response");

                        if (responseNode.isArray()) {
                            for (JsonNode teamJsonWrapper : responseNode) {
                                JsonNode teamDetails = teamJsonWrapper.path("team");
                                Integer apiId = teamDetails.path("id").asInt();

                                Optional<Team> existingTeam = teamRepository.findByApiId(apiId);
                                if (existingTeam.isPresent()) {
                                    // Atualiza informações do time
                                    Team teamToUpdate = existingTeam.get();
                                    teamToUpdate.setName(teamDetails.path("name").asText());
                                    teamToUpdate.setCode(teamDetails.path("code").asText());
                                    teamToUpdate.setCountry(teamDetails.path("country").asText());
                                    teamToUpdate.setFounded(teamDetails.path("founded").asInt());
                                    teamToUpdate.setNational(teamDetails.path("national").asBoolean());
                                    teamToUpdate.setLogoUrl(teamDetails.path("logo").asText());
                                    teamRepository.save(teamToUpdate);
                                    log.debug("Time com API ID {} atualizado: {}", apiId, teamToUpdate.getName());
                                } else {
                                    Team team = new Team();
                                    team.setApiId(apiId);
                                    team.setName(teamDetails.path("name").asText());
                                    team.setCode(teamDetails.path("code").asText());
                                    team.setCountry(teamDetails.path("country").asText());
                                    team.setFounded(teamDetails.path("founded").asInt());
                                    team.setNational(teamDetails.path("national").asBoolean());
                                    team.setLogoUrl(teamDetails.path("logo").asText());
                                    teamRepository.save(team);
                                    log.info("Time salvo: {}", team.getName());
                                }
                            }
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        log.error("Erro ao processar JSON de times para liga {}: {}", leagueApiId, e.getMessage(), e);
                        return Mono.error(new RuntimeException("Failed to ingest teams", e));
                    }
                })
                .then();
    }

    // --- Ingestão de Partidas (Fixtures) ---
    @Transactional
    public Mono<Void> ingestFixturesForLeagueAndSeason(Integer leagueApiId, Integer seasonYear) {
        log.info("Processando ingestão de partidas para a liga API ID {} na temporada {}", leagueApiId, seasonYear);

        return footballApiClient.getFixturesByLeagueAndSeason(leagueApiId, seasonYear)
                .flatMap(jsonString -> {
                    try {
                        JsonNode rootNode = objectMapper.readTree(jsonString);
                        JsonNode responseNode = rootNode.path("response");

                        if (responseNode.isArray()) {
                            for (JsonNode fixtureJsonWrapper : responseNode) {
                                JsonNode fixtureDetails = fixtureJsonWrapper.path("fixture");
                                JsonNode leagueDetails = fixtureJsonWrapper.path("league");
                                JsonNode teamsDetails = fixtureJsonWrapper.path("teams");
                                JsonNode scoreDetails = fixtureJsonWrapper.path("score");

                                Integer apiId = fixtureDetails.path("id").asInt();

                                Optional<Fixture> existingFixture = fixtureRepository.findByApiId(apiId);
                                if (existingFixture.isPresent()) {
                                    // Se a partida já existe, atualizamos as informações de placar e status
                                    Fixture fixtureToUpdate = existingFixture.get();
                                    fixtureToUpdate.setDate(Instant.ofEpochSecond(fixtureDetails.path("timestamp").asLong()));
                                    fixtureToUpdate.setTimezone(fixtureDetails.path("timezone").asText());
                                    fixtureToUpdate.setTimestamp(fixtureDetails.path("timestamp").asLong());
                                    fixtureToUpdate.setStatus(fixtureDetails.path("status").path("long").asText());
                                    fixtureToUpdate.setElapsed(fixtureDetails.path("status").path("elapsed").asInt());
                                    fixtureToUpdate.setHomeGoals(scoreDetails.path("fulltime").path("home").asInt());
                                    fixtureToUpdate.setAwayGoals(scoreDetails.path("fulltime").path("away").asInt());
                                    fixtureToUpdate.setHomeHalfTimeGoals(scoreDetails.path("halftime").path("home").asInt());
                                    fixtureToUpdate.setAwayHalfTimeGoals(scoreDetails.path("halftime").path("away").asInt());
                                    fixtureToUpdate.setHomeExtraTimeGoals(scoreDetails.path("extratime").path("home").asInt());
                                    fixtureToUpdate.setAwayExtraTimeGoals(scoreDetails.path("extratime").path("away").asInt());
                                    fixtureToUpdate.setHomePenaltyGoals(scoreDetails.path("penalty").path("home").asInt());
                                    fixtureToUpdate.setAwayPenaltyGoals(scoreDetails.path("penalty").path("away").asInt());
                                    fixtureToUpdate.setVenueApiId(fixtureJsonWrapper.path("venue").path("id").asInt());
                                    fixtureToUpdate.setVenueName(fixtureJsonWrapper.path("venue").path("name").asText());
                                    fixtureToUpdate.setVenueCity(fixtureJsonWrapper.path("venue").path("city").asText());
                                    fixtureToUpdate.setReferee(fixtureDetails.path("referee").asText());

                                    fixtureRepository.save(fixtureToUpdate);
                                    log.debug("Partida com API ID {} atualizada: {} vs {}", apiId, fixtureToUpdate.getHomeTeam().getName(), fixtureToUpdate.getAwayTeam().getName());

                                } else {
                                    // Se a partida não existe, cria uma nova
                                    Fixture fixture = new Fixture();
                                    fixture.setApiId(apiId);
                                    fixture.setDate(Instant.ofEpochSecond(fixtureDetails.path("timestamp").asLong()));
                                    fixture.setTimezone(fixtureDetails.path("timezone").asText());
                                    fixture.setTimestamp(fixtureDetails.path("timestamp").asLong());
                                    fixture.setStatus(fixtureDetails.path("status").path("long").asText());
                                    fixture.setElapsed(fixtureDetails.path("status").path("elapsed").asInt());

                                    Integer leagueApiIdFromFixture = leagueDetails.path("id").asInt();
                                    League league = leagueRepository.findByApiId(leagueApiIdFromFixture)
                                            .orElseGet(() -> {
                                                League newLeague = new League();
                                                newLeague.setApiId(leagueApiIdFromFixture);
                                                newLeague.setName(leagueDetails.path("name").asText());
                                                newLeague.setCountry(leagueDetails.path("country").asText());
                                                newLeague.setLogoUrl(leagueDetails.path("logo").asText());
                                                newLeague.setType(leagueDetails.path("type").asText());
                                                log.warn("Liga {} (API ID {}) não encontrada, criando a partir dos dados da partida.", newLeague.getName(), newLeague.getApiId());
                                                return leagueRepository.save(newLeague);
                                            });
                                    fixture.setLeague(league);
                                    fixture.setSeason(leagueDetails.path("season").asInt());

                                    Integer homeTeamApiId = teamsDetails.path("home").path("id").asInt();
                                    Team homeTeam = teamRepository.findByApiId(homeTeamApiId)
                                            .orElseGet(() -> {
                                                Team newTeam = new Team();
                                                newTeam.setApiId(homeTeamApiId);
                                                newTeam.setName(teamsDetails.path("home").path("name").asText());
                                                newTeam.setLogoUrl(teamsDetails.path("home").path("logo").asText());
                                                log.warn("Time {} (API ID {}) não encontrado, criando a partir dos dados da partida.", newTeam.getName(), newTeam.getApiId());
                                                return teamRepository.save(newTeam);
                                            });
                                    fixture.setHomeTeam(homeTeam);

                                    Integer awayTeamApiId = teamsDetails.path("away").path("id").asInt();
                                    Team awayTeam = teamRepository.findByApiId(awayTeamApiId)
                                            .orElseGet(() -> {
                                                Team newTeam = new Team();
                                                newTeam.setApiId(awayTeamApiId);
                                                newTeam.setName(teamsDetails.path("away").path("name").asText());
                                                newTeam.setLogoUrl(teamsDetails.path("away").path("logo").asText());
                                                log.warn("Time {} (API ID {}) não encontrado, criando a partir dos dados da partida.", newTeam.getName(), newTeam.getApiId());
                                                return teamRepository.save(newTeam);
                                            });
                                    fixture.setAwayTeam(awayTeam);

                                    fixture.setHomeGoals(scoreDetails.path("fulltime").path("home").asInt());
                                    fixture.setAwayGoals(scoreDetails.path("fulltime").path("away").asInt());
                                    fixture.setHomeHalfTimeGoals(scoreDetails.path("halftime").path("home").asInt());
                                    fixture.setAwayHalfTimeGoals(scoreDetails.path("halftime").path("away").asInt());
                                    fixture.setHomeExtraTimeGoals(scoreDetails.path("extratime").path("home").asInt());
                                    fixture.setAwayExtraTimeGoals(scoreDetails.path("extratime").path("away").asInt());
                                    fixture.setHomePenaltyGoals(scoreDetails.path("penalty").path("home").asInt());
                                    fixture.setAwayPenaltyGoals(scoreDetails.path("penalty").path("away").asInt());

                                    fixture.setVenueApiId(fixtureJsonWrapper.path("venue").path("id").asInt());
                                    fixture.setVenueName(fixtureJsonWrapper.path("venue").path("name").asText());
                                    fixture.setVenueCity(fixtureJsonWrapper.path("venue").path("city").asText());
                                    fixture.setReferee(fixtureDetails.path("referee").asText());

                                    fixtureRepository.save(fixture);
                                    log.info("Partida salva: {} vs {}", fixture.getHomeTeam().getName(), fixture.getAwayTeam().getName());
                                }
                            }
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        log.error("Erro ao processar JSON de partidas para liga {} na temporada {}: {}", leagueApiId, seasonYear, e.getMessage(), e);
                        return Mono.error(new RuntimeException("Failed to ingest fixtures", e));
                    }
                })
                .then();
    }

}