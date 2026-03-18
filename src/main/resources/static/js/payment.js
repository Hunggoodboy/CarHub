
document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const carId = params.get("id");

    if (!carId) return;

    fetch(`/api/cars/${carId}`)
        .then(res => res.json())
        .then(data => {
            const car = data.car;
            if (!car) return;

            const priceEl = document.getElementById("finalPrice");
            if (priceEl) {
                const price = car.finalPrice || car.price;
                priceEl.innerText =
                    new Intl.NumberFormat('vi-VN').format(price) + ' ₫';
            }
        })
        .catch(err => console.error("Lỗi load giá xe:", err));
});

document.addEventListener("DOMContentLoaded", () => {
    const confirmBtn = document.querySelector(".payment-btn");
    const modal = document.getElementById("bill-modal");
    const overlay = document.getElementById("bill-overlay");
    const closeBtn = document.getElementById("close-bill");
    const qrBox = document.getElementById("qr-box");
    const submitBtn = document.getElementById("submit-order-btn"); 

    if (!confirmBtn) return;

    confirmBtn.addEventListener("click", () => {
        const address = document.querySelector('input[placeholder*="Số nhà"]')?.value.trim();
        const ward = document.querySelector('input[placeholder*="xã"]')?.value.trim();
        const city = document.querySelector('input[placeholder*="tỉnh"]')?.value.trim();
        const phone = document.querySelector('input[placeholder*="Điện Thoại"]')?.value.trim();

        if (!address || !ward || !city || !phone) {
            alert("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        const methodLabel =
            document.querySelector('input[name="paymentMethod"]:checked')
                .parentElement.innerText.trim();

        document.getElementById("bill-price").innerText =
            document.getElementById("finalPrice").innerText;
        document.getElementById("bill-address").innerText =
            `${address}, ${ward}, ${city}`;
        document.getElementById("bill-phone").innerText = phone;
        document.getElementById("bill-method").innerText = methodLabel;

        qrBox.style.display = methodLabel.includes("Chuyển khoản")
            ? "block"
            : "none";

        overlay.style.display = "block";
        modal.style.display = "block";
    });

    
    submitBtn?.addEventListener("click", () => {
        const params = new URLSearchParams(window.location.search);
        const carId = params.get("id");

        const address = document.querySelector('input[placeholder*="Số nhà"]').value.trim();
        const ward = document.querySelector('input[placeholder*="xã"]').value.trim();
        const city = document.querySelector('input[placeholder*="tỉnh"]').value.trim();
        const phone = document.querySelector('input[placeholder*="Điện Thoại"]').value.trim();

        const paymentMethod =
            document.querySelector('input[name="paymentMethod"]:checked')
                .value;

        const data = {
            carId: carId,
            street: address,
            ward: ward,
            city: city,
            phone: phone,
            paymentMethod: paymentMethod
        };

        fetch("/api/orders", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
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

    closeBtn.addEventListener("click", closeModal);
    overlay.addEventListener("click", closeModal);

    function closeModal() {
        modal.style.display = "none";
        overlay.style.display = "none";
    }
});