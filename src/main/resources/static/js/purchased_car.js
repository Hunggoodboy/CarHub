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
            showToast("Cập nhật avatar thành công");
        })
        .catch(() => {
            showToast("Cập nhật thất bại");
        });
    });

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
    document.getElementById("revenue-section").style.display = "none";
    document.getElementById("orders-section").style.display = "none";
    document.getElementById("customer-request-section").style.display = "none";
    document.getElementById("warranty-section").style.display = "none";
    document.getElementById("warranty-seller-section").style.display = "none";

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

    if (section === 'customer-request') {
        const requestBadge = document.getElementById("customer-request-badge");
        if (requestBadge) requestBadge.style.display = "none";
    }

    if (section === 'revenue' && typeof loadRevenueData === 'function') {
        loadRevenueData();
    }
}

function loadRevenueData() {
    fetch('/api/orders/seller/orders?status=COMPLETED')
        .then(res => res.json())
        .then(data => renderRevenueData(data))
        .catch(err => {
            console.error('Lỗi tải doanh thu:', err);
            renderRevenueData([]);
        });
}

function renderRevenueData(orders) {
    const safeOrders = Array.isArray(orders) ? orders : [];
    const formatter = new Intl.NumberFormat('vi-VN');

    const totalRevenue = safeOrders.reduce((sum, item) => {
        const price = Number(item.price || 0);
        const qty = Number(item.quantity || 1);
        return sum + (price * qty);
    }, 0);

    const totalOrders = safeOrders.length;
    const totalQuantity = safeOrders.reduce((sum, item) => sum + Number(item.quantity || 1), 0);
    const averageRevenue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

    const totalEl = document.getElementById('revenue-total');
    const ordersEl = document.getElementById('revenue-orders');
    const avgEl = document.getElementById('revenue-average');
    const qtyEl = document.getElementById('revenue-quantity');

    if (totalEl) totalEl.textContent = formatter.format(totalRevenue) + ' ₫';
    if (ordersEl) ordersEl.textContent = String(totalOrders);
    if (avgEl) avgEl.textContent = formatter.format(Math.round(averageRevenue)) + ' ₫';
    if (qtyEl) qtyEl.textContent = String(totalQuantity);

    renderTopCarsByRevenue(safeOrders, formatter);
    renderRevenueOrderList(safeOrders, formatter);
}

function renderTopCarsByRevenue(orders, formatter) {
    const container = document.getElementById('revenue-top-cars');
    if (!container) return;

    if (!orders.length) {
        container.innerHTML = '<h3>Top xe mang lại doanh thu</h3><p>Chưa có đơn hàng hoàn tất.</p>';
        return;
    }

    const grouped = {};
    orders.forEach(item => {
        const key = item.carName || 'Xe không tên';
        const amount = Number(item.price || 0) * Number(item.quantity || 1);

        if (!grouped[key]) {
            grouped[key] = { revenue: 0, quantity: 0 };
        }

        grouped[key].revenue += amount;
        grouped[key].quantity += Number(item.quantity || 1);
    });

    const topCars = Object.entries(grouped)
        .map(([name, val]) => ({ name, revenue: val.revenue, quantity: val.quantity }))
        .sort((a, b) => b.revenue - a.revenue)
        .slice(0, 5);

    const listHtml = topCars.map(car =>
        '<li><b>' + car.name + '</b> - ' + car.quantity + ' xe - ' + formatter.format(car.revenue) + ' ₫</li>'
    ).join('');

    container.innerHTML = '<h3>Top xe mang lại doanh thu</h3><ul>' + listHtml + '</ul>';
}

function renderRevenueOrderList(orders, formatter) {
    const container = document.getElementById('revenue-orders-list');
    if (!container) return;

    if (!orders.length) {
        container.innerHTML = '<h3>Đơn hàng hoàn tất gần đây</h3><p>Chưa có dữ liệu.</p>';
        return;
    }

    const sorted = [...orders]
        .sort((a, b) => Number(b.orderId || 0) - Number(a.orderId || 0))
        .slice(0, 8);

    const listHtml = sorted.map(item => {
        const qty = Number(item.quantity || 1);
        const lineTotal = Number(item.price || 0) * qty;
        return '<li>Đơn #' + item.orderId + ' - ' + (item.carName || 'Xe không tên') + ' - ' + qty + ' xe - ' + formatter.format(lineTotal) + ' ₫</li>';
    }).join('');

    container.innerHTML = '<h3>Đơn hàng hoàn tất gần đây</h3><ul>' + listHtml + '</ul>';
}

function showCustomerRequests() {
    showSection("customer-request");
    loadCustomerRequests();
}

function loadCustomerRequests() {
    fetch("/api/messages/recent")
        .then(res => res.json())
        .then(data => {
            renderCustomerRequests(data);

            const requestBadge = document.getElementById("customer-request-badge");
            if (!requestBadge) return;

            if (Array.isArray(data) && data.length > 0) {
                requestBadge.style.display = "inline-block";
                requestBadge.innerText = data.length;
            } else {
                requestBadge.style.display = "none";
            }
        })
        .catch(err => {
            console.error("Lỗi tải yêu cầu khách hàng:", err);
            renderCustomerRequests([]);
        });
}

function renderCustomerRequests(requests) {
    const container = document.getElementById("customer-request-list");
    if (!container) return;

    if (!Array.isArray(requests) || requests.length === 0) {
        container.innerHTML = "<p>Hiện chưa có yêu cầu nào từ khách hàng.</p>";
        return;
    }

    const html = requests.map(req => {
        const sentAt = req.sentAt
            ? new Date(req.sentAt).toLocaleString("vi-VN")
            : "Vừa xong";
        const partnerName = req.partnerName || "Khách hàng";
        const lastMessage = req.lastMessage || "(Không có nội dung)";

        return `
            <div class="customer-request-card">
                <div class="customer-request-card__top">
                    <h4>${partnerName}</h4>
                    <span>${sentAt}</span>
                </div>
                <p>${lastMessage}</p>
                <a href="/chat" class="btn">Mở cuộc trò chuyện</a>
            </div>
        `;
    }).join("");

    container.innerHTML = html;
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

    loadCustomerRequests();

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
        if (o.status === "PENDING") statusClass = "status-pending";
        if (o.status === "DELIVERING") statusClass = "status-delivering";
        if (o.status === "DELIVERED") statusClass = "status-delivered";
        if (o.status === "COMPLETED") statusClass = "status-completed";

        const imgPath = o.imageUrl
            ? `/${o.imageUrl.replace("car_images", "car-images")}`
            : "/images/default-car.png";

        let actionButtons = "";

        if (o.status === 'CONFIRMED') {
    actionButtons = `
        <button onclick="startDelivery(${o.orderId})">
            🚚 Bắt đầu giao
        </button>
    `;
}
else if (o.status === 'DELIVERING') {
    actionButtons = `
        <button onclick="updateOrderStatus(${o.orderId}, 'DELIVERED')">
            📦 Đã giao
        </button>
    `;
}
else if (o.status === 'DELIVERED') {
    actionButtons = `
        <button onclick="confirmSeller(${o.orderId})">
            ✔ Xác nhận đã giao
        </button>
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
            } else {
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
        if (car.status === "PENDING") {
            statusLabel = `<span class="status pending">Đã đặt</span>`;
        } else if (car.status === "DELIVERING") {
            statusLabel = `<span class="status delivering">Đang giao</span>`;
        } else if (car.status === "COMPLETED") {
            statusLabel = `<span class="status completed">Đã nhận</span>`;
        }

        let button = "";
        if (car.status === "DELIVERED") {
            button = `<button class="btn-confirm" onclick="confirmBuyerOrder(${car.orderId})">Xác nhận đã nhận xe</button>`;
        } else if (car.status === "COMPLETED") {
            button = `<a href="/api/warranty/request?carId=${car.carId}" class="btn">Yêu cầu bảo hành</a>`;
        }

        html += `
        <div class="card">
            <img src="${imgPath}" alt="${car.model}">
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
    if (!confirm("Bạn xác nhận đã nhận được xe và kiểm tra kỹ lưỡng?")) return;

    fetch(`/api/orders/${orderId}/confirm-buyer`, {
        method: "PUT"
    })
        .then(res => {
            if (res.ok) {
                showToast("Xác nhận thành công!");
                loadPurchasedCars('DELIVERED');
            } else {
                showToast("Có lỗi xảy ra khi xác nhận.");
            }
        })
        .catch(err => console.error("Lỗi confirm buyer:", err));
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
                <img src="${imgPath}" alt="${car.model}">
                <div class="info">
                    <h4>${car.model}</h4>
                    <p class="price">${formatter.format(car.price)} đ</p>
                    <button class="btn" onclick="openDetail(${car.id})">Xem chi tiết</button>
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
        })
        .catch(err => console.error(err));
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
        })
        .catch(err => console.error(err));
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

            data.forEach(w => {
                let actions = "";

                if (w.status === "PROCESSING" && !w.customerConfirmed) {
                    actions = `<button onclick="confirmCustomerWarranty(${w.id})">Xác nhận đã sửa</button>`;
                }

                const div = document.createElement("div");
                div.className = "warranty-card";

                div.innerHTML = `
                    <h3>${w.carModel}</h3>
                    <p>Lỗi: ${w.defectDescription}</p>
                    <p>Ngày gửi: ${new Date(w.receivedDate).toLocaleDateString()}</p>
                    <p>Trạng thái: ${w.status}</p>
                    ${actions}
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

            data.forEach(w => {
                let actions = "";

                if (w.status === "PENDING") {
                    actions = `<button onclick="acceptWarranty(${w.id})">Nhận</button>`;
                }
                else if (w.status === "PROCESSING") {
                    actions = `<button onclick="confirmSellerWarranty(${w.id})">Đã sửa xong</button>`;
                }

                const div = document.createElement("div");
                div.className = "warranty-card";

                div.innerHTML = `
                    <h3>${w.carModel}</h3>
                    <p>Khách: ${w.customerName}</p>
                    <p>Lỗi: ${w.defectDescription}</p>
                    <p>SĐT: ${w.phone}</p>
                    <p>Địa chỉ: ${w.street}, ${w.ward}, ${w.city}</p>
                    <p>Trạng thái: ${w.status}</p>
                    ${actions}
                `;

                container.appendChild(div);
            });
        });
}

function acceptWarranty(id) {
    fetch(`/api/warranty/${id}/accept`, {
        method: "PUT"
    })
    .then(() => {
        showToast("Đã nhận bảo hành");
        loadWarrantySeller();
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
function startDelivery(orderId) {
    fetch(`/api/orders/${orderId}/start-delivery`, {
        method: "PUT"
    })
    .then(() => {
        showToast("Bắt đầu giao hàng!");
        loadOrders(currentTabStatus);
    });
}