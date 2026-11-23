// City picker on the "Create / Edit plan" form.
// Type a city, press Find (or Enter), pick the right one from the list: the hidden latitude/longitude fields are filled
// and the small map jumps to that city. The search runs only on Find, never while typing (OpenStreetMap's rule).
(function () {
    const cityInput = document.getElementById('city');
    const latInput = document.getElementById('cityLatitude');
    const lonInput = document.getElementById('cityLongitude');
    const results = document.getElementById('city-results');
    const status = document.getElementById('city-status');
    const findButton = document.getElementById('city-find');
    if (!cityInput || !window.L) {
        return;
    }

    const map = L.map('city-map', {scrollWheelZoom: false}).setView([45, 15], 3);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);
    let marker = null;

    // Editing a plan (or the page came back with a form error): show the city that is already chosen.
    if (latInput.value && lonInput.value) {
        showCity(Number(latInput.value), Number(lonInput.value));
        status.textContent = '✓ ' + cityInput.value;
    }

    findButton.addEventListener('click', findCities);
    cityInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            e.preventDefault();   // Enter searches instead of submitting the whole form
            findCities();
        }
    });
    // Typing a different city makes the old choice invalid until a new one is picked.
    cityInput.addEventListener('input', () => {
        latInput.value = '';
        lonInput.value = '';
        status.textContent = 'Press Find and pick the city from the list.';
    });

    async function findCities() {
        const query = cityInput.value.trim();
        results.replaceChildren();
        if (!query) {
            status.textContent = 'Type a city name first.';
            return;
        }
        status.textContent = 'Searching…';
        findButton.disabled = true;
        try {
            const response = await fetch('/api/cities?q=' + encodeURIComponent(query), {headers: {Accept: 'application/json'}});
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            const cities = await response.json();
            if (!cities.length) {
                status.textContent = 'No city found with that name. Check the spelling, or try the local name (e.g. Firenze).';
                return;
            }
            status.textContent = cities.length === 1 ? 'Is this the right city?' : 'Which one do you mean?';
            cities.forEach(c => {
                const option = document.createElement('button');
                option.type = 'button';
                option.className = 'list-group-item list-group-item-action small';
                option.textContent = c.label;
                option.addEventListener('click', () => choose(c));
                results.append(option);
            });
        } catch (e) {
            status.textContent = 'City search is not available right now. Please try again in a moment.';
        } finally {
            findButton.disabled = false;
        }
    }

    function choose(city) {
        cityInput.value = city.name;
        latInput.value = city.latitude;
        lonInput.value = city.longitude;
        results.replaceChildren();
        status.textContent = '✓ ' + city.label;
        showCity(city.latitude, city.longitude);
    }

    function showCity(lat, lon) {
        map.setView([lat, lon], 12);
        if (marker) {
            marker.remove();
        }
        marker = L.marker([lat, lon]).addTo(map);
    }
})();
