package com.travelpal.web;

import com.travelpal.dto.CityAttractions;
import com.travelpal.dto.CityOption;
import com.travelpal.service.AttractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** JSON API used by the maps (plan page and plan form). Requires login, like every page except /, /login, /register. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MapApiController {

    private final AttractionService attractionService;

    /** Example: GET /api/attractions?city=Split */
    @GetMapping("/attractions")
    public CityAttractions attractions(@RequestParam String city) {
        return attractionService.findAttractions(city);
    }

    /** Example: GET /api/cities?q=Pisa -> Pisa, Toscana, Italia (+ coordinates) */
    @GetMapping("/cities")
    public List<CityOption> cities(@RequestParam String q) {
        return attractionService.searchCities(q);
    }
}
