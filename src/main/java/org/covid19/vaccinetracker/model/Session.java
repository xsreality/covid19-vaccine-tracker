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
public class Session {

    @JsonProperty("session_id")
    public String sessionId;
    @JsonProperty("date")
    public String date;
    @JsonProperty("available_capacity")
    public Integer availableCapacity;
    @JsonProperty("min_age_limit")
    public Integer minAgeLimit;
    @JsonProperty("vaccine")
    public String vaccine;
    @JsonProperty("slots")
    public List<String> slots = null;

}