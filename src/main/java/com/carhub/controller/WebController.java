package com.carhub.controller;

import com.carhub.dto.Response.AuthResponse;
import com.carhub.dto.CarDTO;
import com.carhub.dto.UserDTO;
import com.carhub.entity.Car;
import com.carhub.entity.User;
import com.carhub.dto.Request.RegisterRequest;
import com.carhub.service.AuthService;
import com.carhub.service.CarService;
import com.carhub.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;


import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;


@Controller
@RequiredArgsConstructor
public class WebController {

    private final CarService carService;
    private final AuthService authService;
    private final UserService userService;

    // Trang chủ
    @GetMapping({ "/", "/index" })
public String index(Model model, Authentication authentication) {

    List<CarDTO> cars = carService.getAllCars();
    model.addAttribute("cars", cars);

    if (authentication != null && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal())) {

        Long userId = userService.getId(authentication);
        UserDTO user = userService.getUserById(userId).orElse(null);

        if (user != null) {
            model.addAttribute("fullName", user.getFullName());
        }
    }

    return "index";
}

    // Trang chi tiết xe
    // Trang chi tiết xe
    @GetMapping("/product_detail")
    public String productDetail() {
        return "product_detail";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    // Trang đăng nhập
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    // Trang thanh toán 
    @GetMapping("/payment")
    public String payment() {
        return "payment";
    }
    // Trang bao hành
    @GetMapping("/warranty")
    public String warranty(){
        return "warranty";
    }
    // Trang xe đã mua
    @GetMapping("/my-cars")
    public String myCarsPage() {
        return "purchased_cars";
    }
    public String getMethodName(@RequestParam String param) {
        return new String();
    }
    
    // Trang đăng ký
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }
    @GetMapping("/customer-view")
    public String showCustomerView(Model model) {
        model.addAttribute("car", new CarDTO());
        return "customer-view";
    }
    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }

    @PostMapping("/car/save")
    public String saveCar(@ModelAttribute CarDTO carDTO,
                          @RequestParam("model") String model,
                          @RequestParam("price") Long price,
                          @RequestParam("manufactureYear") int manufactureYear,
                          @RequestParam("color") String color,
                          @RequestParam("description") String description,
                          @RequestParam("imageFile") MultipartFile imageFile)
    {
        try {
            carService.saveCarService(model, price, manufactureYear, color, description, imageFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "redirect:/";
    }

    // Xử lý đăng ký
    @PostMapping("/register")
    public String processRegister(@ModelAttribute("registerRequest") RegisterRequest request, Model model) {
        AuthResponse response = authService.register(request);

        if (response.isSuccess()) {
            // Đăng ký thành công -> Chuyển hướng sang login
            return "redirect:/login?success";
        } else {
            // Đăng ký thất bại -> Ở lại trang register và báo lỗi
            model.addAttribute("error", response.getMessage());
            return "register";
        }
    }
}