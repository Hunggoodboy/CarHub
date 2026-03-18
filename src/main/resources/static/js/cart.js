const CART_STORAGE_KEY = "carhub_cart_items";

function readCartItems() {
    try {
        const raw = localStorage.getItem(CART_STORAGE_KEY);
        if (!raw) {
            return [];
        }
        const parsed = JSON.parse(raw);
        if (!Array.isArray(parsed)) {
            return [];
        }

        return parsed
            .map(item => ({
                id: Number(item.id),
                quantity: Number(item.quantity || 1)
            }))
            .filter(item => Number.isFinite(item.id) && item.quantity > 0);
    } catch (error) {
        console.error("Không đọc được giỏ hàng:", error);
        return [];
    }
}

function saveCartItems(items) {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
}

function formatCurrency(value) {
    return Number(value || 0).toLocaleString("vi-VN") + " đ";
}

function normalizeItem(item) {
    return {
        id: Number(item.id),
        quantity: Number(item.quantity || 1)
    };
}

function addItemToCart(item) {
    const normalized = normalizeItem(item);
    if (!Number.isFinite(normalized.id) || normalized.quantity <= 0) {
        return;
    }

    const items = readCartItems();
    const existing = items.find(cartItem => cartItem.id === normalized.id);

    if (existing) {
        existing.quantity += normalized.quantity;
    } else {
        items.push(normalized);
    }

    saveCartItems(items);
    updateCartUI();
}

function removeItemFromCart(id) {
    const itemId = Number(id);
    if (!Number.isFinite(itemId)) {
        return;
    }

    const filtered = readCartItems().filter(item => item.id !== itemId);
    saveCartItems(filtered);
    updateCartUI();
}

function updateItemQuantity(id, delta) {
    const itemId = Number(id);
    if (!Number.isFinite(itemId)) {
        return;
    }

    const items = readCartItems();
    const existing = items.find(item => item.id === itemId);

    if (!existing) {
        return;
    }

    existing.quantity += delta;
    if (existing.quantity <= 0) {
        removeItemFromCart(itemId);
        return;
    }

    saveCartItems(items);
    updateCartUI();
}

async function fetchCarById(id) {
    const response = await fetch(`/api/cars/${id}`);
    if (!response.ok) {
        throw new Error(`Không tải được xe ${id}`);
    }

    const data = await response.json();
    return data && data.car ? data.car : null;
}

async function hydrateCartItems() {
    const baseItems = readCartItems();
    if (baseItems.length === 0) {
        return [];
    }

    const hydrated = await Promise.all(baseItems.map(async item => {
        try {
            const car = await fetchCarById(item.id);
            if (!car) {
                return null;
            }

            return {
                id: item.id,
                model: car.model || "Xe không tên",
                imageUrl: car.imageUrl || "",
                finalPrice: Number(car.finalPrice || 0),
                quantity: item.quantity
            };
        } catch (error) {
            console.error(error);
            return null;
        }
    }));

    const validItems = hydrated.filter(Boolean);
    const validIds = new Set(validItems.map(item => item.id));
    const cleaned = baseItems.filter(item => validIds.has(item.id));

    if (cleaned.length !== baseItems.length) {
        saveCartItems(cleaned);
    }

    return validItems;
}

function buildImagePath(imageUrl) {
    if (!imageUrl) {
        return "https://via.placeholder.com/80x60?text=Car";
    }

    if (imageUrl.startsWith("http")) {
        return imageUrl;
    }

    return `/${imageUrl.replace("car_images", "car-images")}`;
}

function ensureCartShell() {
    if (document.getElementById("floating-cart")) {
        return;
    }

    const shell = document.createElement("aside");
    shell.id = "floating-cart";
    shell.className = "floating-cart";
    shell.innerHTML = `
        <button type="button" class="cart-fab" id="cart-fab" aria-label="Mở giỏ hàng">
			<i class="fa-solid fa-cart-shopping"></i>
			<span class="cart-count" id="cart-count">0</span>
		</button>

		<section class="cart-drawer" id="cart-drawer" aria-hidden="true">
			<header class="cart-header">
                <h3>Mục yêu thích</h3>
                <button type="button" id="close-cart" class="cart-close" aria-label="Đóng giỏ hàng">
					<i class="fa-solid fa-xmark"></i>
				</button>
			</header>

			<div class="cart-items" id="cart-items"></div>

			<footer class="cart-footer">
				<div class="cart-total-row">
                    <span>Tổng tạm tính</span>
					<strong id="cart-total">0 đ</strong>
				</div>
                <button type="button" class="cart-checkout-btn">Đặt lịch tư vấn</button>
			</footer>
		</section>
	`;

    document.body.appendChild(shell);
}

function renderCartItems(items) {
    const cartItemsEl = document.getElementById("cart-items");
    const cartTotalEl = document.getElementById("cart-total");
    const cartCountEl = document.getElementById("cart-count");
    const cartLinkEl = document.getElementById
    if (!cartItemsEl || !cartTotalEl || !cartCountEl) {
        return;
    }

    if (items.length === 0) {
        cartItemsEl.innerHTML = `
			<div class="cart-empty">
				<i class="fa-regular fa-face-smile"></i>
                <p>Chưa có xe nào trong mục yêu thích.</p>
			</div>
		`;
        cartTotalEl.textContent = "0 đ";
        cartCountEl.textContent = "0";
        return;
    }

    const totalQuantity = items.reduce((sum, item) => sum + item.quantity, 0);
    const totalPrice = items.reduce((sum, item) => sum + item.finalPrice * item.quantity, 0);

    cartItemsEl.innerHTML = items.map(item => `
		<article class="cart-item" data-id="${item.id}">
			<img src="${buildImagePath(item.imageUrl)}" alt="${item.model}">
			<div class="cart-item-content">
				<h4 href = "/product_detail?id=${item.id}">${item.model}</h4>
				<p>${formatCurrency(item.finalPrice)}</p>
			</div>
            <a id="view-detail-cart" href="/product_detail?id=${item.id}"> Xem thông tin</a>
			<button type="button" class="cart-remove" data-action="remove" data-id="${item.id}">
				<i class="fa-regular fa-trash-can"></i>
			</button>
		</article>
	`).join("");

    cartCountEl.textContent = String(totalQuantity);
    cartTotalEl.textContent = formatCurrency(totalPrice);
}

function openCart() {
    const drawer = document.getElementById("cart-drawer");
    if (!drawer) {
        return;
    }

    drawer.classList.add("is-open");
    drawer.setAttribute("aria-hidden", "false");
}

function closeCart() {
    const drawer = document.getElementById("cart-drawer");
    if (!drawer) {
        return;
    }

    drawer.classList.remove("is-open");
    drawer.setAttribute("aria-hidden", "true");
}

async function updateCartUI() {
    try {
        const hydratedItems = await hydrateCartItems();
        renderCartItems(hydratedItems);
    } catch (error) {
        console.error(error);
        renderCartItems([]);
    }
}

function bindCartEvents() {
    const fab = document.getElementById("cart-fab");
    const closeBtn = document.getElementById("close-cart");
    const itemsContainer = document.getElementById("cart-items");
    const checkoutBtn = document.querySelector(".cart-checkout-btn");

    if (fab) {
        fab.addEventListener("click", openCart);
    }

    if (closeBtn) {
        closeBtn.addEventListener("click", closeCart);
    }

    if (checkoutBtn) {
        checkoutBtn.addEventListener("click", () => {
            alert("Cảm ơn bạn! Chúng tôi sẽ liên hệ để tư vấn đơn hàng sớm nhất.");
        });
    }

    if (itemsContainer) {
        itemsContainer.addEventListener("click", event => {
            const action = event.target.closest("[data-action]");
            if (!action) {
                return;
            }

            const id = action.getAttribute("data-id");
            const type = action.getAttribute("data-action");

            if (type === "plus") {
                updateItemQuantity(id, 1);
            }

            if (type === "minus") {
                updateItemQuantity(id, -1);
            }

            if (type === "remove") {
                removeItemFromCart(id);
            }
        });
    }

    document.addEventListener("click", event => {
        const addButton = event.target.closest("[data-add-to-cart]");
        if (!addButton) {
            return;
        }

        event.preventDefault();
        addItemToCart({
            id: addButton.getAttribute("data-id"),
            model: addButton.getAttribute("data-model"),
            imageUrl: addButton.getAttribute("data-image"),
            finalPrice: addButton.getAttribute("data-price"),
            quantity: 1
        });
        openCart();
    });
}

document.addEventListener("DOMContentLoaded", () => {
    ensureCartShell();
    bindCartEvents();
    updateCartUI();
});

window.CarHubCart = {
    addItem: addItemToCart,
    open: openCart,
    close: closeCart,
    getItems: readCartItems
};
