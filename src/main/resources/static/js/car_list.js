document.addEventListener("DOMContentLoaded", () => {
    loadAllCars();

    const form = document.querySelector(".search-box");
    if (form) {
        form.addEventListener("submit", function (e) {
            e.preventDefault(); // không reload trang
            searchCars();
        });
    }
});

function loadAllCars() {
    fetch("/api/cars")
        .then(res => res.json())
        .then(cars => renderCars(cars))
        .catch(err => console.error("Lỗi load xe:", err));
}

function searchCars() {
    const brand = document.querySelector("input[name='brand']").value.trim();
    const year = document.querySelector("input[name='year']").value;
    const price = document.querySelector("select[name='price']").value;

    let url = "/api/cars/advanced-search";
    const params = [];

    if (brand !== "") {
        params.push(`brand=${encodeURIComponent(brand)}`);
    }

    if (year !== "") {
        params.push(`year=${year}`);
    }

    if (price !== "") {
        const [min, max] = price.split("-");
        params.push(`minPrice=${min}`);
        params.push(`maxPrice=${max}`);
    }

    if (params.length > 0) {
        url += "?" + params.join("&");
    }

    fetch(url)
        .then(res => res.json())
        .then(cars => renderCars(cars))
        .catch(err => console.error("Lỗi tìm kiếm:", err));
}


function renderCars(cars) {
    const container = document.getElementById("car-list");
    let html = "";

    if (!cars || cars.length === 0) {
        container.innerHTML = "<p>Không tìm thấy xe phù hợp</p>";
        return;
    }

    cars.forEach(car => {
        const imgPath = `/${car.imageUrl.replace("car_images", "car-images")}`;

        html += `
            <div class="card">
                <img src="${imgPath}" alt="${car.model}">
                <div class="info">
                    <h3>${car.model}</h3>
                    <p>Năm sản xuất: ${car.manufactureYear}</p>
                    <p class="price">${formatPrice(car.finalPrice)}</p>
                    <a href="/product_detail?id=${car.id}" class="btn">
                        Xem chi tiết
                    </a>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

/* Giá */
function formatPrice(price) {
    return price.toLocaleString("vi-VN") + " ₫";
}
