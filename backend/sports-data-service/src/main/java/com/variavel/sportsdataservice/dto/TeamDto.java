package com.variavel.sportsdataservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamDto {
    private Long id; // Nosso ID interno
    private Integer apiId; // ID do time na API-Football
    private String name; // Nome do time
    private String code; // Código (ex: MUN)
    private String country; // País
    private String logoUrl; // URL do logo
}