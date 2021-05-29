package org.covid19.vaccinetracker.persistence.mariadb.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@Table(name = "user_notifications")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification {
    @EmbeddedId
    private UserNotificationId userNotificationId;

    @Column(name = "notification_hash")
    private String notificationHash;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
}
