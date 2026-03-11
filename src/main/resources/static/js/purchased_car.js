document.addEventListener("DOMContentLoaded",function(){
    loadPurchasedCars();
});
function loadPurchasedCars(){
    fetch("/api/orders/my-cars")
    .then(res => res.json())
    .then(data =>{
        document.getElementById("loading-msg").style.display="none";
        renderPurchasedCars(data);
    })
    .catch(err =>{
        console.error(err);
    });
}
function renderPurchasedCars(cars){
    const container=document.getElementById("purchased-car-list");
    if(!Array.isArray(cars) || cars.length==0){
        container.innerHTML="<p> Bạn chưa mua xe nào</p>";
        return;
    }
    const formatter=new Intl.NumberFormat("vi-VN");
    let html="";
    cars.forEach(car =>{
        const imgPath=car.imageUrl
        ? `/${car.imageUrl.replace("car_images","car-images")}`
        : "/images/default-car.png";

    html+=`
        <div class="card">

            <img src="${imgPath}" alt="${car.model}">

            <div class="info">

                <h4>${car.model}</h4>

                <p class="price">
                    ${formatter.format(car.price)} ₫
                </p>

                <a href="/warranty/request?carId=${car.id}" class="btn">
                    Yêu cầu bảo hành
                </a>

            </div>

        </div>
        `;
    });
    container.innerHTML=html;
}