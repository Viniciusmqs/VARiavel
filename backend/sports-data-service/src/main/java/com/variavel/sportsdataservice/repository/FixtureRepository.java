package com.variavel.sportsdataservice.repository;

import com.variavel.sportsdataservice.domain.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture, Long> {
    Optional<Fixture> findByApiId(Integer apiId);
    List<Fixture> findByLeagueIdAndSeason(Long leagueId, Integer season);
    List<Fixture> findByStatus(String status);
    // Podemos adicionar mais métodos de busca conforme a necessidade
}