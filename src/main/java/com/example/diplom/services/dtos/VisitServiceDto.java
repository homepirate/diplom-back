package com.example.diplom.services.dtos;

import com.example.diplom.models.Service;
import com.example.diplom.models.Visit;

import java.util.UUID;

public class VisitServiceDto {

    private UUID visitId;
    private UUID serviceId;
    private int quantity;

    public VisitServiceDto(UUID visitId, UUID serviceId, int quantity) {
        this.visitId = visitId;
        this.serviceId = serviceId;
        this.quantity = quantity;
    }

    public VisitServiceDto() {
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
