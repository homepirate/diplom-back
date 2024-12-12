package com.example.diplom.models;

import com.example.diplom.models.PK.VisitServicePK;
import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "visit_service")
@IdClass(VisitServicePK.class)
public class VisitService {

    private UUID visitId;
    private UUID serviceId;
    private Visit visit;
    private Service service;

    public VisitService() {
    }

    public VisitService(Visit visit, Service service) {
        this.visit = visit;
        this.service = service;
        this.visitId = visit.getId();
        this.serviceId = service.getId();
    }


    @Id
    @Column(name = "visit_id", nullable = false)
    public UUID getVisitId() {
        return visitId;
    }

    public void setVisitId(UUID visitId) {
        this.visitId = visitId;
    }

    @Id
    @Column(name = "service_id", nullable = false)
    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", insertable = false, updatable = false)
    public Visit getVisit() {
        return visit;
    }

    public void setVisit(Visit visit) {
        this.visit = visit;
        if (visit != null) {
            this.visitId = visit.getId();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", insertable = false, updatable = false)
    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
        if (service != null) {
            this.serviceId = service.getId();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VisitService)) return false;
        VisitService that = (VisitService) o;
        return Objects.equals(visitId, that.visitId) &&
                Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visitId, serviceId);
    }

    @Override
    public String toString() {
        return "VisitService{" +
                "visitId=" + visitId +
                ", serviceId=" + serviceId +
                '}';
    }
}
