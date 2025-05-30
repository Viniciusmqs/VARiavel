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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    // Agendador para ligas (executa uma vez por dia, à 1 da manhã)
    // A sintaxe cron é "segundos minutos horas diaDoMes mes diaDaSemana"
    @Scheduled(cron = "0 0 1 * * *")
    public void scheduledIngestLeagues() {
        log.info("Iniciando ingestão agendada de ligas...");
        ingestLeagues().subscribe(
                null, // onNext
                error -> log.error("Erro na ingestão agendada de ligas: {}", error.getMessage(), error), // onError
                () -> log.info("Ingestão agendada de ligas concluída.") // onComplete
        );
    }

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
                                    League leagueToUpdate = existingLeague.get();
                                    leagueToUpdate.setName(leagueDetails.path("name").asText());
                                    leagueToUpdate.setType(leagueDetails.path("type").asText());
                                    leagueToUpdate.setCountry(countryDetails.path("name").asText());
                                    leagueToUpdate.setLogoUrl(leagueDetails.path("logo").asText());
                                    leagueRepository.save(leagueToUpdate);
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

    // Agendador para ingestão de partidas diárias (uma vez por dia, às 2 da manhã)
    // Busca partidas de hoje, ontem e amanhã para as ligas salvas.
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledIngestDailyFixtures() {
        log.info("Iniciando ingestão agendada de partidas diárias...");
        List<League> leagues = leagueRepository.findAll();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayStr = today.format(formatter);
        String yesterdayStr = yesterday.format(formatter);
        String tomorrowStr = tomorrow.format(formatter);

        for (League league : leagues) {
            // A API-Football exige 'season' junto com 'league' e 'date'
            // O campo 'season' na nossa entidade League atualmente é nulo ou não está sendo preenchido
            // Precisamos garantir que 'league.getSeason()' retorne um valor válido, ou buscar a temporada atual.
            // Para simplicidade inicial, vamos usar um ano fixo como 2024.
            // Em um projeto real, você buscaria a temporada atual da API para cada liga.
            Integer currentSeason = 2024; // <-- TODO: Implementar lógica para obter a temporada atual da liga

            ingestFixturesForDate(league.getApiId(), todayStr, currentSeason)
                    .subscribe(null, error -> log.error("Erro na ingestão diária de partidas (hoje) para liga {} (API ID {}): {}", league.getName(), league.getApiId(), error.getMessage()));

            ingestFixturesForDate(league.getApiId(), yesterdayStr, currentSeason)
                    .subscribe(null, error -> log.error("Erro na ingestão diária de partidas (ontem) para liga {} (API ID {}): {}", league.getName(), league.getApiId(), error.getMessage()));

            ingestFixturesForDate(league.getApiId(), tomorrowStr, currentSeason)
                    .subscribe(null, error -> log.error("Erro na ingestão diária de partidas (amanhã) para liga {} (API ID {}): {}", league.getName(), league.getApiId(), error.getMessage()));

            try {
                TimeUnit.SECONDS.sleep(1); // Pequeno delay para respeitar limites da API (1 req/segundo)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Agendador de partidas diárias interrompido.");
            }
        }
        log.info("Ingestão agendada de partidas diárias concluída.");
    }

    // Método para buscar partidas por data (chamado pelo agendador ou manualmente)
    @Transactional
    public Mono<Void> ingestFixturesForDate(Integer leagueApiId, String date, Integer seasonYear) {
        log.info("Processando ingestão de partidas para a liga API ID {} na data {} e temporada {}", leagueApiId, date, seasonYear);

        return footballApiClient.getFixturesByLeagueSeasonAndDate(leagueApiId, seasonYear, date)
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
                                    Fixture fixtureToUpdate = existingFixture.get();
                                    // Atualiza apenas os campos que podem mudar (status, placar, etc.)
                                    updateFixtureFields(fixtureToUpdate, fixtureDetails, leagueDetails, teamsDetails, scoreDetails, fixtureJsonWrapper);
                                    fixtureRepository.save(fixtureToUpdate);
                                    log.debug("Partida com API ID {} atualizada: {} vs {}", apiId, fixtureToUpdate.getHomeTeam().getName(), fixtureToUpdate.getAwayTeam().getName());

                                } else {
                                    // Cria nova partida
                                    Fixture fixture = new Fixture();
                                    fixture.setApiId(apiId);
                                    updateFixtureFields(fixture, fixtureDetails, leagueDetails, teamsDetails, scoreDetails, fixtureJsonWrapper); // Reutiliza lógica de preenchimento

                                    // Ligas (precisamos buscar/salvar se não existirem)
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

                                    // Times (home e away)
                                    // Home Team
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

                                    fixtureRepository.save(fixture);
                                    log.info("Partida salva: {} vs {}", fixture.getHomeTeam().getName(), fixture.getAwayTeam().getName());
                                }
                            }
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        log.error("Erro ao processar JSON de partidas para liga {} na data {}: {}", leagueApiId, date, e.getMessage(), e);
                        return Mono.error(new RuntimeException("Failed to ingest fixtures for date " + date, e));
                    }
                })
                .then();
    }


    // Agendador para ingestão de jogos ao vivo (mais frequente)
    // A cada 5 minutos, por exemplo.
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void scheduledIngestLiveFixtures() {
        log.info("Iniciando ingestão agendada de partidas ao vivo...");
        footballApiClient.getLiveMatches() // Este método já existe no FootballApiClient
                .flatMap(jsonString -> {
                    try {
                        JsonNode rootNode = objectMapper.readTree(jsonString);
                        JsonNode responseNode = rootNode.path("response");

                        if (responseNode.isArray()) {
                            for (JsonNode fixtureJsonWrapper : responseNode) {
                                JsonNode fixtureDetails = fixtureJsonWrapper.path("fixture");
                                JsonNode leagueDetails = fixtureJsonWrapper.path("league"); // Pode ser útil para criar ligas/times que ainda não existam
                                JsonNode teamsDetails = fixtureJsonWrapper.path("teams");
                                JsonNode scoreDetails = fixtureJsonWrapper.path("score");

                                Integer apiId = fixtureDetails.path("id").asInt();

                                Optional<Fixture> existingFixture = fixtureRepository.findByApiId(apiId);
                                if (existingFixture.isPresent()) {
                                    // ATUALIZAR PARTIDA EXISTENTE
                                    Fixture fixtureToUpdate = existingFixture.get();
                                    updateFixtureFields(fixtureToUpdate, fixtureDetails, leagueDetails, teamsDetails, scoreDetails, fixtureJsonWrapper);
                                    fixtureRepository.save(fixtureToUpdate);
                                    log.info("Partida ao vivo atualizada: {} vs {} - Status: {}",
                                            fixtureToUpdate.getHomeTeam().getName(),
                                            fixtureToUpdate.getAwayTeam().getName(),
                                            fixtureToUpdate.getStatus());
                                } else {
                                    // CRIAR NOVA PARTIDA AO VIVO (se não existia por algum motivo)
                                    log.warn("Partida ao vivo (API ID {}) não encontrada, criando-a. Isso pode indicar um erro de ingestão diária anterior.", apiId);

                                    Fixture fixture = new Fixture();
                                    fixture.setApiId(apiId);
                                    updateFixtureFields(fixture, fixtureDetails, leagueDetails, teamsDetails, scoreDetails, fixtureJsonWrapper);

                                    // Para ligas e times, reusar a lógica de busca/criação, como no ingestFixturesForDate
                                    Integer leagueApiIdFromFixture = leagueDetails.path("id").asInt();
                                    League league = leagueRepository.findByApiId(leagueApiIdFromFixture)
                                            .orElseGet(() -> {
                                                League newLeague = new League();
                                                newLeague.setApiId(leagueApiIdFromFixture);
                                                newLeague.setName(leagueDetails.path("name").asText());
                                                newLeague.setCountry(leagueDetails.path("country").asText());
                                                newLeague.setLogoUrl(leagueDetails.path("logo").asText());
                                                newLeague.setType(leagueDetails.path("type").asText());
                                                log.warn("Liga {} (API ID {}) não encontrada, criando a partir dos dados da partida ao vivo.", newLeague.getName(), newLeague.getApiId());
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
                                                log.warn("Time {} (API ID {}) não encontrado, criando a partir dos dados da partida ao vivo.", newTeam.getName(), newTeam.getApiId());
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
                                                log.warn("Time {} (API ID {}) não encontrado, criando a partir dos dados da partida ao vivo.", newTeam.getName(), newTeam.getApiId());
                                                return teamRepository.save(newTeam);
                                            });
                                    fixture.setAwayTeam(awayTeam);

                                    fixtureRepository.save(fixture);
                                    log.info("Nova partida ao vivo salva: {} vs {}", fixture.getHomeTeam().getName(), fixture.getAwayTeam().getName());
                                }
                            }
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        log.error("Erro na ingestão agendada de partidas ao vivo: {}", e.getMessage(), e);
                        return Mono.error(new RuntimeException("Failed to ingest live fixtures", e));
                    }
                })
                .then();
    }

    // NOVO MÉTODO AUXILIAR: Para evitar duplicação de código no preenchimento de campos de Fixture
    private void updateFixtureFields(Fixture fixture, JsonNode fixtureDetails, JsonNode leagueDetails,
                                     JsonNode teamsDetails, JsonNode scoreDetails, JsonNode fixtureJsonWrapper) {
        fixture.setDate(Instant.ofEpochSecond(fixtureDetails.path("timestamp").asLong()));
        fixture.setTimezone(fixtureDetails.path("timezone").asText());
        fixture.setTimestamp(fixtureDetails.path("timestamp").asLong());
        fixture.setStatus(fixtureDetails.path("status").path("long").asText());
        fixture.setElapsed(fixtureDetails.path("status").path("elapsed").asInt());
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
    }
}