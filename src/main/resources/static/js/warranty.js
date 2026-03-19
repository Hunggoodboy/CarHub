document.getElementById("submit-warranty-btn").addEventListener("click",function() {
    const carIdField = document.getElementById("car_id");
    if (!carIdField || !carIdField.value) {
        alert("Không xác định được xe cần bảo hành.");
        return;
    }
    const data ={
        carId : carIdField.value,
        street : document.getElementById("street").value ,
        ward : document.getElementById("ward").value,
        city : document.getElementById("city").value,
        phone : document.getElementById("phone").value,
        errorCar : document.getElementById("err_car").value,
    };
    fetch("/warranty/create",{
        method : "POST",
        headers :{
            "Content-Type":"application/json"
        },
        body: JSON.stringify(data)
    })
    .then(res => res.json())
    .then(result => {
        alert("Gửi yêu cầu bảo hành xe thành công!");
        console.log(result);
    })
    .catch(err => {
        console.error(err);
        alert("Có lỗi xảy ra!");
    });
});

