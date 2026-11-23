-- The city is now picked from OpenStreetMap when creating a plan, so we also store where it is.
-- The plan map opens on these coordinates. Older plans keep NULL here and the map looks the city up by name.

ALTER TABLE plans ADD COLUMN city_latitude DOUBLE PRECISION;
ALTER TABLE plans ADD COLUMN city_longitude DOUBLE PRECISION;
