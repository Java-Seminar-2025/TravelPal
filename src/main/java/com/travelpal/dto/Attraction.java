package com.travelpal.dto;

/**
 * One place from OpenStreetMap that can be added to a plan.
 *
 * @param osmId  OpenStreetMap id, e.g. "w43768260" (w = way, n = node, r = relation)
 * @param type   OSM type such as "museum", "viewpoint", "castle"
 * @param famous true if the place has a Wikipedia/Wikidata link (used to show it first)
 */
public record Attraction(String osmId, String name, String type, double latitude, double longitude, boolean famous) {
}
