package com.example.diplom.models;

import jakarta.persistence.*;

@Entity
@Table(name = "specializations")
public class Specialization extends Base {

    private String name;

    public Specialization() {}

    public Specialization(String name) {
        this.name = name;
    }

    @Column(name = "name", nullable = false, unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
