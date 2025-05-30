package com.variavel.sportsdataservice.dto;

import lombok.Builder; // Importar Lombok Builder
import lombok.Data;

@Data
@Builder // Gera um builder pattern para facilitar a criação de objetos
public class LeagueDto {
    private Long id; // Nosso ID interno
    private Integer apiId; // ID da liga na API-Football
    private String name; // Nome da liga
    private String type; // Tipo (league, cup)
    private String country; // País
    private String logoUrl; // URL do logo
    // Podemos adicionar o currentSeason aqui no futuro
}