package com.travelpal.service;

import com.travelpal.dto.Attraction;
import com.travelpal.dto.CityAttractions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Plain unit test: OpenStreetMap is replaced by a fake server, so no internet is needed. */
class AttractionServiceTest {

    private MockRestServiceServer osm;
    private AttractionService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://nominatim.test");
        osm = MockRestServiceServer.bindTo(builder).build();
        service = new AttractionService(builder.build(), 0);   // 0 = no 1-second wait in tests
    }

    @Test
    void mergesCategoriesDropsDuplicatesAndPutsFamousPlacesFirst() {
        expectSearch("Split", """
                [{"name":"Split","lat":"43.51","lon":"16.44","type":"administrative","osm_type":"relation","osm_id":1}]""");
        expectSearch("attractions in Split", """
                [{"name":"Small statue","lat":"43.50","lon":"16.43","type":"artwork","osm_type":"node","osm_id":10},
                 {"name":"Diocletian's Palace","lat":"43.5085","lon":"16.4402","type":"attraction","osm_type":"way","osm_id":20,
                  "extratags":{"wikidata":"Q193066"}}]""");
        expectSearch("museums in Split", """
                [{"name":"Diocletian's Palace","lat":"43.5085","lon":"16.4402","type":"museum","osm_type":"way","osm_id":20},
                 {"name":"","lat":"43.0","lon":"16.0","type":"museum","osm_type":"node","osm_id":30}]""");
        expectSearch("churches in Split", "[]");
        expectSearch("castles in Split", "[]");
        expectSearch("monuments in Split", "[]");
        expectSearch("viewpoints in Split", "[]");

        CityAttractions result = service.findAttractions("Split");

        assertThat(result.city()).isEqualTo("Split");
        assertThat(result.latitude()).isEqualTo(43.51);
        assertThat(result.attractions()).extracting(Attraction::name)
                .containsExactly("Diocletian's Palace", "Small statue");   // famous first, duplicate and nameless dropped
        assertThat(result.attractions().get(0).osmId()).isEqualTo("w20");
        osm.verify();
    }

    @Test
    void secondLookupOfTheSameCityUsesTheCache() {
        expectSearch("Split", """
                [{"name":"Split","lat":"43.51","lon":"16.44","type":"administrative","osm_type":"relation","osm_id":1}]""");
        for (String category : new String[]{"attractions", "museums", "churches", "castles", "monuments", "viewpoints"}) {
            expectSearch(category + " in Split", "[]");
        }

        service.findAttractions("Split");
        service.findAttractions(" split ");   // same city: no new requests (the fake server would fail on extra calls)
        osm.verify();
    }

    @Test
    void oneFailingCategoryDoesNotBreakTheResult() {
        expectSearch("Split", """
                [{"name":"Split","lat":"43.51","lon":"16.44","type":"administrative","osm_type":"relation","osm_id":1}]""");
        osm.expect(requestTo(containsString("/search"))).andExpect(queryParam("q", "attractions%20in%20Split"))
                .andRespond(withServerError());
        expectSearch("museums in Split", """
                [{"name":"Galerija umjetnina","lat":"43.51","lon":"16.44","type":"museum","osm_type":"relation","osm_id":5}]""");
        expectSearch("churches in Split", "[]");
        expectSearch("castles in Split", "[]");
        expectSearch("monuments in Split", "[]");
        expectSearch("viewpoints in Split", "[]");

        assertThat(service.findAttractions("Split").attractions()).extracting(Attraction::name)
                .containsExactly("Galerija umjetnina");
    }

    @Test
    void prefersTheCityOverAProvinceWithTheSameName() {
        expectSearch("Pisa", """
                [{"name":"Pisa","lat":"43.4715","lon":"10.6798","type":"administrative","addresstype":"county","osm_type":"relation","osm_id":1},
                 {"name":"Pisa","lat":"43.7159","lon":"10.4019","type":"administrative","addresstype":"city","osm_type":"relation","osm_id":2}]""");
        for (String category : new String[]{"attractions", "museums", "churches", "castles", "monuments", "viewpoints"}) {
            expectSearch(category + " in Pisa", "[]");
        }

        CityAttractions result = service.findAttractions("Pisa");

        assertThat(result.latitude()).isEqualTo(43.7159);
        assertThat(result.longitude()).isEqualTo(10.4019);
    }

    @Test
    void unknownCityIsNotFound() {
        expectSearch("Atlantis", "[]");

        assertThatThrownBy(() -> service.findAttractions("Atlantis")).isInstanceOf(NoSuchElementException.class);
    }

    private void expectSearch(String query, String json) {
        osm.expect(requestTo(containsString("/search")))
                .andExpect(queryParam("q", query.replace(" ", "%20").replace("'", "%27")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}
