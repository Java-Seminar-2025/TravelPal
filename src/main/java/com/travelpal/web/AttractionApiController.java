package com.travelpal.web;

import com.travelpal.dto.CityAttractions;
import com.travelpal.service.AttractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** JSON API used by the map on the plan page. Requires login (like every page except /, /login, /register). */
@RestController
@RequestMapping("/api/attractions")
@RequiredArgsConstructor
public class AttractionApiController {

    private final AttractionService attractionService;

    /** Example: GET /api/attractions?city=Split */
    @GetMapping
    public CityAttractions attractions(@RequestParam String city) {
        return attractionService.findAttractions(city);
    }
}
