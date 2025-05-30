package com.variavel.sportsdataservice.repository;

import com.variavel.sportsdataservice.domain.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Indica que é um componente de repositório Spring
public interface LeagueRepository extends JpaRepository<League, Long> {
    // JpaRepository<Tipo da Entidade, Tipo da Chave Primária>
    // Automaticamente fornece métodos como save(), findById(), findAll(), delete()

    // Podemos adicionar métodos personalizados de busca aqui
    Optional<League> findByApiId(Integer apiId); // Busca uma liga pelo ID da API
    Optional<League> findByName(String name); // Busca uma liga pelo nome
}