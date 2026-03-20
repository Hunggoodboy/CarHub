document.addEventListener("DOMContentLoaded", () => {
    // --- CẤU HÌNH KEY (Thay key của bạn lấy từ console.goong.io) ---
    const GOONG_MAP_KEY = 'NAuy0xKrg4BYVnfEnD6OKD3GS8hJ1prrWtm1naq8'; // Map Key (m_...)
    const GOONG_API_KEY = '7wYgWYbRnGZRFPMBG6blGJQRLN7Mz5eyE3apnifh'; // API Key (a_...)

    const params = new URLSearchParams(window.location.search);
    const carId = params.get("id");

    const streetInput = document.getElementById('street-input');
    const wardInput = document.getElementById('ward-input');
    const cityInput = document.getElementById('city-input');
    const phoneInput = document.getElementById('phone-input');
    const suggestionsBox = document.getElementById('address-suggestions');
    const mapContainer = document.getElementById('map-container');

    const confirmBtn = document.querySelector(".payment-btn");
    const modal = document.getElementById("bill-modal");
    const overlay = document.getElementById("bill-overlay");
    const closeBtn = document.getElementById("close-bill");
    const qrBox = document.getElementById("qr-box");
    const submitBtn = document.getElementById("submit-order-btn");

    // 1. KHỞI TẠO MAP GOONG
    goongjs.accessToken = GOONG_MAP_KEY;
    const map = new goongjs.Map({
        container: 'map',
        style: 'https://tiles.goong.io/assets/navigation_day.json',
        center: [105.8342, 21.0278], // Mặc định ở Hà Nội
        zoom: 13
    });
    
    const marker = new goongjs.Marker({
          draggable: true // Cho phép kéo ghim bằng chuột
    })
    .setLngLat([105.8342, 21.0278])
    .addTo(map);

    // Lắng nghe sự kiện khi người dùng kéo xong ghim
    marker.on('dragend', function() {
        const lngLat = marker.getLngLat();
        console.log('Tọa độ mới:', lngLat.lng, lngLat.lat);
    
        // (Tùy chọn) Bạn có thể dùng Reverse Geocoding của Goong 
        // để cập nhật lại tên đường vào ô input khi người dùng thả ghim
        fetch(`https://rsapi.goong.io/Geocode?latlng=${lngLat.lat},${lngLat.lng}&api_key=${GOONG_API_KEY}`)
            .then(res => res.json())
            .then(data => {
                if (data.results && data.results.length > 0) {
                    streetInput.value = data.results[0].formatted_address;
                }
            });
    });

    // 2. XỬ LÝ TẢI GIÁ XE
    if (carId) {
        fetch(`/api/cars/${carId}`)
            .then(res => res.json())
            .then(data => {
                const car = data.car;
                if (!car) return;
                const priceEl = document.getElementById("finalPrice");
                if (priceEl) {
                    const price = car.finalPrice || car.price;
                    priceEl.innerText = new Intl.NumberFormat('vi-VN').format(price) + ' ₫';
                }
            })
            .catch(err => console.error("Lỗi load giá xe:", err));
    }

    // 3. TÌM KIẾM ĐỊA CHỈ (AUTOCOMPLETE)
    let searchTimeout = null;
    streetInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        const query = this.value.trim();
        if (query.length < 4) { suggestionsBox.innerHTML = ''; return; }

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
                                // Lấy chi tiết Place
                                fetch(`https://rsapi.goong.io/Place/Detail?api_key=${GOONG_API_KEY}&place_id=${item.place_id}`)
                                    .then(res => res.json())
                                    .then(detail => {
                                        const res = detail.result;
                                        const loc = res.geometry.location;
                                        
                                        // Hiển thị Map
                                        mapContainer.style.display = 'block';
                                        map.resize();
                                        map.flyTo({ center: [loc.lng, loc.lat], zoom: 16 });
                                        marker.setLngLat([loc.lng, loc.lat]);

                                        // Điền dữ liệu
                                        streetInput.value = res.name + ", " + res.formatted_address;
                                        suggestionsBox.innerHTML = '';
                                        
                                        // Tự động điền Xã/Tỉnh
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

    // 4. XÁC NHẬN THANH TOÁN (HIỆN BILL)
    confirmBtn?.addEventListener("click", () => {
        const address = streetInput.value.trim();
        const ward = wardInput.value.trim();
        const city = cityInput.value.trim();
        const phone = phoneInput.value.trim();

        if (!address || !ward || !city || !phone) {
            alert("Vui lòng điền đầy đủ địa chỉ và số điện thoại!");
            return;
        }

        const methodLabel = document.querySelector('input[name="paymentMethod"]:checked')
                            .parentElement.innerText.trim();

        document.getElementById("bill-price").innerText = document.getElementById("finalPrice").innerText;
        document.getElementById("bill-address").innerText = `${address}`;
        document.getElementById("bill-phone").innerText = phone;
        document.getElementById("bill-method").innerText = methodLabel;

        qrBox.style.display = methodLabel.includes("Chuyển khoản") ? "block" : "none";
        overlay.style.display = "block";
        modal.style.display = "block";
    });

    // 5. GỬI ĐƠN HÀNG LÊN BE (GIỮ NGUYÊN TÊN BIẾN CỦA BẠN)
    submitBtn?.addEventListener("click", () => {
        const data = {
            carId: carId,
            street: streetInput.value.trim(),
            ward: wardInput.value.trim(),
            city: cityInput.value.trim(),
            phone: phoneInput.value.trim(),
            paymentMethod: document.querySelector('input[name="paymentMethod"]:checked').value
        };

        fetch("/api/orders", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        })
        .then(res => {
            if (!res.ok) throw new Error("Gửi yêu cầu thất bại");
            return res.json();
        })
        .then(() => {
            alert("Gửi yêu cầu mua xe thành công!");
            closeModal();
        })
        .catch(err => {
            console.error(err);
            alert("Có lỗi xảy ra, vui lòng thử lại!");
        });
    });

    function closeModal() {
        modal.style.display = "none";
        overlay.style.display = "none";
    }
    closeBtn?.addEventListener("click", closeModal);
    overlay?.addEventListener("click", closeModal);
});
