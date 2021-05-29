package org.covid19.vaccinetracker.userrequests.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@Data
@Entity
@Table(name = "states")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class State {
    @Id
    private int id;

    @Column(name = "state_name")
    private String stateName;
}
