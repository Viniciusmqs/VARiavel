package com.variavel.sportsdataservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant; // Para timestamps em UTC

@Entity
@Table(name = "fixtures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fixture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID interno do nosso sistema (PK)

    @Column(unique = true, nullable = false)
    private Integer apiId; // ID da partida na API-Football (ex: 1035544)

    // Informações da Partida
    private Instant date; // Data e hora da partida (Instant é bom para UTC)
    private String timezone; // Fuso horário da partida (ex: "America/Sao_Paulo")
    private Long timestamp; // Timestamp Unix da partida
    private String status; // Status da partida (ex: "Not Started", "Live", "Match Finished")
    private Integer elapsed; // Minutos decorridos da partida

    // Relacionamento com Liga
    @ManyToOne // Uma partida pertence a uma liga
    @JoinColumn(name = "league_id", nullable = false) // Coluna de FK (foreign key)
    private League league;

    // Relacionamento com Temporada (se for relevante, ou já está na Liga)
    private Integer season; // Ano da temporada (ex: 2024)

    // Times (Home e Away)
    @ManyToOne // Uma partida tem um time da casa
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne // Uma partida tem um time visitante
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    // Placar (podemos criar uma classe separada para isso ou embutir)
    // Para simplificar, vamos embutir por enquanto
    private Integer homeGoals;
    private Integer awayGoals;

    // Placar de intervalo (Half time)
    private Integer homeHalfTimeGoals;
    private Integer awayHalfTimeGoals;

    // Placar de tempo extra (Extra time)
    private Integer homeExtraTimeGoals;
    private Integer awayExtraTimeGoals;

    // Placar de pênaltis (Penalty shootout)
    private Integer homePenaltyGoals;
    private Integer awayPenaltyGoals;

    // Informações do Estádio (opcional, pode ser uma entidade separada se houver muitas informações)
    private Integer venueApiId; // ID do estádio na API
    private String venueName; // Nome do estádio
    private String venueCity; // Cidade do estádio

    // Informações do Árbitro (opcional)
    private String referee; // Nome do árbitro
}