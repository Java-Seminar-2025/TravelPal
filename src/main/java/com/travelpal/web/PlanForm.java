package com.travelpal.web;

import com.travelpal.entity.Plan;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlanForm {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    /** Filled in when the user picks the city from the search results on the form. */
    @NotNull(message = "Search for the city and pick it from the list")
    private Double cityLatitude;

    @NotNull(message = "Search for the city and pick it from the list")
    private Double cityLongitude;

    @NotNull(message = "Days count is required")
    @Min(value = 1, message = "Plan must be at least 1 day long")
    @Max(value = 60, message = "Plan cannot exceed 60 days")
    private Integer days = 3;

    @NotNull
    private Plan.Visibility visibility = Plan.Visibility.PRIVATE;

    private boolean aiGenerated = false;
}