package com.example.diplom.models.PK;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DoctorPatientPK implements Serializable {

    private UUID doctorId;
    private UUID patientId;

    public DoctorPatientPK() {
    }

    public DoctorPatientPK(UUID doctorId, UUID patientId) {
        this.doctorId = doctorId;
        this.patientId = patientId;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoctorPatientPK)) return false;
        DoctorPatientPK that = (DoctorPatientPK) o;
        return Objects.equals(doctorId, that.doctorId) &&
                Objects.equals(patientId, that.patientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(doctorId, patientId);
    }
}
