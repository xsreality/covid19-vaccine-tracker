package org.covid19.vaccinetracker.userrequests.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@Data
@Entity
@Table(name = "districts")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class District {
    @Id
    private int id;

    @Column(name = "district_name")
    private String districtName;

    @ManyToOne(optional = false)
    private State state;
}
