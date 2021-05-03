package com.abhinav.covid19.vaccinetracker.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class VaccineCenters {

    @JsonProperty("centers")
    public List<Center> centers = null;

}