document.getElementById("submit-warranty-btn").addEventListener("click",function() {
    const data ={
        nameCar : document.getElementById("name_car").value,
        branchCar : document.getElementById("branch_car").value,
        yearCar : document.getElementById("year_car").value,
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

