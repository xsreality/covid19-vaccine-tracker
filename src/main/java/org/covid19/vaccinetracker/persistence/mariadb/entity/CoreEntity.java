package org.covid19.vaccinetracker.persistence.mariadb.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class CoreEntity {
    @Id
    @Column(nullable = false, unique = true, insertable = false, updatable = false)
    private String id = UUID.randomUUID().toString();
}
