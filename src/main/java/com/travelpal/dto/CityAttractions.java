package com.travelpal.dto;

import java.util.List;

/** A city's map center plus the attractions found in it. Returned as JSON by GET /api/attractions?city=... */
public record CityAttractions(String city, double latitude, double longitude, List<Attraction> attractions) {
}
