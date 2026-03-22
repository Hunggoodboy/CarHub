document.addEventListener("DOMContentLoaded", () => {
    // --- CẤU HÌNH KEY GOONG ---
    const GOONG_MAP_KEY = 'NAuy0xKrg4BYVnfEnD6OKD3GS8hJ1prrWtm1naq8';
    const GOONG_API_KEY = '7wYgWYbRnGZRFPMBG6blGJQRLN7Mz5eyE3apnifh';

    const params = new URLSearchParams(window.location.search);
    const carId = params.get("id");

    // Elements địa chỉ
    const streetInput = document.getElementById('street-input');
    const wardInput = document.getElementById('ward-input');
    const cityInput = document.getElementById('city-input');
    const phoneInput = document.getElementById('phone-input');
    const suggestionsBox = document.getElementById('address-suggestions');
    const mapContainer = document.getElementById('map-container');

    // Elements đơn hàng (Cần thiết cho tính toán)
    const quantityInput = document.getElementById("quantity-input");
    const unitPriceEl = document.getElementById("unit-price");
    const originalPriceEl = document.getElementById("original-price");
    const discountPriceEl = document.getElementById("discount-price");
    const finalPriceTotalEl = document.getElementById("finalPrice");

    // Elements Modal
    const confirmBtn = document.querySelector(".payment-btn");
    const modal = document.getElementById("bill-modal");
    const overlay = document.getElementById("bill-overlay");
    const closeBtn = document.getElementById("close-bill");
    const qrBox = document.getElementById("qr-box");
    const submitBtn = document.getElementById("submit-order-btn");

    const formatter = new Intl.NumberFormat('vi-VN');

    // 1. KHỞI TẠO MAP GOONG (Giữ nguyên logic cũ)
    goongjs.accessToken = GOONG_MAP_KEY;
    const map = new goongjs.Map({
        container: 'map',
        style: 'https://tiles.goong.io/assets/navigation_day.json',
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

    // xu ly du lieu xe , tinh toan
    if (carId) {
        fetch(`/api/cars/${carId}`)
            .then(res => res.json())
            .then(data => {
                const car = data.car;
                if (!car) return;

                // lay gia
                const unitPriceValue = car.finalPrice || car.price; 
                const originalPriceValue = car.price;              
                const discountPerUnit = originalPriceValue - unitPriceValue;

                // cap nhat gia
                function updateCalculation() {
                    const qty = parseInt(quantityInput.value) || 1;
                    if (qty < 1) { quantityInput.value = 1; return; }

                    const originalPriceRow = originalPriceEl.parentElement; 
                    const discountPriceRow = discountPriceEl.parentElement; 

                    if (discountPerUnit > 0) {
                    // neu ko giam gia thi hien tat ca
                    originalPriceRow.style.display = "flex";
                    discountPriceRow.style.display = "flex";

                    unitPriceEl.innerText = formatter.format(unitPriceValue) + ' ₫';
                    originalPriceEl.innerText = formatter.format(originalPriceValue) + ' ₫';
                    originalPriceEl.style.textDecoration = "line-through"; // Hiện gạch ngang

                    const totalDiscount = discountPerUnit * qty;
                    discountPriceEl.innerText = "- " + formatter.format(totalDiscount) + " ₫";
                    } else {
                        // ẩn giá gốc và giảm giá
                        originalPriceRow.style.display = "none";
                        discountPriceRow.style.display = "none";

                        unitPriceEl.innerText = formatter.format(unitPriceValue) + ' ₫';
                    }

                     // hien thi tong thanh toan
                    const totalFinal = unitPriceValue * qty;
                    finalPriceTotalEl.innerHTML = `<b>${formatter.format(totalFinal)} ₫</b>`;
                }

                // Khởi tạo ban đầu
                updateCalculation();

                // Lắng nghe sự kiện đổi số lượng
                quantityInput.addEventListener("input", updateCalculation);
            })
            .catch(err => console.error("Lỗi tải dữ liệu xe:", err));
    }

    // 3. AUTOCOMPLETE ĐỊA CHỈ 
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
                                fetch(`https://rsapi.goong.io/Place/Detail?api_key=${GOONG_API_KEY}&place_id=${item.place_id}`)
                                    .then(res => res.json())
                                    .then(detail => {
                                        const res = detail.result;
                                        const loc = res.geometry.location;
                                        mapContainer.style.display = 'block';
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
    confirmBtn?.addEventListener("click", () => {
        const address = streetInput.value.trim();
        const ward = wardInput.value.trim();
        const city = cityInput.value.trim();
        const phone = phoneInput.value.trim();

        if (!address || !ward || !city || !phone) {
            alert("Vui lòng điền đầy đủ địa chỉ và số điện thoại!");
            return;
        }

        const methodSelected = document.querySelector('input[name="paymentMethod"]:checked');
        const methodLabel = methodSelected.parentElement.innerText.trim();

        // Gán dữ liệu vào Modal Bill
        document.getElementById("bill-price").innerText = finalPriceTotalEl.innerText;
        document.getElementById("bill-address").innerText = `${address}, ${ward}, ${city}`;
        document.getElementById("bill-phone").innerText = phone;
        document.getElementById("bill-method").innerText = methodLabel;

        qrBox.style.display = methodSelected.value === "BANK" ? "block" : "none";
        overlay.style.display = "block";
        modal.style.display = "block";
    });

    // 5. GỬI ĐƠN HÀNG LÊN SERVER
    submitBtn?.addEventListener("click", () => {
        const data = {
            carId: carId,
            street: streetInput.value.trim(),
            ward: wardInput.value.trim(),
            city: cityInput.value.trim(),
            phone: phoneInput.value.trim(),
            paymentMethod: document.querySelector('input[name="paymentMethod"]:checked').value,
            quantity: parseInt(quantityInput.value)
        };

        fetch("/api/orders", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        })
        .then(res => {
            if (!res.ok) throw new Error("Thất bại");
            return res.json();
        })
        .then(() => {
            alert("Gửi yêu cầu mua xe thành công!");
            closeModal();
            window.location.href = "/index"; // Chuyển trang khi xong
        })
        .catch(err => alert("Có lỗi xảy ra, vui lòng thử lại!"));
    });

    function closeModal() {
        modal.style.display = "none";
        overlay.style.display = "none";
    }
    closeBtn?.addEventListener("click", closeModal);
    overlay?.addEventListener("click", closeModal);
});
