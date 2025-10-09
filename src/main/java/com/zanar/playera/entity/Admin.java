package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("ADMIN")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Admin extends User {

  private String department;

  private String employeeId;

  private LocalDateTime lastLoginAt;

  private String permissions; // JSON string of permissions

  private Boolean superAdmin = false;

  private String notes;

  // Helper methods
  public boolean isSuperAdmin() {
    return superAdmin != null && superAdmin;
  }

  public void updateLastLogin() {
    this.lastLoginAt = LocalDateTime.now();
  }

  public boolean hasPermission(String permission) {
    if (isSuperAdmin()) {
      return true;
    }
    return permissions != null && permissions.contains(permission);
  }
}
