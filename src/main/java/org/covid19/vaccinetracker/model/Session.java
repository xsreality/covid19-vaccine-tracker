package org.covid19.vaccinetracker.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.covid19.vaccinetracker.userrequests.model.Vaccine;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.covid19.vaccinetracker.userrequests.model.Vaccine.COVAXIN;
import static org.covid19.vaccinetracker.userrequests.model.Vaccine.COVISHIELD;
import static org.covid19.vaccinetracker.userrequests.model.Vaccine.SPUTNIK_V;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Session {

    @JsonProperty("session_id")
    public String sessionId;
    @JsonProperty("date")
    public String date;
    @JsonProperty("available_capacity")
    public Integer availableCapacity;
    @JsonProperty("available_capacity_dose1")
    public Integer availableCapacityDose1;
    @JsonProperty("available_capacity_dose2")
    public Integer availableCapacityDose2;
    @JsonProperty("min_age_limit")
    public Integer minAgeLimit;
    @JsonProperty("vaccine")
    public String vaccine;
    @JsonProperty("slots")
    public List<String> slots = null;

    public boolean ageLimit18AndAbove() {
        return minAgeLimit >= 18;
    }

    public boolean ageLimitExactly18() {
        return minAgeLimit == 18;
    }

    public boolean ageLimitBetween18And45() {
        return minAgeLimit >= 18 && minAgeLimit < 45;
    }

    public boolean ageLimitExactly45() {
        return minAgeLimit == 45;
    }

    public boolean hasCapacity() {
        return (availableCapacityDose1 >= 10 || availableCapacityDose2 >= 10)
                && (availableCapacity == (availableCapacityDose1 + availableCapacityDose2));
    }

    public boolean hasDose1Capacity() {
        return availableCapacityDose1 >= 10;
    }

    public boolean hasDose2Capacity() {
        return availableCapacityDose2 >= 10;
    }

    public boolean hasCovishield() {
        return COVISHIELD.toString().equalsIgnoreCase(vaccine);
    }

    public boolean hasCovaxin() {
        return COVAXIN.toString().equalsIgnoreCase(vaccine);
    }

    public boolean hasSputnikV() {
        return SPUTNIK_V.toString().equalsIgnoreCase(vaccine);
    }
}