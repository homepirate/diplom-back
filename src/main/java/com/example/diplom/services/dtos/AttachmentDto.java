package com.example.diplom.services.dtos;

import java.util.UUID;

public class AttachmentDto {
    private UUID Id;
    private UUID visitId;
    private String filePath;
    private String description;

    public AttachmentDto(UUID id, UUID visitId, String filePath, String description) {
        Id = id;
        this.visitId = visitId;
        this.filePath = filePath;
        this.description = description;
    }

    public AttachmentDto() {
    }

    public UUID getId() {
        return Id;
    }

    public void setId(UUID id) {
        Id = id;
    }

    public UUID getVisitId() {
        return visitId;
    }

    public void setVisitId(UUID visitId) {
        this.visitId = visitId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
