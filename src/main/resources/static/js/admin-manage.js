document.addEventListener("DOMContentLoaded", function () {
    const tabs = document.querySelectorAll(".tab-btn");
    const pageTitle = document.getElementById("page-title");
    const searchSection = document.getElementById("search-section");
    const container = document.getElementById("table-container");
    const btnSearch = document.getElementById("btn-search");
    const searchInput = document.getElementById("searchInput");

    let currentTab = "users";
    
    // Support hash routing
    if (window.location.hash) {
        let hash = window.location.hash.substring(1);
        if (["users", "products", "orders"].includes(hash)) {
            currentTab = hash;
            // Update active class on setup
            tabs.forEach(t => t.classList.remove("active"));
            let activeTab = Array.from(tabs).find(t => t.getAttribute("data-target") === hash);
            if(activeTab) activeTab.classList.add("active");
        }
    }
    
    switchTab(currentTab);

    tabs.forEach(tab => {
        tab.addEventListener("click", function () {
            // Remove active classes
            tabs.forEach(t => t.classList.remove("active"));
            // Add active to clicked tab
            this.classList.add("active");

            currentTab = this.getAttribute("data-target");
            window.location.hash = currentTab;
            switchTab(currentTab);
        });
    });

    if (btnSearch && searchInput) {
        btnSearch.addEventListener("click", () => {
            loadData(currentTab, searchInput.value.trim());
        });
        
        searchInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                loadData(currentTab, searchInput.value.trim());
            }
        });
    }

    function switchTab(tabId) {
        if (searchInput) searchInput.value = ""; // Xóa keyword cũ

        container.innerHTML = `
            <div class="dash-empty">
                <i class="fa-solid fa-circle-notch fa-spin"></i>
                <p>Đang tải dữ liệu...</p>
            </div>
        `;
        
        let desc = document.getElementById("page-desc");

        if (tabId === "orders") {
            pageTitle.textContent = "Quản lý đơn hàng";
            if (desc) desc.textContent = "Danh sách đơn hàng trong hệ thống";
            if (searchSection) searchSection.style.display = "none";
            loadData("orders", "");
        } else if (tabId === "users") {
            pageTitle.textContent = "Quản lý người dùng";
            if (desc) desc.textContent = "Danh sách tài khoản hệ thống";
            if (searchSection) searchSection.style.display = "flex";
            if (searchInput) searchInput.placeholder = "Tìm theo tên, email, username...";
            loadData("users", "");
        } else if (tabId === "products") {
            if (desc) desc.textContent = "Danh sách xe đang bán";
            pageTitle.textContent = "Quản lý sản phẩm";
            if (searchSection) searchSection.style.display = "flex";
            if (searchInput) searchInput.placeholder = "Tìm theo tên xe hoặc model...";
            loadData("products", "");
        }
    }

    function loadData(tabId, keyword) {
        let url = "";
        if (tabId === "users") url = `/admin/api/users?keyword=${encodeURIComponent(keyword)}`;
        else if (tabId === "products") url = `/admin/api/cars?keyword=${encodeURIComponent(keyword)}`;
        else if (tabId === "orders") url = `/admin/orders`;

        fetch(url)
            .then(res => res.json())
            .then(data => {
                if (tabId === "users") renderUsers(data);
                else if (tabId === "products") renderCars(data);
                else if (tabId === "orders") renderOrders(data);
            })
            .catch(err => {
                console.error("Lỗi tải dữ liệu:", err);
                container.innerHTML = `
                    <div class="dash-empty" style="color: var(--brand-primary)">
                        <i class="fa-solid fa-triangle-exclamation"></i>
                        <p>Lỗi kết nối máy chủ</p>
                    </div>`;
            });
    }

    // ==========================================
    // RENDER NGƯỜI DÙNG
    // ==========================================
    function renderUsers(users) {
        if (!users || users.length === 0) {
            container.innerHTML = `
                <div class="dash-empty">
                    <i class="fa-solid fa-users-slash"></i>
                    <p>Không tìm thấy người dùng nào phù hợp.</p>
                </div>`;
            return;
        }

        let html = `
            <table class="dash-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Username</th>
                        <th>Họ tên</th>
                        <th>Email</th>
                        <th>Điện thoại</th>
                        <th>Vai trò</th>
                        <th>Thao tác</th>
                    </tr>
                </thead>
                <tbody>
        `;

        users.forEach(u => {
            let isCustomer = u.role && u.role.toUpperCase() === 'CUSTOMER';
            let badgeClass = isCustomer ? 'dash-badge-customer' : 'dash-badge-admin';
            
            let deleteBtn = isCustomer ? 
                `<button type="button" class="dash-btn dash-btn-danger" onclick="deleteUserAjax(${u.id})" title="Xoá user"><i class="fa-solid fa-trash"></i></button>` : 
                ``;

            html += `
                <tr>
                    <td style="font-weight: 500;">#${u.id}</td>
                    <td><b>${u.username || '-'}</b></td>
                    <td>${u.fullName || '-'}</td>
                    <td><a href="mailto:${u.email}" style="color: var(--brand-primary); text-decoration: none;">${u.email || '-'}</a></td>
                    <td>${u.phoneNumber || '-'}</td>
                    <td><span class="dash-badge ${badgeClass}">${u.role || '-'}</span></td>
                    <td>
                        <div class="action-stack">
                            <a class="dash-btn dash-btn-edit" href="/admin/users/${u.id}/edit" title="Sửa"><i class="fa-solid fa-pen"></i></a>
                            ${deleteBtn}
                        </div>
                    </td>
                </tr>
            `;
        });

        html += `</tbody></table>`;
        container.innerHTML = html;
    }

    // ==========================================
    // RENDER SẢN PHẨM (XE)
    // ==========================================
    function renderCars(cars) {
        if (!cars || cars.length === 0) {
            container.innerHTML = `
                <div class="dash-empty">
                    <i class="fa-solid fa-car-tunnel"></i>
                    <p>Không tìm thấy xe nào phù hợp.</p>
                </div>`;
            return;
        }

        let html = `
            <table class="dash-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Ảnh</th>
                        <th>Model</th>
                        <th>Hãng xe</th>
                        <th>Năm SX</th>
                        <th>Giá bán</th>
                        <th>Tồn kho</th>
                        <th>Thao tác</th>
                    </tr>
                </thead>
                <tbody>
        `;

        cars.forEach(c => {
            let priceFormat = new Intl.NumberFormat('vi-VN').format(c.price || 0) + ' đ';
            if (c.discount > 0) {
                let finalPriceFormat = new Intl.NumberFormat('vi-VN').format(c.finalPrice || 0) + ' đ';
                priceFormat = `<span style="text-decoration: line-through; color: var(--dash-text-muted); font-size: 12px;">${priceFormat}</span><br><b style="color: var(--brand-primary);">${finalPriceFormat}</b>`;
            }

            let imgUrl = c.imageUrl ? '/' + c.imageUrl : '/images/default-car.jpg';

            html += `
                <tr>
                    <td style="font-weight: 500;">#${c.id}</td>
                    <td>
                        <img src="${imgUrl}" class="dash-table-img" alt="Car">
                    </td>
                    <td style="font-weight: 600; color: var(--dash-text-main);">${c.model || '-'}</td>
                    <td>${c.brandName || '-'}</td>
                    <td>${c.manufactureYear || '-'}</td>
                    <td>${priceFormat}</td>
                    <td>
                        <span class="dash-badge ${c.stockQuantity > 0 ? 'dash-badge-success' : 'dash-badge-error'}">${c.stockQuantity || 0}</span>
                    </td>
                    <td>
                        <div class="action-stack">
                            <a class="dash-btn dash-btn-edit" href="/admin/cars/${c.id}/edit" title="Sửa"><i class="fa-solid fa-pen"></i></a>
                            <button type="button" class="dash-btn dash-btn-danger" onclick="deleteCarAjax(${c.id})" title="Xoá"><i class="fa-solid fa-trash"></i></button>
                        </div>
                    </td>
                </tr>
            `;
        });

        html += `</tbody></table>`;
        container.innerHTML = html;
    }

    // ==========================================
    // RENDER ĐƠN HÀNG
    // ==========================================
    function renderOrders(orders) {
        if (!orders || orders.length === 0) {
            container.innerHTML = `
                <div class="dash-empty">
                    <i class="fa-solid fa-box-open"></i>
                    <p>Chưa có đơn hàng nào trong hệ thống.</p>
                </div>`;
            return;
        }

        let html = `
            <table class="dash-table">
                <thead>
                    <tr>
                        <th>Mã ĐH</th>
                        <th>Khách hàng</th>
                        <th>Xe</th>
                        <th>Ngày đặt</th>
                        <th>Tổng tiền</th>
                        <th>Thanh toán</th>
                        <th>Trạng thái</th>
                        <th>Thao tác</th>
                    </tr>
                </thead>
                <tbody>
        `;

        const statusMap = {
            'PENDING': { text: 'Chờ duyệt', badge: 'dash-badge-warning' },
            'APPROVED': { text: 'Đã duyệt', badge: 'dash-badge-info' },
            'CONFIRMED': { text: 'Xác nhận', badge: 'dash-badge-info' },
            'DELIVERING': { text: 'Đang giao', badge: 'dash-badge-info' },
            'DELIVERED': { text: 'Đã giao', badge: 'dash-badge-success' },
            'COMPLETED': { text: 'Hoàn tất', badge: 'dash-badge-success' },
            'CANCELLED': { text: 'Đã hủy', badge: 'dash-badge-error' },
        };

        const paymentMap = {
            'PENDING': { text: 'Chưa TT', badge: 'dash-badge-warning' },
            'SUCCESS': { text: 'Đã TT', badge: 'dash-badge-success' },
            'CANCELLED': { text: 'Hủy', badge: 'dash-badge-error' },
        };

        orders.forEach(o => {
            let totalStr = new Intl.NumberFormat('vi-VN').format(o.totalPrice || 0) + ' đ';
            let dateStr = o.orderDate ? new Date(o.orderDate).toLocaleDateString('vi-VN') : '-';

            let st = statusMap[o.status] || { text: o.status, badge: 'dash-badge-warning' };
            let pt = paymentMap[o.paymentStatus] || { text: o.paymentStatus || 'N/A', badge: 'dash-badge-warning' };

            let canConfirm = o.status === 'PENDING';
            let canCancel = o.status === 'PENDING' || o.status === 'DELIVERING';

            let confirmBtn = canConfirm
                ? `<button type="button" class="dash-btn dash-btn-success" style="background-color:#22c55e;color:white" onclick="confirmOrderAjax(${o.id})" title="Duyệt đơn"><i class="fa-solid fa-check"></i></button>`
                : '';

            let cancelBtn = canCancel
                ? `<button type="button" class="dash-btn dash-btn-danger" onclick="cancelOrderAjax(${o.id})" title="Hủy đơn"><i class="fa-solid fa-ban"></i></button>`
                : '';

            html += `
                <tr>
                    <td style="font-weight: 500;">#${o.id}</td>
                    <td>
                        <div style="font-weight: 500; margin-bottom: 4px;">${o.customerName || 'N/A'}</div>
                        <div style="font-size: 12px; color: var(--dash-text-muted);"><i class="fa-solid fa-phone"></i> ${o.phone || 'N/A'}</div>
                    </td>
                    <td>
                        <div style="font-weight: 500;">${o.carName || 'N/A'}</div>
                        <div style="font-size: 12px; color: var(--dash-text-muted);">SL: ${o.quantity || 0}</div>
                    </td>
                    <td>${dateStr}</td>
                    <td style="color: var(--brand-primary); font-weight: 600;">${totalStr}</td>
                    <td><span class="dash-badge ${pt.badge}">${pt.text}</span></td>
                    <td><span class="dash-badge ${st.badge}">${st.text}</span></td>
                    <td>
                        <div class="action-stack">
                            ${confirmBtn}
                            ${cancelBtn}
                        </div>
                    </td>
                </tr>
            `;
        });

        html += `</tbody></table>`;
        container.innerHTML = html;
    }

    // ==========================================
    // AJAX ACTIONS
    // ==========================================
    window.deleteUserAjax = function(id) {
        if (!confirm("Chắc chắn xóa người dùng này?")) return;
        fetch(`/admin/api/users/${id}`, { method: 'DELETE' })
            .then(res => {
                if(res.ok) loadData("users", searchInput ? searchInput.value : "");
                else res.text().then(msg => alert("Lỗi: " + msg));
            })
            .catch(err => console.error(err));
    };

    window.deleteCarAjax = function(id) {
        if (!confirm("Chắc chắn xóa sản phẩm xe này?")) return;
        fetch(`/admin/api/cars/${id}`, { method: 'DELETE' })
            .then(res => {
                if(res.ok) loadData("products", searchInput ? searchInput.value : "");
                else res.text().then(msg => alert("Lỗi: " + msg));
            })
            .catch(err => console.error(err));
    };

    window.confirmOrderAjax = function(id) {
        if (!confirm("Bạn muốn xác nhận duyệt đơn hàng #" + id + "?")) return;
        fetch(`/admin/orders/${id}/confirm`, { method: 'PUT' })
            .then(res => {
                if(res.ok) loadData("orders", "");
                else res.text().then(msg => alert("Lỗi: " + msg));
            })
            .catch(err => console.error(err));
    };

    window.cancelOrderAjax = function(id) {
        if (!confirm("Bạn có chắc muốn HỦY đơn hàng #" + id + "? Tồn kho sẽ được hoàn lại.")) return;
        fetch(`/admin/orders/${id}/cancel`, { method: 'PUT' })
            .then(res => {
                if(res.ok) loadData("orders", "");
                else res.text().then(msg => alert("Lỗi: " + msg));
            })
            .catch(err => console.error(err));
    };
});