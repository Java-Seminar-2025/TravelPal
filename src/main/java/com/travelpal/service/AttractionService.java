package com.travelpal.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelpal.dto.Attraction;
import com.travelpal.dto.CityAttractions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds sightseeing spots in a city using OpenStreetMap's Nominatim search.
 *
 * Nominatim understands searches like "museums in Florence", so we run one search per category,
 * merge the results and put famous places (those with a Wikipedia link) first.
 * Results are kept in memory, so each city is only looked up once while the app runs.
 */
@Slf4j
@Service
public class AttractionService {

    private static final List<String> CATEGORIES = List.of("attractions", "museums", "churches", "castles", "monuments", "viewpoints");
    private static final Set<String> SETTLEMENT_TYPES = Set.of("city", "town", "village", "municipality", "hamlet");
    private static final int MAX_RESULTS = 60;

    private final RestClient nominatim;
    private final long delayBetweenRequestsMillis;
    private final Map<String, CityAttractions> cache = new ConcurrentHashMap<>();
    private long lastRequestAt = 0;

    public AttractionService(RestClient nominatimClient,
                             @Value("${travelpal.osm.request-delay-ms:1100}") long delayBetweenRequestsMillis) {
        this.nominatim = nominatimClient;
        this.delayBetweenRequestsMillis = delayBetweenRequestsMillis;
    }

    public CityAttractions findAttractions(String city) {
        String cityName = city.trim();
        String cacheKey = cityName.toLowerCase(Locale.ROOT);
        CityAttractions cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Place center = findCityCenter(cityName);

        Map<String, Place> found = new LinkedHashMap<>();   // keyed by OSM id, so duplicates are dropped
        for (String category : CATEGORIES) {
            try {
                search(category + " in " + cityName).stream()
                        .filter(place -> place.name() != null && !place.name().isBlank())
                        .forEach(place -> found.putIfAbsent(place.osmId(), place));
            } catch (RestClientException e) {
                log.warn("Searching '{} in {}' failed, skipping this category: {}", category, cityName, e.getMessage());
            }
        }

        List<Attraction> attractions = found.values().stream()
                .sorted(Comparator.comparing(Place::famous).reversed())
                .limit(MAX_RESULTS)
                .map(Place::toAttraction)
                .toList();

        CityAttractions result = new CityAttractions(center.name(), center.latitude(), center.longitude(), attractions);
        cache.put(cacheKey, result);
        return result;
    }

    /**
     * The city itself, not a region with the same name: searching "Pisa" first returns the province of Pisa
     * (30 km away), so we prefer a result that is a city, town or village.
     */
    private Place findCityCenter(String cityName) {
        List<Place> matches = search(cityName);
        return matches.stream()
                .filter(place -> place.addresstype() != null && SETTLEMENT_TYPES.contains(place.addresstype()))
                .findFirst()
                .or(() -> matches.stream().findFirst())
                .orElseThrow(() -> new NoSuchElementException("City not found: " + cityName));
    }

    /** One Nominatim search. synchronized + wait = never more than one request per second (their rule). */
    private synchronized List<Place> search(String query) {
        waitForRateLimit();
        Place[] places = nominatim.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "jsonv2")
                        .queryParam("limit", 40)
                        .queryParam("extratags", 1)
                        .build())
                .retrieve()
                .body(Place[].class);
        return places == null ? List.of() : List.of(places);
    }

    private void waitForRateLimit() {
        long wait = lastRequestAt + delayBetweenRequestsMillis - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }

    /** The parts of a Nominatim search result we use (it sends lat/lon as text). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Place(String name, String lat, String lon, String type, String addresstype,
                 @JsonProperty("osm_type") String osmType,
                 @JsonProperty("osm_id") long osmNumber,
                 Map<String, String> extratags) {

        String osmId() {
            return osmType.charAt(0) + String.valueOf(osmNumber);
        }

        double latitude() {
            return Double.parseDouble(lat);
        }

        double longitude() {
            return Double.parseDouble(lon);
        }

        boolean famous() {
            return extratags != null && (extratags.containsKey("wikidata") || extratags.containsKey("wikipedia"));
        }

        Attraction toAttraction() {
            return new Attraction(osmId(), name, type, latitude(), longitude(), famous());
        }
    }
}
