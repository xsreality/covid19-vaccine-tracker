package org.covid19.vaccinetracker.persistence.mariadb.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@Builder
@Data
@Entity
@Table(name = "sessions")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionEntity {
    @Id
    private String id;

    @Column
    private String date;

    @Column(name = "available_capacity")
    private int availableCapacity;

    @Column(name = "min_age_limit")
    private int minAgeLimit;

    private String vaccine;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
