package org.covid19.vaccinetracker.userrequests.model;

import org.covid19.vaccinetracker.persistence.mariadb.entity.CoreEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Entity
@Table(name = "pincodes")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pincode extends CoreEntity {
    @Column
    private String pincode;

    @ManyToOne(optional = false)
    private District district;
}
