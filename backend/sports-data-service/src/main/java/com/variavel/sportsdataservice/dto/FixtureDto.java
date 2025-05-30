package com.variavel.sportsdataservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class FixtureDto {
    private Long id;
    private Integer apiId;

    private Instant date;
    private String timezone;
    private Long timestamp;
    private String status;
    private Integer elapsed;

    private String leagueName;
    private String leagueLogoUrl;

    private String homeTeamName;
    private String homeTeamLogoUrl;
    private String awayTeamName;
    private String awayTeamLogoUrl;

    private Integer homeGoals;
    private Integer awayGoals;

    // Adicione estes campos que estavam faltando
    private Integer homeHalfTimeGoals;
    private Integer awayHalfTimeGoals;
    private Integer homeExtraTimeGoals;
    private Integer awayExtraTimeGoals;
    private Integer homePenaltyGoals;
    private Integer awayPenaltyGoals;

    private String venueName;
    private String venueCity;
    private String referee;
}