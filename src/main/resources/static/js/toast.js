function showToast(message, type = "success") {
    const overlay = document.getElementById("toast-overlay");
    const box = overlay.querySelector(".toast-box");
    const msg = document.getElementById("toast-message");

    msg.innerText = message;

    box.className = `toast-box ${type}`;
    overlay.classList.remove("hidden");

    setTimeout(() => {
        overlay.classList.add("show");
    }, 10);

    setTimeout(() => {
        overlay.classList.remove("show");
        overlay.classList.add("hidden");
    }, 2000);
}

/* override alert */
window.alert = function(message) {
    showToast(message, "success");
};