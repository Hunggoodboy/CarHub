document.addEventListener("DOMContentLoaded", function() {
    const urlParams = new URLSearchParams(window.location.search);
    const carId = urlParams.get('id');

    if (!carId) {
        document.getElementById('loading-msg').innerHTML = "<h2 style='color:red'>Lỗi: Không tìm thấy ID xe!</h2>";
        return;
    }

    // Gọi API từ @RestController CarController
    fetch(`/api/cars/${carId}`)
        .then(response => {
            if (!response.ok) throw new Error("Xe không tồn tại");
            return response.json();
        })
        .then(car => {
            document.getElementById('loading-msg').style.display = 'none';
            document.getElementById('car-content').style.display = 'flex';

            // lấy các dữ liệu tương ứng của xe 
            document.getElementById('car-model').innerText = car.model;
            document.getElementById('car-brand').innerText = car.brandName;
            document.getElementById('car-origin').innerText = car.brandOrigin;
            document.getElementById('car-color').innerText = car.color;
            document.getElementById('car-year').innerText = car.manufactureYear;
            document.getElementById('car-stock').innerText = car.stockQuantity > 0 ? car.stockQuantity : "Hết hàng";
            document.getElementById('car-desc').innerText = car.description || "Đang cập nhật...";
            
            // hiển thị ảnh
           document.getElementById('car-img').src = car.imageUrl
           ? `/${car.imageUrl.replace("car_images", "car-images")}`
           : '/images/default-car.png';

            const formatter = new Intl.NumberFormat('vi-VN');
            document.getElementById('car-final-price').innerText = formatter.format(car.finalPrice);
            
            if (car.discount > 0) {
                const oldPriceEl = document.getElementById('car-old-price');
                oldPriceEl.style.display = 'inline';
                oldPriceEl.innerText = formatter.format(car.price) + " đ";
            }
        })
        .catch(error => {
            console.error(error);
            document.getElementById('loading-msg').innerHTML = `<h2 style='color:red'>${error.message}</h2>`;
        });
});