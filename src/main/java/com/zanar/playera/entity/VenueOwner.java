package com.zanar.playera.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("VENUE_OWNER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueOwner extends User {

    @OneToMany(mappedBy = "venueOwner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Venue> venues = new ArrayList<>();

    // Getters, Setters, Constructors
}
