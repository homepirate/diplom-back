package com.example.diplom.models;
import jakarta.persistence.*;

@Entity
@Table(name = "attachments")
public class Attachment extends Base{

    private Visit visit;
    private String filePath;
    private String description;

    public Attachment() {
    }

    @OneToOne(optional = false)
    @JoinColumn(name = "visit_id", unique = true, nullable = false)
    public Visit getVisit() {
        return visit;
    }

    public void setVisit(Visit visit) {
        this.visit = visit;
    }

    @Column(name = "file_path", nullable = false)
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Column(name="description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public String toString() {
        return "Attachment{" +
                "id=" + id +
                ", filePath='" + filePath + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}