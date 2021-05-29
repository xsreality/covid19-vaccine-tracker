package org.covid19.vaccinetracker.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.digest.DigestUtils;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.model.Session;
import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotification;
import org.covid19.vaccinetracker.persistence.mariadb.repository.UserNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class NotificationCacheTest {
    @Autowired
    private UserNotificationRepository repository;

    private NotificationCache cache;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        this.cache = new NotificationCache(repository, objectMapper);
    }

    @Test
    public void testNewNotification() throws Exception {
        List<Center> old = List.of(Center.builder().centerId(123).name("abc")
                .sessions(List.of(Session.builder()
                        .availableCapacity(10)
                        .availableCapacityDose1(10)
                        .build()))
                .build());
        this.repository.save(UserNotification.builder()
                .userId("userA")
                .pincode("110022")
                .notificationHash(DigestUtils.sha256Hex(objectMapper.writeValueAsBytes(old)))
                .build());

        List<Center> updated = List.of(Center.builder().centerId(123).pincode(110022).name("abc")
                .sessions(List.of(Session.builder()
                        .availableCapacity(5)   // capacity changed
                        .availableCapacityDose1(5)
                        .build()))
                .build());

        assertTrue(cache.isNewNotification("userA", "110022", updated));
    }

    @Test
    public void testOldNotification() throws Exception {
        List<Center> old = List.of(Center.builder().centerId(123).pincode(110022).name("abc")
                .sessions(List.of(Session.builder()
                        .availableCapacity(10)
                        .availableCapacityDose1(10)
                        .build()))
                .build());
        this.repository.save(UserNotification.builder()
                .userId("userA")
                .pincode("110022")
                .notificationHash(DigestUtils.sha256Hex(objectMapper.writeValueAsBytes(old)))
                .build());

        assertFalse(cache.isNewNotification("userA", "110022", old));
    }

    @Test
    public void testNewNotificationWhenFirstTime() {
        assertTrue(cache.isNewNotification("userA", "110022", List.of()));
    }
}
