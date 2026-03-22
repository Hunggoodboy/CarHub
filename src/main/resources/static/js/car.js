document.addEventListener("DOMContentLoaded", function () {

    const urlParams = new URLSearchParams(window.location.search);
    const carId = urlParams.get("id");

    if (!carId) {
        document.getElementById("loading-msg").innerHTML =
            "<h2 style='color:red'>Lỗi: Không tìm thấy ID xe!</h2>";
        return;
    }

    loadCarDetail(carId);

    //  khôi phục comment nếu đúng xe 
    const savedComment = localStorage.getItem("pendingComment");
    const savedRating = localStorage.getItem("pendingRating");
    const savedCarId = localStorage.getItem("pendingCarId");

    const currentCarId = carId;

    if(savedComment && savedCarId === currentCarId){

        const commentInput = document.getElementById("comment-input");

        if(commentInput){
            commentInput.value = savedComment;
        }

        if(savedRating){
            selectedRating = savedRating;

            const stars = document.querySelectorAll(".star");

            stars.forEach(s => s.classList.remove("active"));

            for(let i=0;i<savedRating;i++){
                if(stars[i]){
                    stars[i].classList.add("active");
                }
            }
        }

    }

});

let selectedRating = 0;

function setupRating() {

    const stars = document.querySelectorAll(".star");

    if (!stars) return;

    stars.forEach(star => {

        star.addEventListener("click", function () {

            selectedRating = this.dataset.value;

            stars.forEach(s => s.classList.remove("active"));

            for (let i = 0; i < selectedRating; i++) {
                stars[i].classList.add("active");
            }

        });

    });

}


function loadCarDetail(carId) {

    fetch(`/api/cars/${carId}`)
        .then(response => {
            if (!response.ok) throw new Error("Xe không tồn tại");
            return response.json();
        })
        .then(data => {

            const car = data.car;
             setupRating();


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
                formatter.format(car.finalPrice || car.price) + " ₫";

            const oldPriceEl = document.getElementById("car-old-price");

            if (car.discount > 0) {
                oldPriceEl.style.display = "inline";
                oldPriceEl.innerText = formatter.format(car.price) + " ₫";
            } else {
                oldPriceEl.style.display = "none";
            }

            // --- PHẦN CHỈNH SỬA THÊM: Xử lý nút Tư vấn ---
            const consultBtn = document.querySelector(".btn-consult");
            if (consultBtn) {
                consultBtn.onclick = function() {
                    if (typeof ChatWidget !== 'undefined') {
                        // Lưu tên xe vào bộ nhớ tạm
                        sessionStorage.setItem("pending_car_name", car.model);
                        // Mở khung chat
                        ChatWidget.openWidget();
                    } else {
                        alert('Đang kết nối, vui lòng thử lại!');
                    }
                };
            }
            // --------------------------------------------

            renderReviews(data.reviews);

            setupComment(car.id);
            console.log('sellerId:', car.sellerId);
            ChatWidget.init(car.sellerId, 'Người bán');
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

}


function setupComment(carId) {

    const submitBtn = document.getElementById("send-comment-btn");
    const commentInput = document.getElementById("comment-input");

    if (!submitBtn || !commentInput) return;

    submitBtn.addEventListener("click", function () {

        const content = commentInput.value.trim();

        if (!content) {
            showAlert("Vui lòng nhập bình luận!");
            return;
        }

        if (selectedRating === 0) {
            showAlert("Vui lòng chọn số sao đánh giá!");
            return;
        }

        fetch(`/api/reviews/${carId}`, {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({

                comment: content,
                rating: selectedRating

            })

        })
            .then(res => {

                if (res.status === 401 || res.redirected ) {
                    throw new Error("Bạn chưa đăng nhập");
                }

                if (res.status === 403) {
                    throw new Error("Bạn chưa mua xe nên không thể đánh giá");
                }

                if (!res.ok) {
                    throw new Error("Có lỗi xảy ra");
                }

            })
            .then(() => {

                commentInput.value = "";

                document.querySelectorAll(".star").forEach(s=>{
                    s.classList.remove("active");
                });

                selectedRating = 0;

                // giữ cmt
                localStorage.removeItem("pendingComment");
                localStorage.removeItem("pendingRating");
                localStorage.removeItem("pendingCarId");

                return fetch(`/api/cars/${carId}`);

            })
            .then(res => res.json())
            .then(updatedData => {

                renderReviews(updatedData.reviews);

            })
            .catch(err => {

                    console.error(err);
                    if(err.message === "Bạn chưa đăng nhập"){
                        showLoginModal();
                    }else if(err.message === "Bạn chưa mua xe nên không thể đánh giá"){
                        showBuyCarModal();
                    }else{
                        showAlert(err.message);
                    }

            });

    });

}
function closeAlert() {
    document.getElementById("custom-alert").style.display = "none";
}
function showAlert(message) {
    const alertBox = document.getElementById("custom-alert");
    const alertMessage = document.getElementById("alert-message");

    if (alertMessage) {
        alertMessage.innerText = message;
    }

    if (alertBox) {
        alertBox.style.display = "flex";
    }
}

function showLoginModal(){
    const modal = document.getElementById("login-modal");
    modal.style.display = "flex";

    document.getElementById("go-login-btn").onclick = function(){

        // giữ cmt
        localStorage.setItem("pendingComment", document.getElementById("comment-input").value);
        localStorage.setItem("pendingRating", selectedRating);
        localStorage.setItem("pendingCarId", new URLSearchParams(window.location.search).get("id"));

        window.location.href = "/login?redirect=" + encodeURIComponent(window.location.href);
    };

    document.getElementById("close-login-modal").onclick=function(){
        modal.style.display = "none";
    };
}

function showBuyCarModal(){
    const modal=document.getElementById("buy-car");
    if(!modal) return;
    modal.style.display="flex";
    document.getElementById("close-buy-car-modal").onclick = function(){
        modal.style.display="none";
    };
}

function renderReviews(reviews) {

    const reviewList = document.getElementById("comment-list");

    if (!reviewList) return;

    if (!Array.isArray(reviews) || reviews.length === 0) {

        reviewList.innerHTML =
            "<p style='color:#777'>Chưa có bình luận nào.</p>";

        return;
    }

    let html = "";

    const firstReviews = reviews.slice(0,5);

    firstReviews.forEach(review => {
        html += `
            <div class="review-item">
                <div class="review-rating">
                    ${"⭐".repeat(review.rating)}
                </div>
                <p class="review-content">${review.comment}</p>
                <small style="color:#888">
                    ${review.userName || "Người dùng ẩn danh"}
                </small>
            </div>
        `;
    });

    if (reviews.length > 5) {
        html += `
            <button id="show-all-reviews" class="review-btn" style="margin-top:10px">
                Xem tất cả bình luận
            </button>
        `;
    }

    reviewList.innerHTML = html;

    setupShowAllReviews(reviews);

}

function setupShowAllReviews(reviews){

    const btn = document.getElementById("show-all-reviews");

    if(!btn) return;

    btn.addEventListener("click",function(){

        const reviewList = document.getElementById("comment-list");

        let html = "";

        reviews.forEach(review => {

            html += `
                <div class="review-item">
                    <div class="review-rating">
                        ${"⭐".repeat(review.rating)}
                    </div>
                    <p class="review-content">${review.comment}</p>
                    <small style="color:#888">
                        ${review.userName || "Người dùng ẩn danh"}
                    </small>
                </div>
            `;
        });

        html += `
            <button id="hide-reviews" class="review-btn" style="margin-top:10px">
                Ẩn bớt
            </button>
        `;

        reviewList.innerHTML = html;

        document.getElementById("hide-reviews").addEventListener("click", function(){
            renderReviews(reviews);
        });

    });

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
                <div class="info">
                    <h4>${car.model}</h4>
                    <p class="price">${formatter.format(price)} ₫</p>
                    <a href="/product_detail?id=${car.id}" class="btn">
                        Xem chi tiết
                    </a>
                </div>
            </div>
        `;

    });

    container.innerHTML = html;

}