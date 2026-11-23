// Map on the plan detail page (Leaflet + OpenStreetMap tiles).
// - Colored numbered circles: the plan's stops (one color per day, the number is the order within the day).
// - Colored lines: walking route through each day's stops, along real streets and footpaths
//   (from the free OpenStreetMap walking router at routing.openstreetmap.de, nothing is stored).
// - Orange dots (owner/admin only): attractions found by GET /api/attractions?city=...
// - Clicking an orange dot or any spot on the map fills the "Add a place" form.
// Day colors come from css/plan.css, so the map and the plan list always match.
(function () {
    const mapEl = document.getElementById('map');
    if (!mapEl || !window.L) {
        return;
    }
    const city = mapEl.dataset.city;
    const cityLat = mapEl.dataset.lat ? Number(mapEl.dataset.lat) : null;
    const cityLon = mapEl.dataset.lon ? Number(mapEl.dataset.lon) : null;
    const canModify = mapEl.dataset.canModify === 'true';
    const status = document.getElementById('map-status');

    const rootStyle = getComputedStyle(document.documentElement);
    const colorFor = day => rootStyle.getPropertyValue('--tp-day-' + ((day - 1) % 7)).trim();

    const map = L.map(mapEl).setView([45, 15], 4);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    // 1. The plan's own stops, read from the plan list.
    const stops = [...document.querySelectorAll('.plan-item')]
        .filter(row => row.dataset.lat && row.dataset.lon)
        .map(row => ({
            name: row.dataset.name,
            lat: Number(row.dataset.lat),
            lon: Number(row.dataset.lon),
            day: Number(row.dataset.day),
            order: Number(row.dataset.order)
        }))
        .sort((a, b) => a.day - b.day || a.order - b.order);

    stops.forEach(stop => {
        const icon = L.divIcon({
            className: '',
            html: `<div class="tp-stop" style="background:${colorFor(stop.day)}">${stop.order}</div>`,
            iconSize: [26, 26],
            iconAnchor: [13, 13]
        });
        L.marker([stop.lat, stop.lon], {icon, zIndexOffset: 1000})
            .addTo(map)
            .bindTooltip(`Day ${stop.day} · ${stop.order}. ${escapeHtml(stop.name)}`);
    });

    // Where to look: the stops if there are any, otherwise the city chosen for the plan.
    // Done again once the page has fully loaded, in case the map's size changed while styles were loading.
    function frameMap() {
        map.invalidateSize();
        if (stops.length) {
            map.fitBounds(L.latLngBounds(stops.map(s => [s.lat, s.lon])), {padding: [40, 40], maxZoom: 16});
        } else if (cityLat !== null && cityLon !== null) {
            map.setView([cityLat, cityLon], 14);
        }
    }
    frameMap();
    window.addEventListener('load', frameMap, {once: true});
    drawWalkingRoutes();

    // 2. Attractions in the city. Plans made before the city picker existed have no coordinates:
    //    then this request also tells us where the city is.
    const needsCityFromApi = !stops.length && cityLat === null;
    if (canModify || needsCityFromApi) {
        setStatus(canModify ? `Loading attractions in ${city}… (the first time can take a few seconds)` : '');
        fetch('/api/attractions?city=' + encodeURIComponent(city), {headers: {Accept: 'application/json'}})
            .then(response => {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                if (needsCityFromApi) {
                    map.setView([data.latitude, data.longitude], 14);
                }
                if (!canModify) {
                    return;
                }
                data.attractions.forEach(a => {
                    L.circleMarker([a.latitude, a.longitude], {
                        radius: a.famous ? 7 : 5, color: '#c2410c', weight: 1, fillColor: '#f97316', fillOpacity: 0.85
                    }).addTo(map).bindPopup(popup(a.name, a.type, () => pick(a.name, a.latitude, a.longitude, a.osmId)));
                });
                setStatus(`${data.attractions.length} attractions found in ${data.city}. Click one to add it, or click anywhere on the map to mark your own spot.`);
            })
            .catch(() => setStatus(canModify
                ? 'Could not load attractions right now. You can still click anywhere on the map to add your own spot.'
                : ''));
    }

    // 3. Owners/admins can click anywhere to mark their own spot.
    let pickedMarker = null;
    if (canModify) {
        map.on('click', e => {
            L.popup()
                .setLatLng(e.latlng)
                .setContent(popup('Your own spot', e.latlng.lat.toFixed(5) + ', ' + e.latlng.lng.toFixed(5),
                    () => pick('', e.latlng.lat, e.latlng.lng, '')))
                .openOn(map);
        });
        // After a form error the page reloads with the picked coordinates still filled in: show them again.
        const lat = document.getElementById('latitude').value;
        const lon = document.getElementById('longitude').value;
        if (lat && lon) {
            showPicked(Number(lat), Number(lon));
        }
    }

    // Walking route for each day with 2+ stops, requested one day after another (the router asks for light use).
    // The distance and time go into that day's header in the plan list. A failed day is simply skipped.
    async function drawWalkingRoutes() {
        const days = [...new Set(stops.map(s => s.day))];
        for (const day of days) {
            const dayStops = stops.filter(s => s.day === day);
            if (dayStops.length < 2) {
                continue;
            }
            const label = document.getElementById('route-day-' + day);
            const points = dayStops.map(s => `${s.lon},${s.lat}`).join(';');   // the router wants lon,lat
            try {
                const response = await fetch('https://routing.openstreetmap.de/routed-foot/route/v1/foot/'
                    + points + '?overview=full&geometries=geojson');
                const data = await response.json();
                if (!response.ok || data.code !== 'Ok') {
                    throw new Error(data.code || response.status);
                }
                const route = data.routes[0];
                L.polyline(route.geometry.coordinates.map(([lon, lat]) => [lat, lon]),
                    {color: colorFor(day), weight: 5, opacity: 0.7}).addTo(map)
                    .bindTooltip(`Day ${day}: ${formatRoute(route)}`, {sticky: true});
                if (label) {
                    label.textContent = formatRoute(route);
                }
                document.getElementById('route-credit').hidden = false;
            } catch (e) {
                if (label) {
                    label.textContent = 'walking route not available right now';
                }
            }
        }
    }

    function formatRoute(route) {
        const km = (route.distance / 1000).toFixed(1);
        const minutes = Math.round(route.duration / 60);
        const time = minutes >= 60 ? `${Math.floor(minutes / 60)} h ${minutes % 60} min` : `${minutes} min`;
        return `${km} km · ${time} walk`;
    }

    function pick(name, lat, lon, osmId) {
        document.getElementById('name').value = name;
        document.getElementById('latitude').value = lat;
        document.getElementById('longitude').value = lon;
        document.getElementById('osmId').value = osmId || '';
        map.closePopup();
        showPicked(lat, lon);
        const nameInput = document.getElementById('name');
        nameInput.scrollIntoView({behavior: 'smooth', block: 'center'});
        nameInput.focus();
    }

    function showPicked(lat, lon) {
        if (pickedMarker) {
            pickedMarker.remove();
        }
        pickedMarker = L.marker([lat, lon]).addTo(map).bindTooltip('Selected place');
        document.getElementById('picked-coords').textContent = `📍 ${lat.toFixed(5)}, ${lon.toFixed(5)}`;
    }

    // Popup with a title, a grey subtitle and an "Add to plan" button (built as DOM elements, so names can't inject HTML).
    function popup(title, subtitle, onAdd) {
        const box = document.createElement('div');
        const strong = document.createElement('strong');
        strong.textContent = title;
        box.append(strong);
        if (subtitle) {
            const small = document.createElement('div');
            small.className = 'small text-muted';
            small.textContent = subtitle;
            box.append(small);
        }
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn btn-primary btn-sm mt-2';
        button.textContent = 'Add to plan';
        button.addEventListener('click', onAdd);
        box.append(button);
        return box;
    }

    function setStatus(text) {
        if (status) {
            status.textContent = text;
        }
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
})();
