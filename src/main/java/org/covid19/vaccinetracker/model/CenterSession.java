package org.covid19.vaccinetracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CenterSession {

    private String centerName;
    private String districtName;
    private String pincode;
    private String sessionDate;
    private int minAge;
    private String sessionVaccine;
    private String multipleDates; // for accumulating multiple sessions

    public CenterSession(String centerName, String districtName, String pincode, String sessionDate,
                         int minAge, String sessionVaccine) {
        this.centerName = centerName;
        this.districtName = districtName;
        this.pincode = pincode;
        this.sessionDate = sessionDate;
        this.minAge = minAge;
        this.sessionVaccine = sessionVaccine;
    }
}
