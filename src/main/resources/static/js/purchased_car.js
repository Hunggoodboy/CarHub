let currentTabStatus = 'PENDING';
let isViewingOrders = false;
let currentCarId = null;

document.addEventListener("DOMContentLoaded", function () {
    loadPurchasedCars();
    loadSellingCars();
    loadUserProfile();
    loadSellerOrders();

    setInterval(loadSellerOrders, 5000);

    showSection("profile");
    const firstMenu = document.querySelector('.sidebar ul li');
    if (firstMenu) firstMenu.classList.add('active');
    document.getElementById("edit-btn")?.addEventListener("click",openEditProfile);
});

function handleMenuClick(el, section) {
    document.querySelectorAll('.sidebar ul li').forEach(li => {
        li.classList.remove('active');
    });

    el.classList.add('active');

    if (section === 'warranty') {
        showWarranty();
    } else if (section === 'warrantySeller') {
        showWarrantySeller();
    } else {
        showSection(section);
    }
}

function handleTabClick(btn, status) {
    btn.parentElement.querySelectorAll('button').forEach(b => {
        b.classList.remove('active');
    });

    btn.classList.add('active');

    if (document.getElementById("orders-section").style.display === "block") {
        loadOrders(status);
    } else {
        loadPurchasedCars(status);
    }
}

function showSection(section) {
    document.getElementById("profile-section").style.display = "none";
    document.getElementById("purchased-section").style.display = "none";
    document.getElementById("selling-section").style.display = "none";
    document.getElementById("orders-section").style.display = "none";
    document.getElementById("warranty-section").style.display="none";
    document.getElementById("warranty-seller-section").style.display="none";

    const target = document.getElementById(section + "-section");
    if (target) target.style.display = "block";

    const badge = document.getElementById("order-badge");
    if (section === 'orders') {
        isViewingOrders = true;
        if (badge) badge.style.display = "none";
        loadOrders(currentTabStatus);
    } else {
        isViewingOrders = false;
        loadSellerOrders();
    }
}

function loadSellerOrders() {
    fetch("/api/orders/seller/orders?status=PENDING")
        .then(res => res.json())
        .then(data => {
            const badge = document.getElementById("order-badge");
            if (!badge) return;

            if (Array.isArray(data) && data.length > 0 && !isViewingOrders) {
                badge.style.display = "inline-block";
                badge.innerText = data.length;
            } else {
                badge.style.display = "none";
            }

            if (isViewingOrders && currentTabStatus === 'PENDING') {
                renderSellerOrders(data);
            }
        })
        .catch(err => console.error("Lỗi badge:", err));

    if (isViewingOrders && currentTabStatus !== 'PENDING') {
        fetch(`/api/orders/seller/orders?status=${currentTabStatus}`)
            .then(res => res.json())
            .then(data => renderSellerOrders(data))
            .catch(err => console.error(err));
    }
}

function loadOrders(status) {
    currentTabStatus = status;
    fetch(`/api/orders/seller/orders?status=${status}`)
        .then(res => res.json())
        .then(data => renderSellerOrders(data))
        .catch(err => console.error(err));
}

function renderSellerOrders(orders) {
    const container = document.getElementById("order-list");
    if (!container) return;

    if (!Array.isArray(orders) || orders.length === 0) {
        container.innerHTML = "<p>Không có đơn hàng nào trong mục này</p>";
        return;
    }

    const formatter = new Intl.NumberFormat("vi-VN");
    let html = "";

    orders.forEach(o => {
        let statusClass = "";
        if(o.status === "PENDING") statusClass = "status-pending";
        if(o.status === "DELIVERING") statusClass = "status-delivering";
        if(o.status === "DELIVERED") statusClass = "status-delivered";
        if(o.status === "COMPLETED") statusClass = "status-completed";

        const imgPath = o.imageUrl 
            ? `/${o.imageUrl.replace("car_images", "car-images")}` 
            : "/images/default-car.png";

        let actionButtons = "";

        if (o.status === 'PENDING') {
            actionButtons = `
                <button onclick="updateOrderStatus(${o.orderId}, 'DELIVERING')">🚚 Nhận cọc & giao hàng</button>
                <button onclick="updateOrderStatus(${o.orderId}, 'CANCELLED')">❌ Hủy</button>
            `;
        } else if (o.status === 'DELIVERING') {
            actionButtons = `
                <button onclick="updateOrderStatus(${o.orderId}, 'DELIVERED')">📦 Đã giao</button>
            `;
        } else if (o.status === 'DELIVERED') {
            actionButtons = `
                <button onclick="confirmSeller(${o.orderId})">✔ Xác nhận đã giao</button>
            `;
        }

        html += `
            <div class="order-card">
                <img src="${imgPath}" class="order-img">
                <h4>🚗 ${o.carName}</h4>
                <p><b>Giá:</b> ${formatter.format(o.price)} ₫</p>
                <p><b>Người mua:</b> ${o.buyerName}</p>
                <p><b>Trạng thái:</b> 
                    <span class="status ${statusClass}">${o.status}</span>
                </p>
                <div class="action-area">
                    ${actionButtons}
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

function updateOrderStatus(orderId, status) {
    fetch(`/api/orders/${orderId}/status?status=${status}`, {
        method: "PUT"
    })
    .then(res => {
        if (res.ok) {
            showToast("Cập nhật thành công");
            loadOrders(currentTabStatus);
            loadSellerOrders();
        }
    })
    .catch(err => console.error(err));
}

function confirmSeller(id) {
    fetch(`/api/orders/${id}/confirm-seller`, {
        method: "PUT"
    })
    .then(() => {
        showToast("Đã xác nhận!");
        loadOrders(currentTabStatus);
    });
}

function loadUserProfile() {
    fetch("/api/users/me/profile")
        .then(res => res.json())
        .then(data => {
            document.getElementById("username").innerText = data.username || "";
            document.getElementById("email").innerText = data.email || "";
            document.getElementById("phone").innerText = data.phoneNumber || "";
            const avatarImg = document.getElementById("avatar-img");
            if(data.avatar){
                avatarImg.src = "/uploads/" + data.avatar;
            }else {
                avatarImg.src = "/img/default-avatar.png";
            }
        })
        .catch(err => console.error(err));
}

function loadPurchasedCars(status = "PENDING") {
    fetch(`/api/orders/purchased?status=${status}`)
        .then(res => res.json())
        .then(data => renderPurchasedCars(data))
        .catch(err => console.error(err));
}

function renderPurchasedCars(cars) {
    const container = document.getElementById("purchased-car-list");
    if (!container) return;

    if (!Array.isArray(cars) || cars.length === 0) {
        container.innerHTML = "<p>Bạn chưa mua xe nào</p>";
        return;
    }

    const formatter = new Intl.NumberFormat("vi-VN");
    let html = "";

    cars.forEach(car => {
        const imgPath = car.imageUrl 
            ? `/${car.imageUrl.replace("car_images", "car-images")}` 
            : "/images/default-car.png";

        let statusLabel = "";
        if (car.status === "PENDING") statusLabel = `<span class="status pending">Đã đặt</span>`;
        else if (car.status === "DELIVERING") statusLabel = `<span class="status delivering">Đang giao</span>`;
        else if (car.status === "COMPLETED") statusLabel = `<span class="status completed">Đã nhận</span>`;

        let button = "";
        if (car.status === "DELIVERED"){
            button = `<button onclick="confirmBuyerOrder(${car.orderId})">Xác nhận đã nhận xe</button>`;
        } else if (car.status === "COMPLETED"){
            button = `<a href="/api/warranty/request?carId=${car.carId}">Yêu cầu bảo hành</a>`;
        }

        html += `
        <div class="card">
            <img src="${imgPath}">
            <div class="info">
                <h4>${car.model}</h4>
                <p class="price">${formatter.format(car.price)} ₫</p>
                <p>${statusLabel}</p>
                ${button}
            </div>
        </div>`;
    });

    container.innerHTML = html;
}

function confirmBuyerOrder(orderId) {
    if (!confirm("Bạn xác nhận đã nhận được xe?")) return;

    fetch(`/api/orders/${orderId}/confirm-buyer`, {
        method: "PUT"
    })
    .then(res => {
        if (res.ok) {
            showToast("Xác nhận thành công!");
            loadPurchasedCars('DELIVERED'); 
        }
    });
}

function loadSellingCars() {
    fetch("/api/cars/car-pass")
        .then(res => res.json())
        .then(data => {
            const loading = document.getElementById("loading-selling");
            if (loading) loading.style.display = "none";
            renderSellingCars(data);
        })
        .catch(err => console.error(err));
}

function renderSellingCars(cars) {
    const container = document.getElementById("passed-car-list");
    if (!container) return;

    if (!Array.isArray(cars) || cars.length == 0) {
        container.innerHTML = "<p>Bạn chưa bán xe nào cả</p>";
        return;
    }

    const formatter = new Intl.NumberFormat("vi-VN");
    let html = "";

    cars.forEach(car => {
        const imgPath = car.imageUrl 
            ? `/${car.imageUrl.replace("car_images", "car-images")}` 
            : "/images/default-car.png";

        html += `
            <div class="card">
                <img src="${imgPath}">
                <div class="info">
                    <h4>${car.model}</h4>
                    <p class="price">${formatter.format(car.price)} đ</p>
                    <button onclick="openDetail(${car.id})">Xem chi tiết</button>
                </div>
            </div>`;
    });

    container.innerHTML = html;
}
function openDetail(carId) {
    currentCarId = carId;
    fetch(`/api/cars/${carId}`)
        .then(res => res.json())
        .then(data => {
            const car = data.car;
            const imgPath = car.imageUrl 
                ? `/${car.imageUrl.replace("car_images", "car-images")}` 
                : "/images/default-car.png";

            document.getElementById("detail-image").src = imgPath;
            document.getElementById("detail-model").innerText = car.model;
            document.getElementById("detail-price").innerText = car.price;
            document.getElementById("detail-color").innerText = car.color;
            document.getElementById("detail-year").innerText = car.manufactureYear;
            document.getElementById("detail-description").innerText = car.description;
            document.getElementById("detail-modal").style.display = "flex";
        });
}

function closeDetail() {
    document.getElementById("detail-modal").style.display = "none";
}

function openEditFromDetail() {
    closeDetail();
    openEditForm(currentCarId);
}

function openEditForm(carId) {
    currentCarId = carId;
    fetch(`/api/cars/${carId}`)
        .then(res => res.json())
        .then(data => {
            const car = data.car;
            document.getElementById("edit-model").value = car.model;
            document.getElementById("edit-price").value = car.price;
            document.getElementById("edit-color").value = car.color;
            document.getElementById("edit-year").value = car.manufactureYear;
            document.getElementById("edit-description").value = car.description;
            document.getElementById("edit-modal").style.display = "flex";
        });
}

function saveEdit() {
    const data = {
        model: document.getElementById("edit-model").value,
        price: parseFloat(document.getElementById("edit-price").value),
        color: document.getElementById("edit-color").value,
        manufactureYear: parseInt(document.getElementById("edit-year").value),
        description: document.getElementById("edit-description").value
    };

    fetch(`/api/cars/${currentCarId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
    .then(res => {
        if (res.ok) {
            showToast("Cập nhật thành công");
            closeEdit();
            loadSellingCars();
        }
    });
}

function closeEdit() {
    document.getElementById("edit-modal").style.display = "none";
}

function showWarranty() {
    showSection("warranty");
    loadWarranty();
}

function showWarrantySeller() {
    showSection("warranty-seller");
    loadWarrantySeller();
}

function loadWarranty() {
    fetch("/api/warranty/my")
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById("warranty-list");
            container.innerHTML = "";

            if (!Array.isArray(data)) return;

            data.forEach(w => {
                const div = document.createElement("div");
                div.className = "warranty-card";

                let button = "";

                if (!w.customerConfirmed && w.status === "SUCCESS") {
                    button = `<button onclick="confirmCustomerWarranty(${w.id})">Xác nhận nhận xe</button>`;
                }

                div.innerHTML = `
                    <h3>${w.carModel}</h3>
                    <p>Lỗi: ${w.defectDescription}</p>
                    <p>Ngày gửi: ${new Date(w.receivedDate).toLocaleDateString()}</p>
                    <p>Trạng thái: ${w.status}</p>
                    ${button}
                `;

                container.appendChild(div);
            });
        });
}

function loadWarrantySeller() {
    fetch("/api/warranty/seller")
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById("warranty-seller-list");
            container.innerHTML = "";

            if (!Array.isArray(data)) return;

            data.forEach(w => {
                const div = document.createElement("div");
                div.className = "warranty-card";

                let button = "";

                if (!w.sellerConfirmed && w.status !== "COMPLETED") {
                    button = `<button onclick="confirmSellerWarranty(${w.id})">Xác nhận bảo hành</button>`;
                }

                div.innerHTML = `
                    <h3>${w.carModel}</h3>
                    <p>Khách: ${w.customerName}</p>
                    <p>Lỗi: ${w.defectDescription}</p>
                    <p>SĐT: ${w.phone}</p>
                    <p>Địa chỉ: ${w.street}, ${w.ward}, ${w.city}</p>
                    <p>Trạng thái: ${w.status}</p>
                    ${button}
                `;

                container.appendChild(div);
            });
        });
}

function confirmSellerWarranty(id) {
    fetch(`/api/warranty/${id}/confirm-seller`, {
        method: "PUT"
    })
    .then(() => {
        showToast("Đã xác nhận bảo hành!");
        loadWarrantySeller();
    });
}

function confirmCustomerWarranty(id) {
    fetch(`/api/warranty/${id}/confirm-customer`, {
        method: "PUT"
    })
    .then(() => {
        showToast("Hoàn tất bảo hành!");
        loadWarranty();
    });
}

const avatarInput = document.getElementById("avatar-input");
const avatarImg = document.getElementById("avatar-img");

avatarInput?.addEventListener("change", () => {
    const file = avatarInput.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    fetch("/api/users/upload-avatar", {
        method: "POST",
        body: formData
    })
    .then(res => {
        if (!res.ok) throw new Error("Upload fail");
        return res.text();
    })
    .then(fileName => {
        avatarImg.src = "/uploads/" + fileName;
        showToast("Upload avatar thành công");
    })
    .catch(() => {
        showToast("Upload thất bại");
    });
});

function openEditProfile() {
    fetch("/api/users/me/profile")
        .then(res => res.json())
        .then(data => {
            document.getElementById("edit-email").value = data.email || "";
            document.getElementById("edit-phone").value = data.phoneNumber || "";

            document.getElementById("edit-profile-modal").style.display = "flex";
        });
}
function saveProfile() {
    const data = {
        email: document.getElementById("edit-email").value,
        phoneNumber: document.getElementById("edit-phone").value
    };

    fetch("/api/users/me", {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(data)
    })
    .then(res => {
        if (res.ok) {
            showToast("Cập nhật thành công");
            closeEditProfile();
            loadUserProfile(); // reload lại info
        } else {
            showToast("Cập nhật thất bại");
        }
    })
    .catch(err => console.error(err));
}
function closeEditProfile() {
    document.getElementById("edit-profile-modal").style.display = "none";
}