package com.variavel.sportsdataservice.domain;

import jakarta.persistence.*;
import lombok.Data; // Importa Lombok para getters/setters/construtores
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity // Indica que esta classe é uma entidade JPA e será mapeada para uma tabela no DB
@Table(name = "leagues") // Nome da tabela no banco de dados
@Data // Lombok: Gera getters, setters, toString, equals e hashCode
@NoArgsConstructor // Lombok: Gera um construtor sem argumentos (necessário para JPA)
@AllArgsConstructor // Lombok: Gera um construtor com todos os argumentos
public class League {

    @Id // Indica que este campo é a chave primária da tabela
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Estratégia de geração de ID (auto-incremento para MySQL)
    private Long id; // ID interno do nosso sistema (PK)

    @Column(unique = true, nullable = false) // Garante que o ID da API seja único e não nulo
    private Integer apiId; // ID da liga na API-Football (ex: 39 para Premier League)

    @Column(nullable = false)
    private String name; // Nome da liga (ex: "Premier League")

    private String type; // Tipo da liga (ex: "league", "cup")

    private String country; // País da liga (ex: "England")

    private String logoUrl; // URL do logo da liga

    // Se a API retornar a temporada atual, podemos adicionar um campo para isso
    // private Integer currentSeason;
}