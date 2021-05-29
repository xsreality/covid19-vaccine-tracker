package org.covid19.vaccinetracker.notifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.digest.DigestUtils;
import org.covid19.vaccinetracker.model.Center;
import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotification;
import org.covid19.vaccinetracker.persistence.mariadb.repository.UserNotificationRepository;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NotificationCache {
    private final UserNotificationRepository repository;
    private final ObjectMapper objectMapper;

    public NotificationCache(UserNotificationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public boolean isNewNotification(String user, List<Center> centers) {
        final Optional<UserNotification> fromCache = this.repository.findById(user);

        if (fromCache.isEmpty()) {
            return true;
        }

        byte[] bytes = serialize(centers);
        if (bytes == null) {
            return true;
        }

        return !DigestUtils.sha256Hex(bytes).equals(fromCache.get().getNotificationHash());
    }

    public void updateUser(String user, List<Center> centers) {
        byte[] bytes = serialize(centers);
        String notificationHash = bytes == null ? "unknown" : DigestUtils.sha256Hex(bytes);
        this.repository.save(
                UserNotification.builder()
                        .userId(user)
                        .notificationHash(notificationHash)
                        .notifiedAt(LocalDateTime.now())
                        .build());
    }

    @Nullable
    private byte[] serialize(List<Center> centers) {
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(centers);
        } catch (JsonProcessingException e) {
            log.error("Error serializing centers {}", centers);
            return null;
        }
        return bytes;
    }
}
