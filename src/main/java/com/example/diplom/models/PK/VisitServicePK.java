package com.example.diplom.models.PK;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class VisitServicePK implements Serializable {

    private UUID visitId;
    private UUID serviceId;

    public VisitServicePK() {
    }

    public UUID getVisitId() {
        return visitId;
    }

    public void setVisitId(UUID visitId) {
        this.visitId = visitId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public VisitServicePK(UUID visitId, UUID serviceId) {
        this.visitId = visitId;
        this.serviceId = serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VisitServicePK)) return false;
        VisitServicePK that = (VisitServicePK) o;
        return Objects.equals(visitId, that.visitId) &&
                Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visitId, serviceId);
    }
}
