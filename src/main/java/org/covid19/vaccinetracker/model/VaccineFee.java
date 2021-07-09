package org.covid19.vaccinetracker.model;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public class VaccineFee {

    public String vaccine;
    public String fee;

    public boolean isVaccine(String vaccine) {
        return this.vaccine.equalsIgnoreCase(vaccine);
    }
}
