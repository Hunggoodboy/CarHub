document.addEventListener("DOMContentLoaded", () => {
    const GOONG_MAP_KEY = 'NAuy0xKrg4BYVnfEnD6OKD3GS8hJ1prrWtm1naq8';
    const GOONG_API_KEY = '7wYgWYbRnGZRFPMBG6blGJQRLN7Mz5eyE3apnifh';

    const streetInput = document.getElementById('street-input');
    const wardInput = document.getElementById('ward-input');
    const cityInput = document.getElementById('city-input');
    const phoneInput = document.getElementById('phone-input');
    const errCarInput = document.getElementById('err_car');
    const carIdField = document.getElementById("car_id");
    const suggestionsBox = document.getElementById('address-suggestions');
    const submitBtn = document.getElementById("submit-warranty-btn");

    console.log("Submit button:", submitBtn); // DEBUG

    // MAP
    goongjs.accessToken = GOONG_MAP_KEY;
    const map = new goongjs.Map({
        container: 'map',
        style: 'https://tiles.goong.io/assets/goong_map_web.json',
        center: [105.8342, 21.0278],
        zoom: 13
    });

    const marker = new goongjs.Marker({ draggable: true })
        .setLngLat([105.8342, 21.0278])
        .addTo(map);

    marker.on('dragend', function() {
        const lngLat = marker.getLngLat();
        fetch(`https://rsapi.goong.io/Geocode?latlng=${lngLat.lat},${lngLat.lng}&api_key=${GOONG_API_KEY}`)
            .then(res => res.json())
            .then(data => {
                if (data.results && data.results.length > 0) {
                    streetInput.value = data.results[0].formatted_address;
                }
            });
    });

    // AUTOCOMPLETE
    let searchTimeout = null;
    streetInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        const query = this.value.trim();
        if (query.length < 2) {
            suggestionsBox.innerHTML = '';
            return;
        }

        searchTimeout = setTimeout(() => {
            fetch(`https://rsapi.goong.io/Place/AutoComplete?api_key=${GOONG_API_KEY}&input=${encodeURIComponent(query)}`)
                .then(res => res.json())
                .then(data => {
                    suggestionsBox.innerHTML = '';
                    if (data.predictions) {
                        data.predictions.forEach(item => {
                            const div = document.createElement('div');
                            div.className = 'suggestion-item';
                            div.innerText = item.description;

                            div.onclick = () => {
                                fetch(`https://rsapi.goong.io/Place/Detail?api_key=${GOONG_API_KEY}&place_id=${item.place_id}`)
                                    .then(res => res.json())
                                    .then(detail => {
                                        const res = detail.result;
                                        const loc = res.geometry.location;

                                        const mapContainer = document.getElementById('map-container');
                                        if (mapContainer) {
                                            mapContainer.style.display = 'block';
                                        }

                                        map.resize();
                                        map.flyTo({ center: [loc.lng, loc.lat], zoom: 16 });
                                        marker.setLngLat([loc.lng, loc.lat]);

                                        streetInput.value = res.name + ", " + res.formatted_address;
                                        suggestionsBox.innerHTML = '';

                                        if (res.compound) {
                                            cityInput.value = res.compound.province || "";
                                            wardInput.value = res.compound.commune || "";
                                        }
                                    });
                            };

                            suggestionsBox.appendChild(div);
                        });
                    }
                });
        }, 300);
    });

    // SUBMIT
    submitBtn?.addEventListener("click", function(e) {
        e.preventDefault(); // 🔥 FIX QUAN TRỌNG

        if (!carIdField || !carIdField.value) {
            alert("Không xác định được xe cần bảo hành.");
            return;
        }

        if (!streetInput.value || !phoneInput.value || !errCarInput.value) {
            alert("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        const data = {
            carId: parseInt(carIdField.value), // 🔥 FIX
            street: streetInput.value.trim(),
            ward: wardInput.value.trim(),
            city: cityInput.value.trim(),
            phone: phoneInput.value.trim(),
            defectDescription: errCarInput.value.trim()
        };

        console.log("DATA SEND:", data); // DEBUG

        fetch("/api/warranty/create", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })
        .then(async res => {
            const text = await res.text();
            console.log("RESPONSE:", text); // DEBUG

            if (!res.ok) throw new Error(text);
            return JSON.parse(text);
        })
        .then(result => {
            alert("Gửi yêu cầu bảo hành xe thành công!");
            window.location.href = "/index";
        })
        .catch(err => {
            console.error(err);
            alert("Lỗi: " + err.message);
        });
    });

    // CLICK OUTSIDE
    document.addEventListener('click', (e) => {
        if (suggestionsBox && !suggestionsBox.contains(e.target) && e.target !== streetInput) {
            suggestionsBox.innerHTML = '';
        }
    });
});