-- Plan items are now places on the map: an attraction found on OpenStreetMap
-- or any spot the user clicked. Each item stores its name and coordinates.

ALTER TABLE plan_items ADD COLUMN name VARCHAR(200);
ALTER TABLE plan_items ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE plan_items ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE plan_items ADD COLUMN osm_id VARCHAR(40);   -- e.g. "w43768260" (way), "n850150112" (node); empty for clicked spots

-- Items created before this change only had a Tripadvisor id: use it as their name.
UPDATE plan_items SET name = tripadvisor_location_id WHERE name IS NULL;
ALTER TABLE plan_items ALTER COLUMN name SET NOT NULL;

-- Tripadvisor is optional from now on.
ALTER TABLE plan_items ALTER COLUMN tripadvisor_location_id DROP NOT NULL;
