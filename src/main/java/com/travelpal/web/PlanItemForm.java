package com.travelpal.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlanItemForm {

    @NotBlank(message = "Location ID is required")
    @Size(max = 30, message = "Location ID must be at most 30 characters")
    private String tripadvisorLocationId;

    @NotNull(message = "Day number is required")
    @Min(value = 1, message = "Day number must be at least 1")
    private Integer dayNumber = 1;

    @NotNull(message = "Order index is required")
    @Min(value = 0, message = "Order index cannot be negative")
    private Integer orderIndex = 1;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}