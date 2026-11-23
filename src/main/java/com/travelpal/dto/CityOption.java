package com.travelpal.dto;

/**
 * One result of the city search on the plan form, e.g. name "Pisa", label "Pisa, Toscana, Italia".
 * The label helps tell apart cities with the same name (Paris, France vs Paris, Texas).
 */
public record CityOption(String name, String label, double latitude, double longitude) {
}
