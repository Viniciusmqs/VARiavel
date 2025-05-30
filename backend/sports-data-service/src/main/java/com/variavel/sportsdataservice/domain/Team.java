package com.variavel.sportsdataservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID interno do nosso sistema (PK)

    @Column(unique = true, nullable = false)
    private Integer apiId; // ID do time na API-Football (ex: 33 para Manchester United)

    @Column(nullable = false)
    private String name; // Nome do time (ex: "Manchester United")

    private String code; // Código do time (ex: "MUN" ou "FCB") - pode ser nulo na API
    private String country; // País do time (ex: "England")
    private Integer founded; // Ano de fundação
    private Boolean national; // Se é uma seleção nacional (true/false)
    private String logoUrl; // URL do logo do time

    // Relacionamento com as ligas que o time participa pode ser modelado depois
    // Ou através de uma tabela de junção se for muitos para muitos
}