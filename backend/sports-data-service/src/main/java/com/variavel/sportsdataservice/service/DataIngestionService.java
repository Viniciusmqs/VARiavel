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
import jakarta.transaction.Transactional; // Importe jakarta.transaction.Transactional
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ObjectMapper objectMapper; // Para parsear JSON

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

    // --- Ingestão de Ligas ---
    @Transactional // Garante que a operação seja transacional (salvar no DB)
    public Mono<Void> ingestLeagues() {
        log.info("Iniciando ingestão de ligas da API-Football...");
        return footballApiClient.getLeagues()
                .flatMap(jsonString -> {
                    try {
                        JsonNode rootNode = objectMapper.readTree(jsonString);
                        JsonNode responseNode = rootNode.path("response");

                        if (responseNode.isArray()) {
                            for (JsonNode leagueJson : responseNode) {
                                // A API-Football retorna Ligas aninhadas (league, country, seasons)
                                // Precisamos extrair os dados da liga e do país.
                                JsonNode leagueDetails = leagueJson.path("league");
                                JsonNode countryDetails = leagueJson.path("country");

                                // Extrair ID da API para verificar duplicatas
                                Integer apiId = leagueDetails.path("id").asInt();

                                // Se a liga já existe, pular
                                Optional<League> existingLeague = leagueRepository.findByApiId(apiId);
                                if (existingLeague.isPresent()) {
                                    // log.debug("Liga com API ID {} já existe: {}", apiId, existingLeague.get().getName());
                                    continue; // Pula para a próxima iteração
                                }

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
                        return Mono.empty(); // Retorna um Mono vazio após processar
                    } catch (Exception e) {
                        log.error("Erro ao processar JSON de ligas: {}", e.getMessage(), e);
                        return Mono.error(new RuntimeException("Failed to ingest leagues", e));
                    }
                })
                .then(); // Converte para Mono<Void> para indicar que a operação está completa
    }


    // --- Ingestão de Times (exemplo simplificado) ---
    // A API-Football para times é mais complexa (por liga/país/nome).
    // Vamos criar um método de exemplo que você pode chamar com um ID de liga.
    @Transactional
    public Mono<Void> ingestTeamsForLeague(Integer leagueApiId, Integer seasonYear) {
        log.info("Iniciando ingestão de times para a liga API ID {} na temporada {}", leagueApiId, seasonYear);
        return footballApiClient.getTeamsByLeagueAndSeason(leagueApiId, seasonYear) // Precisamos criar este método no FootballApiClient
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
                                    // log.debug("Time com API ID {} já existe: {}", apiId, existingTeam.get().getName());
                                    continue;
                                }

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
        log.info("Iniciando ingestão de partidas para a liga API ID {} na temporada {}", leagueApiId, seasonYear);

        return footballApiClient.getFixturesByLeagueAndSeason(leagueApiId, seasonYear) // Precisamos criar este método no FootballApiClient
                .flatMap(jsonString -> {
                    try {
                        JsonNode rootNode = objectMapper.readTree(jsonString);
                        JsonNode responseNode = rootNode.path("response");

                        if (responseNode.isArray()) {
                            for (JsonNode fixtureJsonWrapper : responseNode) {
                                JsonNode fixtureDetails = fixtureJsonWrapper.path("fixture");
                                JsonNode leagueDetails = fixtureJsonWrapper.path("league"); // Detalhes da liga na resposta da fixture
                                JsonNode teamsDetails = fixtureJsonWrapper.path("teams");
                                JsonNode scoreDetails = fixtureJsonWrapper.path("score");

                                Integer apiId = fixtureDetails.path("id").asInt();

                                Optional<Fixture> existingFixture = fixtureRepository.findByApiId(apiId);
                                if (existingFixture.isPresent()) {
                                    // log.debug("Partida com API ID {} já existe: {}", apiId, existingFixture.get().getId());
                                    continue;
                                }

                                Fixture fixture = new Fixture();
                                fixture.setApiId(apiId);

                                // Mapear detalhes da partida
                                fixture.setDate(Instant.ofEpochSecond(fixtureDetails.path("timestamp").asLong()));
                                fixture.setTimezone(fixtureDetails.path("timezone").asText());
                                fixture.setTimestamp(fixtureDetails.path("timestamp").asLong());
                                fixture.setStatus(fixtureDetails.path("status").path("long").asText());
                                fixture.setElapsed(fixtureDetails.path("status").path("elapsed").asInt());

                                // Ligas (precisamos buscar/salvar se não existirem)
                                Integer leagueApiIdFromFixture = leagueDetails.path("id").asInt();
                                League league = leagueRepository.findByApiId(leagueApiIdFromFixture)
                                        .orElseGet(() -> {
                                            // Se a liga não existe no nosso DB, criamos uma (com base nos dados da fixture)
                                            // Idealmente, as ligas já teriam sido ingeridas pelo método ingestLeagues()
                                            League newLeague = new League();
                                            newLeague.setApiId(leagueApiIdFromFixture);
                                            newLeague.setName(leagueDetails.path("name").asText());
                                            newLeague.setCountry(leagueDetails.path("country").asText());
                                            newLeague.setLogoUrl(leagueDetails.path("logo").asText());
                                            newLeague.setType(leagueDetails.path("type").asText()); // Adicione type
                                            log.warn("Liga {} (API ID {}) não encontrada, criando a partir dos dados da partida.", newLeague.getName(), newLeague.getApiId());
                                            return leagueRepository.save(newLeague);
                                        });
                                fixture.setLeague(league);
                                fixture.setSeason(leagueDetails.path("season").asInt());


                                // Times (home e away)
                                // Home Team
                                Integer homeTeamApiId = teamsDetails.path("home").path("id").asInt();
                                Team homeTeam = teamRepository.findByApiId(homeTeamApiId)
                                        .orElseGet(() -> {
                                            // Se o time não existe, cria (idealmente, times seriam ingeridos separadamente)
                                            Team newTeam = new Team();
                                            newTeam.setApiId(homeTeamApiId);
                                            newTeam.setName(teamsDetails.path("home").path("name").asText());
                                            newTeam.setLogoUrl(teamsDetails.path("home").path("logo").asText());
                                            // Não temos country, founded, national aqui na resposta da fixture diretamente,
                                            // então eles podem ficar nulos ou buscar de outro lugar.
                                            log.warn("Time {} (API ID {}) não encontrado, criando a partir dos dados da partida.", newTeam.getName(), newTeam.getApiId());
                                            return teamRepository.save(newTeam);
                                        });
                                fixture.setHomeTeam(homeTeam);

                                // Away Team
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

                                // Placar
                                fixture.setHomeGoals(scoreDetails.path("fulltime").path("home").asInt());
                                fixture.setAwayGoals(scoreDetails.path("fulltime").path("away").asInt());
                                fixture.setHomeHalfTimeGoals(scoreDetails.path("halftime").path("home").asInt());
                                fixture.setAwayHalfTimeGoals(scoreDetails.path("halftime").path("away").asInt());
                                fixture.setHomeExtraTimeGoals(scoreDetails.path("extratime").path("home").asInt());
                                fixture.setAwayExtraTimeGoals(scoreDetails.path("extratime").path("away").asInt());
                                fixture.setHomePenaltyGoals(scoreDetails.path("penalty").path("home").asInt());
                                fixture.setAwayPenaltyGoals(scoreDetails.path("penalty").path("away").asInt());

                                // Informações do Estádio e Árbitro
                                fixture.setVenueApiId(fixtureJsonWrapper.path("venue").path("id").asInt());
                                fixture.setVenueName(fixtureJsonWrapper.path("venue").path("name").asText());
                                fixture.setVenueCity(fixtureJsonWrapper.path("venue").path("city").asText());
                                fixture.setReferee(fixtureDetails.path("referee").asText());

                                fixtureRepository.save(fixture);
                                log.info("Partida salva: {} vs {}", fixture.getHomeTeam().getName(), fixture.getAwayTeam().getName());
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