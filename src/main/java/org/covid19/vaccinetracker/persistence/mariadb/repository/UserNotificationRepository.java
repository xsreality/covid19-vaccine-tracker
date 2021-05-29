package org.covid19.vaccinetracker.persistence.mariadb.repository;

import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotification;
import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotificationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UserNotificationId> {
}
