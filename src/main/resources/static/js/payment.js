// ================= LOAD GIÁ XE =================
let currentCar = null;

document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const carId = params.get("id");

    if (!carId) return;

    fetch(`/api/cars/${carId}`)
        .then(res => res.json())
        .then(data => {
            currentCar = data.car;
            if (!currentCar) return;
            updatePriceDisplay();
        })
        .catch(err => console.error("Lỗi load giá xe:", err));
});

// ================= TÍNH GIÁ THEO SỐ LƯỢNG =================
function getQuantity() {
    return parseInt(document.getElementById("quantity-input").value) || 1;
}

function updatePriceDisplay() {
    if (!currentCar) return;
    const formatter = new Intl.NumberFormat('vi-VN');
    const quantity = getQuantity();
    const priceOriginal = currentCar.price;
    const discountPercent = currentCar.discount || 0;           // ví dụ: 5 (%)
    const discountAmount = priceOriginal * (discountPercent / 100); // tiền giảm 1 xe
    const unitPrice = priceOriginal - discountAmount;            // giá sau giảm 1 xe
    const totalOriginal = priceOriginal * quantity;
    const totalDiscount = discountAmount * quantity;
    const totalFinal = unitPrice * quantity;

    document.getElementById("unit-price").innerText = formatter.format(unitPrice) + ' ₫';
    document.getElementById("original-price").innerText = formatter.format(totalOriginal) + ' ₫';
    document.getElementById("discount-price").innerText =
        discountPercent + '% (-' + formatter.format(totalDiscount) + ' ₫)';
    document.getElementById("finalPrice").innerText = formatter.format(totalFinal) + ' ₫';
}

document.addEventListener("DOMContentLoaded", () => {
    const decreaseBtn = document.getElementById("decrease-btn");
    const increaseBtn = document.getElementById("increase-btn");
    const quantityInput = document.getElementById("quantity-input");

    decreaseBtn?.addEventListener("click", () => {
        let val = parseInt(quantityInput.value) || 1;
        if (val > 1) {
            quantityInput.value = val - 1;
            updatePriceDisplay();
        }
    });

    increaseBtn?.addEventListener("click", () => {
        let val = parseInt(quantityInput.value) || 1;
        quantityInput.value = val + 1;
        updatePriceDisplay();
    });

    quantityInput?.addEventListener("input", () => {
        if (parseInt(quantityInput.value) < 1 || !quantityInput.value) {
            quantityInput.value = 1;
        }
        updatePriceDisplay();
    });
});


// ================= BILL MODAL LOGIC =================
document.addEventListener("DOMContentLoaded", () => {
    const confirmBtn = document.querySelector(".payment-btn");
    const modal = document.getElementById("bill-modal");
    const overlay = document.getElementById("bill-overlay");
    const closeBtn = document.getElementById("close-bill");
    const qrBox = document.getElementById("qr-box");
    const submitBtn = document.getElementById("submit-order-btn");

    if (!confirmBtn) return;

    // ====== MỞ BILL ======
    confirmBtn.addEventListener("click", () => {
        const address = document.querySelector('input[name="street"]')?.value.trim();
        const ward = document.querySelector('input[name="ward"]')?.value.trim();
        const city = document.querySelector('input[name="city"]')?.value.trim();
        const phone = document.querySelector('input[name="phone"]')?.value.trim();

        if (!address || !ward || !city || !phone) {
            alert("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        const methodLabel =
            document.querySelector('input[name="paymentMethod"]:checked')
                .parentElement.innerText.trim();

        document.getElementById("bill-quantity").innerText = getQuantity();
        document.getElementById("bill-original-price").innerText =
            document.getElementById("original-price").innerText;
        document.getElementById("bill-discount").innerText =
            document.getElementById("discount-price").innerText;
        document.getElementById("bill-price").innerText =
            document.getElementById("finalPrice").innerText;
        document.getElementById("bill-address").innerText = `${address}, ${ward}, ${city}`;
        document.getElementById("bill-phone").innerText = phone;
        document.getElementById("bill-method").innerText = methodLabel;

        qrBox.style.display = methodLabel.includes("Chuyển khoản") ? "block" : "none";

        overlay.style.display = "block";
        modal.style.display = "block";
    });

    // ====== GỬI YÊU CẦU (POST BE) ======
    submitBtn?.addEventListener("click", () => {
        const params = new URLSearchParams(window.location.search);
        const carId = params.get("id");

        const address = document.querySelector('input[name="street"]').value.trim();
        const ward = document.querySelector('input[name="ward"]').value.trim();
        const city = document.querySelector('input[name="city"]').value.trim();
        const phone = document.querySelector('input[name="phone"]').value.trim();
        const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;
        const quantity = getQuantity();

        const priceOriginal = currentCar?.price || 0;
        const discountPercent = currentCar?.discount || 0;
        const discountAmount = priceOriginal * (discountPercent / 100);
        const unitPrice = priceOriginal - discountAmount;

        const data = {
            carId: carId,
            street: address,
            ward: ward,
            city: city,
            deliveryAddress: `${address}, ${ward}, ${city}`,
            phone: phone,
            paymentMethod: paymentMethod,
            quantity: quantity,
            priceOriginal: priceOriginal,
            pricePaid: unitPrice,
            discount: discountAmount,
            totalAmountOriginal: priceOriginal * quantity,
            totalAmountFinal: unitPrice * quantity,
            totalDiscount: discountAmount * quantity
        };

        fetch("/api/orders", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        })
            .then(res => {
                if (!res.ok) {
                    return res.text().then(errMsg => {
                        throw new Error(errMsg);
                    });
                }
                return res.json();
            })
            .then(() => {
                alert("Gửi yêu cầu mua xe thành công!");
                closeModal();
            })
            .catch(err => {
                console.error(err);
                alert("Lỗi: " + err.message);
            });
    });

    closeBtn.addEventListener("click", closeModal);
    overlay.addEventListener("click", closeModal);

    function closeModal() {
        modal.style.display = "none";
        overlay.style.display = "none";
    }
});