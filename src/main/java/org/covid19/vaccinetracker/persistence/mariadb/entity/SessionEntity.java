package org.covid19.vaccinetracker.persistence.mariadb.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Data
@Entity
@EntityListeners(EntityListeners.class)
@Table(name = "sessions")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionEntity {
    @EqualsAndHashCode.Include
    @Id
    private String id;

    @Column
    private String date;

    @Column(name = "available_capacity")
    private int availableCapacity;

    @Column(name = "available_capacity_dose1")
    private int availableCapacityDose1;

    @Column(name = "available_capacity_dose2")
    private int availableCapacityDose2;

    @Column(name = "min_age_limit")
    private int minAgeLimit;

    private String vaccine;

    private String cost;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
