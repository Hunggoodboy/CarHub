package com.fnb.carhub.controller;

import com.fnb.carhub.dto.CarDTO;
import com.fnb.carhub.dto.RegisterRequest;
import com.fnb.carhub.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final CarService carService;

    // Trang chủ
    @GetMapping({"/", "/index"})
    public String index(Model model) {
        List<CarDTO> cars = carService.getAllCars();
        model.addAttribute("cars", cars);
        return "index";
    }

    // Trang đăng nhập
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Trang đăng ký
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }
}