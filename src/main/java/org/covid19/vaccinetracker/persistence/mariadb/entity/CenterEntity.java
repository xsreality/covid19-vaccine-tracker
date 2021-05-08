package org.covid19.vaccinetracker.persistence.mariadb.entity;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@Data
@Builder
@Entity
@Table(name = "vaccine_centers")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CenterEntity {
    @Id
    private long id;

    @Column
    private String name;

    @Column
    private String address;

    @Column
    private String pincode;

    @Column(name = "fee_type")
    private String feeType;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "state_name")
    private String stateName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<SessionEntity> sessions;
}
