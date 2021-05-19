package org.covid19.vaccinetracker.persistence.mariadb.repository;

import org.covid19.vaccinetracker.persistence.mariadb.entity.SessionEntity;
import org.springframework.stereotype.Component;

import javax.persistence.PreUpdate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SessionEntityListener {
    @PreUpdate
    public void onUpdate(final SessionEntity entity) {
        log.info("Session entity listener onUpdate called {}", entity);
    }
}
