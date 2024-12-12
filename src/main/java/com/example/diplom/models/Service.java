package com.example.diplom.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "services")
public class Service extends Base{


    private String name;

    private BigDecimal price;

    private Set<VisitService> visitServices = new HashSet<>();

    public Service() {
    }

    @Column(name="name", nullable=false, length=100)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name="price", nullable=false, precision=10, scale=2)
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<VisitService> getVisitServices() {
        return visitServices;
    }

    public void setVisitServices(Set<VisitService> visitServices) {
        this.visitServices = visitServices;
    }


    @Override
    public String toString() {
        return "Service{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}
