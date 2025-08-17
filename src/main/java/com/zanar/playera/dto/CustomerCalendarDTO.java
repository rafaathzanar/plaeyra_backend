package com.zanar.playera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCalendarDTO {
    private List<BookingResponseDTO> pastBookings;
    private List<BookingResponseDTO> upcomingBookings;
} 