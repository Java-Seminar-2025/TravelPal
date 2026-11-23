package com.travelpal.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** "Add to plan" form. Latitude/longitude/osmId are filled in by clicking on the map. */
@Data
public class PlanItemForm {

    @NotBlank(message = "Give the place a name")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @NotNull(message = "Pick a place on the map first")
    @DecimalMin("-90") @DecimalMax("90")
    private Double latitude;

    @NotNull(message = "Pick a place on the map first")
    @DecimalMin("-180") @DecimalMax("180")
    private Double longitude;

    @Size(max = 40)
    private String osmId;

    @NotNull(message = "Day number is required")
    @Min(value = 1, message = "Day number must be at least 1")
    private Integer dayNumber = 1;

    @NotNull(message = "Order index is required")
    @Min(value = 0, message = "Order index cannot be negative")
    private Integer orderIndex = 1;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}
