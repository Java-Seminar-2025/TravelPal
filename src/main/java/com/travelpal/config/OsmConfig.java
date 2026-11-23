package com.travelpal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP client for OpenStreetMap's Nominatim search (free, no API key).
 * Usage policy: identify the app in the User-Agent, max 1 request per second, cache results.
 * https://operations.osmfoundation.org/policies/nominatim/
 */
@Configuration
public class OsmConfig {

    @Bean
    RestClient nominatimClient() {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(5));
        timeouts.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader(HttpHeaders.USER_AGENT, "TravelPal/0.1 (university student project)")
                .requestFactory(timeouts)
                .build();
    }
}
