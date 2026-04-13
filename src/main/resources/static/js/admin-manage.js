const toggleBtn = document.getElementById("toggleBtn");
const sidebar = document.getElementById("sidebar");
const main = document.getElementById("main");

const menuItems = document.querySelectorAll(".menu-item");
const sections = document.querySelectorAll(".section");

let allOrders = [];
let currentTab = "ALL";

toggleBtn.addEventListener("click", () => {
    sidebar.classList.toggle("hidden");
    main.classList.toggle("full");
});

menuItems.forEach(item => {
    item.addEventListener("click", () => {

        menuItems.forEach(i => i.classList.remove("active"));
        item.classList.add("active");

        sections.forEach(sec => sec.classList.remove("active-section"));

        const id = item.getAttribute("data-section");
        document.getElementById(id).classList.add("active-section");

        if (id === "orders") {
            loadAllOrders();
        }
    });
});

function loadAllOrders() {
    fetch("/admin/orders")
        .then(res => res.json())
        .then(data => {
            allOrders = data;
            applyFilter();
        })
        .catch(err => console.error("Lỗi load orders:", err));
}

function switchTab(event, tab) {
    currentTab = tab;

    document.querySelectorAll(".tab-btn").forEach(btn => btn.classList.remove("active"));
    event.target.classList.add("active");

    applyFilter();
}

function applyFilter() {
    let filtered = allOrders;

    if (currentTab !== "ALL") {
        filtered = allOrders.filter(o => o.paymentStatus === currentTab);
    }

    renderOrders(filtered);
}

function renderOrders(orders) {
    const container = document.getElementById("admin-order-list");
    if (!container) return;

    if (!orders || orders.length === 0) {
        container.innerHTML = "<p>Không có đơn hàng</p>";
        return;
    }

    let html = "";

    orders.forEach(o => {

        let statusClass = "";
        if (o.status === "PENDING") statusClass = "status-pending";
        else if (o.status === "DELIVERING") statusClass = "status-delivering";
        else if (o.status === "DELIVERED") statusClass = "status-delivered";
        else if (o.status === "COMPLETED") statusClass = "status-completed";
        else if (o.status === "CANCELLED") statusClass = "status-cancelled";

        let actionBtn = "";
        if (o.status === "PENDING") {
            actionBtn = `
                <button class="confirm-btn" onclick="confirmOrder(${o.id})">
                    ✔ Xác nhận thanh toán
                </button>
            `;
        }

        html += `
            <div class="order-box">
                <h3>Đơn hàng #${o.id}</h3>
                <table class="order-detail-table">
                    <tr>
                        <th>Khách</th>
                        <td>${o.username}</td>
                    </tr>
                    <tr>
                        <th>Xe</th>
                        <td>${o.carName}</td>
                    </tr>
                    <tr>
                        <th>Trạng thái</th>
                        <td><span class="${statusClass}">${o.status}</span></td>
                    </tr>
                    <tr>
                        <th>Thanh toán</th>
                        <td>${o.paymentStatus}</td>
                    </tr>
                    <tr>
                        <th>Hành động</th>
                        <td>${actionBtn}</td>
                    </tr>
                </table>
            </div>
        `;
    });

    container.innerHTML = html;
}

function confirmOrder(orderId) {
    if (!confirm("Xác nhận khách đã thanh toán?")) return;

    fetch(`/admin/orders/${orderId}/confirm`, {
        method: "PUT"
    })
    .then(res => {
        if (res.ok) {
            alert("Đã xác nhận!");
            loadAllOrders();
        } else {
            alert("Lỗi!");
        }
    })
    .catch(err => console.error(err));
}