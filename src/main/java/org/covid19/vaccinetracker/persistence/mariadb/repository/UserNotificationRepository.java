package org.covid19.vaccinetracker.persistence.mariadb.repository;

import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotification;
import org.covid19.vaccinetracker.persistence.mariadb.entity.UserNotificationId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationRepository extends CrudRepository<UserNotification, UserNotificationId> {
}
