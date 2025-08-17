package com.zanar.playera.repo;

import com.zanar.playera.entity.VenueOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VenueOwnerRepository extends JpaRepository<VenueOwner, Long> {
  Optional<VenueOwner> findByEmail(String email);

  Optional<VenueOwner> findByBusinessName(String businessName);

  Optional<VenueOwner> findByBusinessRegistrationNumber(String businessRegistrationNumber);
}
