document.addEventListener("DOMContentLoaded",function(){
    const msg=document.getElementById("welcome-message");
    if (msg){
        setTimeout(() => {
            msg.style.opacity="0";
            msg.style.top="0px";
            setTimeout(() => msg.remove(),500);
        },1000);
        fetch("/clear-login-flag",{method:"POST"});
    }

});