document.addEventListener("DOMContentLoaded", () => {
    // --- CẤU HÌNH KEY GOONG ---
    const GOONG_MAP_KEY = 'NAuy0xKrg4BYVnfEnD6OKD3GS8hJ1prrWtm1naq8';
    const GOONG_API_KEY = '7wYgWYbRnGZRFPMBG6blGJQRLN7Mz5eyE3apnifh';

    //  BANK INFO
    const BANK_INFO = {
        bank: "BIDV",
        account: "8860036029",
        name: "Dương Đức Thành"
    };

    const params = new URLSearchParams(window.location.search);
    const carId = params.get("id");

    // Elements địa chỉ
    const streetInput = document.getElementById('street-input');
    const wardInput = document.getElementById('ward-input');
    const cityInput = document.getElementById('city-input');
    const phoneInput = document.getElementById('phone-input');
    const suggestionsBox = document.getElementById('address-suggestions');
    const mapContainer = document.getElementById('map-container');

    // Elements đơn hàng
    const quantityInput = document.getElementById("quantity-input");
    const unitPriceEl = document.getElementById("unit-price");
    const originalPriceEl = document.getElementById("original-price");
    const discountPriceEl = document.getElementById("discount-price");
    const finalPriceTotalEl = document.getElementById("finalPrice");

    // Elements Modal BILL
    const confirmBtn = document.getElementById("confirm-payment-btn");
    const modal = document.getElementById("bill-modal");
    const overlay = document.getElementById("bill-overlay");
    const closeBtn = document.getElementById("close-bill");
    const qrBox = document.getElementById("qr-box");
    const submitBtn = document.getElementById("submit-order-btn");

    const depositModal = document.getElementById("deposit-modal");
    const depositOverlay = document.getElementById("deposit-overlay");
    const confirmDepositBtn = document.getElementById("confirm-deposit-btn");
    const closeDepositBtn = document.getElementById("close-deposit");

    const formatter = new Intl.NumberFormat('vi-VN');

    //  MAP 
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

    marker.on('dragend', function () {
        const lngLat = marker.getLngLat();
        fetch(`https://rsapi.goong.io/Geocode?latlng=${lngLat.lat},${lngLat.lng}&api_key=${GOONG_API_KEY}`)
            .then(res => res.json())
            .then(data => {
                if (data.results && data.results.length > 0) {
                    streetInput.value = data.results[0].formatted_address;
                }
            });
    });

    // LOAD XE 
    if (carId) {
        fetch(`/api/cars/${carId}`)
            .then(res => res.json())
            .then(data => {
                const car = data.car;
                if (!car) return;

                const carNameEl = document.getElementById("car-name");
                if (carNameEl) {
                    carNameEl.innerText = car.model;
                }

                const unitPriceValue = car.finalPrice || car.price;
                const originalPriceValue = car.price;
                const discountPerUnit = originalPriceValue - unitPriceValue;

                function updateCalculation() {
                    const qty = parseInt(quantityInput.value) || 1;
                    if (qty < 1) { quantityInput.value = 1; return; }

                    const originalPriceRow = originalPriceEl.parentElement;
                    const discountPriceRow = discountPriceEl.parentElement;

                    if (discountPerUnit > 0) {
                        originalPriceRow.style.display = "flex";
                        discountPriceRow.style.display = "flex";

                        unitPriceEl.innerText = formatter.format(unitPriceValue) + ' ₫';
                        originalPriceEl.innerText = formatter.format(originalPriceValue) + ' ₫';
                        originalPriceEl.style.textDecoration = "line-through";

                        const totalDiscount = discountPerUnit * qty;
                        discountPriceEl.innerText = "- " + formatter.format(totalDiscount) + " ₫";
                    } else {
                        originalPriceRow.style.display = "none";
                        discountPriceRow.style.display = "none";

                        unitPriceEl.innerText = formatter.format(unitPriceValue) + ' ₫';
                    }

                    const totalFinal = unitPriceValue * qty;
                    finalPriceTotalEl.innerHTML = `<b>${formatter.format(totalFinal)} ₫</b>`;
                }

                updateCalculation();
                quantityInput.addEventListener("input", updateCalculation);
            })
            .catch(err => console.error("Lỗi tải dữ liệu xe:", err));
    }

    //  AUTOCOMPLETE 
    let searchTimeout = null;
    streetInput.addEventListener('input', function () {
        clearTimeout(searchTimeout);
        const query = this.value.trim();
        if (query.length < 2) { suggestionsBox.innerHTML = ''; return; }

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

    //  CLICK THANH TOÁN 
    confirmBtn?.addEventListener("click", () => {
        const address = streetInput.value.trim();
        const ward = wardInput.value.trim();
        const city = cityInput.value.trim();
        const phone = phoneInput.value.trim();

        if (!address || !ward || !city || !phone) {
            showToast("Vui lòng điền thông tin và số điện thoại để chúng tôi có thể liên lạc với bạn ");
            return;
        }

        const methodSelected = document.querySelector('input[name="paymentMethod"]:checked');
        if (!methodSelected) {
            alert("Vui lòng chọn phương thức thanh toán");
            return;
        }

        if (methodSelected.value === "CASH") {
            const total = parseInt(finalPriceTotalEl.textContent.replace(/[^\d]/g, ""));
            const depositAmount = Math.floor(total * 0.1);

            const username = document.getElementById("username")?.value || "guest";
            const carName = document.querySelector("#car-name")?.innerText || "CarShop";

            const content = `${username} - Dat coc - ${carName}`;

            const qrUrl =
                `https://img.vietqr.io/image/${BANK_INFO.bank}-${BANK_INFO.account}-compact.png` +
                `?amount=${depositAmount}&addInfo=${encodeURIComponent(content)}`;

            document.getElementById("qr-image").src = qrUrl;

            document.getElementById("deposit-info").innerText =
                `${BANK_INFO.bank} - ${BANK_INFO.account} - ${BANK_INFO.name} | ${content}`;

            depositOverlay.style.display = "block";
            depositModal.style.display = "block";
            return;
        }

        showBill();
    });

    // SHOW BILL 
    function showBill() {
        const address = streetInput.value.trim();
        const ward = wardInput.value.trim();
        const city = cityInput.value.trim();
        const phone = phoneInput.value.trim();

        const methodSelected = document.querySelector('input[name="paymentMethod"]:checked');
        const methodLabel = methodSelected.parentElement.innerText.trim();

        document.getElementById("bill-price").innerText = finalPriceTotalEl.innerText;
        document.getElementById("bill-address").innerText = `${address}, ${ward}, ${city}`;
        document.getElementById("bill-phone").innerText = phone;
        document.getElementById("bill-method").innerText = methodLabel;

        const username = document.getElementById("username")?.value || "guest";
        const carName = document.querySelector("#car-name")?.innerText || "CarShop";
        const amount = finalPriceTotalEl.textContent.replace(/[^\d]/g, "");

        const typeText = methodSelected.value === "BANK" ? "Chuyen khoan" : "Dat coc";
        const content = `${username} - ${typeText} - ${carName}`;

        const qrUrl =
            `https://img.vietqr.io/image/BIDV-8860036029-compact.png` +
            `?amount=${amount}&addInfo=${encodeURIComponent(content)}`;

        const qrContainer = document.getElementById("qr-image-container");
        const qrInfo = document.getElementById("qr-info");

        qrContainer.innerHTML = `<img src="${qrUrl}" width="180" style="border:1px solid #ddd; padding:5px;">`;
        qrInfo.innerText = `${BANK_INFO.bank} - ${BANK_INFO.account} - ${BANK_INFO.name}`;

        qrBox.style.display = methodSelected.value === "BANK" ? "block" : "none";

        overlay.style.display = "block";
        modal.style.display = "block";
    }

    // EVENTS 
    confirmDepositBtn?.addEventListener("click", () => {
        depositModal.style.display = "none";
        depositOverlay.style.display = "none";
        showBill();
    });

    closeDepositBtn?.addEventListener("click", () => {
        depositModal.style.display = "none";
        depositOverlay.style.display = "none";
    });

    depositOverlay?.addEventListener("click", () => {
        depositModal.style.display = "none";
        depositOverlay.style.display = "none";
    });

    submitBtn?.addEventListener("click", () => {
    const methodSelected = document.querySelector('input[name="paymentMethod"]:checked');

    if (!methodSelected) {
        alert("Vui lòng chọn phương thức thanh toán");
        return;
    }

    const total = parseInt(finalPriceTotalEl.textContent.replace(/[^\d]/g, ""));

    const data = {
        carId: parseInt(carId),
        street: streetInput.value.trim(),
        ward: wardInput.value.trim(),
        city: cityInput.value.trim(),
        phone: phoneInput.value.trim(),
        paymentMethod: methodSelected.value,
        quantity: parseInt(quantityInput.value),

        totalAmountFinal: total,
        totalAmountOriginal: total,
        pricePaid: total,
        discount: 0
    };

    console.log("DATA SEND:", data); // 👈 bật lại dòng này để debug

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
                showToast("Gửi yêu cầu mua xe thành công!");
                closeModal();
                window.location.href = "/index";
            })
            .catch(() => alert("Có lỗi xảy ra, vui lòng thử lại!"));
    });

    function closeModal() {
        modal.style.display = "none";
        overlay.style.display = "none";
    }

    closeBtn?.addEventListener("click", closeModal);
    overlay?.addEventListener("click", closeModal);
});