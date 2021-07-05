package org.covid19.vaccinetracker.persistence.mariadb.repository;

import org.covid19.vaccinetracker.persistence.mariadb.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, String> {
    @Query("SELECT s FROM CenterEntity c " +
            "JOIN c.sessions s " +
            "WHERE c.id = :centerId " +
            "AND s.date = :date " +
            "AND s.minAgeLimit = :age " +
            "AND s.vaccine = :vaccine ")
    List<SessionEntity> findLatestSession(Long centerId, String date, Integer age, String vaccine);
}
