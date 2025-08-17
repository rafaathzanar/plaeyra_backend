package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlertDTO {
    private String type;
    private String message;
    private LocalDateTime timestamp;
} 