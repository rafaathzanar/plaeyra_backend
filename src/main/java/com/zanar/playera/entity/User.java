package com.zanar.playera.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phone;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String profileImage;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    private boolean emailVerified = false;

    private String emailVerificationToken;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, DELETED
    }

    public enum UserRole {
        CUSTOMER, VENUE_OWNER, ADMIN
    }

    // Helper methods
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isCustomer() {
        return role == UserRole.CUSTOMER;
    }

    public boolean isVenueOwner() {
        return role == UserRole.VENUE_OWNER;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
