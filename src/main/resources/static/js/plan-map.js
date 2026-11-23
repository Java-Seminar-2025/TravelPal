// Map on the plan detail page (Leaflet + OpenStreetMap tiles).
// - Colored numbered circles: the plan's stops (one color per day, the number is the order within the day).
// - Colored lines: walking route through each day's stops, along real streets and footpaths
//   (from the free OpenStreetMap walking router at routing.openstreetmap.de, nothing is stored).
// - Orange dots (owner/admin only): attractions found by GET /api/attractions?city=...
// - Clicking an orange dot or any spot on the map fills the "Add a place" form.
(function () {
    const mapEl = document.getElementById('map');
    if (!mapEl || !window.L) {
        return;
    }
    const city = mapEl.dataset.city;
    const canModify = mapEl.dataset.canModify === 'true';
    const status = document.getElementById('map-status');
    const dayColors = ['#2563eb', '#16a34a', '#9333ea', '#dc2626', '#0891b2', '#ca8a04', '#db2777'];

    const style = document.createElement('style');
    style.textContent = '.tp-stop{width:26px;height:26px;border-radius:50%;color:#fff;font:600 12px/22px system-ui,sans-serif;'
        + 'text-align:center;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.45)}';
    document.head.appendChild(style);

    const map = L.map(mapEl).setView([45, 15], 4);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    // 1. The plan's own stops, read from the itinerary table rows.
    const stops = [...document.querySelectorAll('tr.plan-item')]
        .filter(row => row.dataset.lat && row.dataset.lon)
        .map(row => ({
            name: row.dataset.name,
            lat: Number(row.dataset.lat),
            lon: Number(row.dataset.lon),
            day: Number(row.dataset.day),
            order: Number(row.dataset.order)
        }))
        .sort((a, b) => a.day - b.day || a.order - b.order);

    const colorFor = day => dayColors[(day - 1) % dayColors.length];
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
    if (stops.length) {
        map.fitBounds(L.latLngBounds(stops.map(s => [s.lat, s.lon])), {padding: [40, 40], maxZoom: 16});
    }
    drawWalkingRoutes();

    // 2. Attractions in the city (also used to center the map when the plan has no stops yet).
    if (canModify || !stops.length) {
        setStatus(canModify ? `Loading attractions in ${city}… (the first time can take a few seconds)` : '');
        fetch('/api/attractions?city=' + encodeURIComponent(city), {headers: {Accept: 'application/json'}})
            .then(response => {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                if (!stops.length) {
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
    // A failed day is simply skipped: the stops are still on the map.
    async function drawWalkingRoutes() {
        const summary = document.getElementById('route-summary');
        const days = [...new Set(stops.map(s => s.day))];
        for (const day of days) {
            const dayStops = stops.filter(s => s.day === day);
            if (dayStops.length < 2) {
                continue;
            }
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
                addSummaryLine(summary, day, formatRoute(route));
            } catch (e) {
                addSummaryLine(summary, day, 'walking route not available right now');
            }
        }
        if (summary && summary.children.length) {
            summary.insertAdjacentHTML('beforeend',
                '<li class="text-muted">Walking routes: <a href="https://routing.openstreetmap.de/" target="_blank" rel="noopener">FOSSGIS OSRM</a>, '
                + 'OpenStreetMap data. Always check the way on site.</li>');
        }
    }

    function formatRoute(route) {
        const km = (route.distance / 1000).toFixed(1);
        const minutes = Math.round(route.duration / 60);
        const time = minutes >= 60 ? `${Math.floor(minutes / 60)} h ${minutes % 60} min` : `${minutes} min`;
        return `${km} km · ${time} walk`;
    }

    function addSummaryLine(list, day, text) {
        if (!list) {
            return;
        }
        const item = document.createElement('li');
        item.innerHTML = `<span class="d-inline-block rounded-circle me-1" style="width:10px;height:10px;background:${colorFor(day)}"></span>`;
        item.append(`Day ${day}: ${text}`);
        list.append(item);
    }

    let pickedMarker = null;

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
