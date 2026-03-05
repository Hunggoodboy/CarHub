document.addEventListener("DOMContentLoaded", function () {
    const urlParams = new URLSearchParams(window.location.search);
    const carId = urlParams.get("id");

    if (!carId) {
        document.getElementById("loading-msg").innerHTML =
            "<h2 style='color:red'>Lỗi: Không tìm thấy ID xe!</h2>";
        return;
    }

    fetch(`/api/cars/${carId}`)
        .then(response => {
            if (!response.ok) throw new Error("Xe không tồn tại");
            return response.json();
        })
        .then(data => {
            const car = data.car;

            document.getElementById("loading-msg").style.display = "none";
            document.getElementById("car-content").style.display = "flex";

            const buyBtn = document.getElementById("buy-now-btn");
            if (buyBtn) {
                buyBtn.href = `/payment?id=${car.id}`;
            }

            document.getElementById("car-model").innerText = car.model;
            document.getElementById("car-brand").innerText =
                car.brandName || "Đang cập nhật";
            document.getElementById("car-origin").innerText =
                car.brandOrigin || "Đang cập nhật";
            document.getElementById("car-color").innerText = car.color;
            document.getElementById("car-year").innerText = car.manufactureYear;
            document.getElementById("car-stock").innerText =
                car.stockQuantity > 0 ? car.stockQuantity : "Hết hàng";
            document.getElementById("car-desc").innerText =
                car.description || "Đang cập nhật...";

            document.getElementById("car-img").src = car.imageUrl
                ? `/${car.imageUrl.replace("car_images", "car-images")}`
                : "/images/default-car.png";

            const formatter = new Intl.NumberFormat("vi-VN");
            document.getElementById("car-final-price").innerText =
                formatter.format(car.finalPrice || car.price);

            const oldPriceEl = document.getElementById("car-old-price");
            if (car.discount > 0) {
                oldPriceEl.style.display = "inline";
                oldPriceEl.innerText = formatter.format(car.price) + " ₫";
            } else {
                oldPriceEl.style.display = "none";
            }


            renderReviews(data.reviews);

            const submitBtn = document.getElementById("send-comment-btn");
            const commentInput = document.getElementById("comment-input");

            if (submitBtn && commentInput) {
                submitBtn.addEventListener("click", function () {
                    const content = commentInput.value.trim();
                    if (!content) {
                        alert("Vui lòng nhập bình luận!");
                        return;
                    }

                    const payload = {
                        carId: carId,
                        content: content
                    };

                    fetch("/api/reviews", {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify(payload)
                    })
                        .then(res => {
                            if (!res.ok) throw new Error("Gửi bình luận thất bại");
                            return res.json();
                        })
                        .then(() => {
                            commentInput.value = "";
                            return fetch(`/api/cars/${carId}`);
                        })
                        .then(res => res.json())
                        .then(updatedData => {
                            renderReviews(updatedData.reviews);
                        })
                        .catch(err => {
                            console.error(err);
                            alert("Có lỗi xảy ra, vui lòng thử lại!");
                        });
                });
            }


            if (Array.isArray(data.carsSimilar)) {
                const relatedCars = data.carsSimilar
                    .filter(c => c.id !== car.id)
                    .slice(0, 8);

                renderRelatedCars(relatedCars);
            } else {
                renderRelatedCars([]);
            }
        })
        .catch(error => {
            console.error(error);
            document.getElementById("loading-msg").innerHTML =
                `<h2 style='color:red'>${error.message}</h2>`;
        });
});

function renderReviews(reviews) {
    const reviewList = document.getElementById("comment-list");
    if (!reviewList) return;

    if (!Array.isArray(reviews) || reviews.length === 0) {
        reviewList.innerHTML =
            "<p style='color:#777'>Chưa có bình luận nào.</p>";
        return;
    }

    let html = "";
    reviews.forEach(review => {
        html += `
            <div class="review-item">
                <p class="review-content">${review.content}</p>
                <small style="color:#888">
                    ${review.userName || "Người dùng ẩn danh"}
                </small>
            </div>
        `;
    });

    reviewList.innerHTML = html;
}

function renderRelatedCars(cars) {
    const container = document.getElementById("related-car-list");
    if (!container) return;

    if (!Array.isArray(cars) || cars.length === 0) {
        container.innerHTML =
            "<p style='color:#777'>Không có xe liên quan</p>";
        return;
    }

    const formatter = new Intl.NumberFormat("vi-VN");
    let html = "";

    cars.forEach(car => {
        const imgPath = car.imageUrl
            ? `/${car.imageUrl.replace("car_images", "car-images")}`
            : "/images/default-car.png";

        const price = car.finalPrice && car.finalPrice > 0
            ? car.finalPrice
            : car.price;

        html += `
            <div class="card">
                <img src="${imgPath}" alt="${car.model}">
                <h4>${car.model}</h4>
                <p class="price">${formatter.format(price)} ₫</p>
                <a href="/product_detail?id=${car.id}" class="btn">
                    Xem chi tiết
                </a>
            </div>
        `;
    });

    container.innerHTML = html;
}