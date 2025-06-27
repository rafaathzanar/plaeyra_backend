package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "venues")
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long venueId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String location;
    private String description;
    private String contactNo;

    @ElementCollection
    private List<String> images = new ArrayList<>();

    @ElementCollection
    private List<String> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Court> courts = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private VenueOwner venueOwner;

    // Getters, Setters, Constructors
}
