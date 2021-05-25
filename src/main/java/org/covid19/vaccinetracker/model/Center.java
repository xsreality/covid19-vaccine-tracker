package org.covid19.vaccinetracker.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Center {

    @JsonProperty("center_id")
    public Integer centerId;
    @JsonProperty("name")
    public String name;
    @JsonProperty("state_name")
    public String stateName;
    @JsonProperty("district_name")
    public String districtName;
    @JsonProperty("block_name")
    public String blockName;
    @JsonProperty("pincode")
    public Integer pincode;
    @JsonProperty("lat")
    public Integer latitude;
    @JsonProperty("long")
    public Integer longitude;
    @JsonProperty("from")
    public String from;
    @JsonProperty("to")
    public String to;
    @JsonProperty("fee_type")
    public String feeType;
    @JsonProperty("sessions")
    public List<Session> sessions = null;

    public boolean areVaccineCentersAvailableFor18plus() {
        return this.getSessions()
                .stream()
                .anyMatch(session -> session.ageLimit18AndAbove() && session.hasCapacity());
    }
}