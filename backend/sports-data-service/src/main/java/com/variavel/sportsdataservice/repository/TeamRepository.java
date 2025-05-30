package com.variavel.sportsdataservice.repository;

import com.variavel.sportsdataservice.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByApiId(Integer apiId);
    Optional<Team> findByName(String name);
}