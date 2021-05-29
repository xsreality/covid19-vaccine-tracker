package org.covid19.vaccinetracker.persistence.mariadb.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationId implements Serializable {
    @Column(name = "user_id")
    private String userId;

    @Column
    private String pincode;
}
