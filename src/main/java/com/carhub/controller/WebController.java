package com.carhub.controller;

import com.carhub.dto.AuthResponse;
import com.carhub.dto.CarDTO;
import com.carhub.dto.RegisterRequest;
import com.carhub.entity.Car;
import com.carhub.service.AuthService;
import com.carhub.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final CarService carService;
    private final AuthService authService;

    // Trang chủ
    @GetMapping({"/", "/index"})
    public String index(Model model) {
        List<CarDTO> cars = carService.getAllCars();
        model.addAttribute("cars", cars);
        return "index";
    }

    // Trang chi tiết xe
    @GetMapping("/product_detail")
    public String productDetail() {
        return "product_detail";
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

    // Trang hiển thị form đăng bán xe
    @GetMapping("/customer-view")
    public String customerView(Model model) {
        model.addAttribute("car", new Car());
        return "customer-view";
    }

    // Xử lý lưu xe từ form customer-view
    @PostMapping("/admin/car/save")
    public String saveCarFromCustomerView(@ModelAttribute("car") Car car,
                                          @RequestParam("imageFile") MultipartFile imageFile,
                                          Model model) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadDir = "src/main/resources/static/car-images";
                Files.createDirectories(Paths.get(uploadDir));

                String originalFilename = imageFile.getOriginalFilename();
                String safeFilename = (originalFilename != null ? originalFilename.replace(" ", "_") : "car.png");
                String fileName = System.currentTimeMillis() + "_" + safeFilename;

                Path destination = Paths.get(uploadDir, fileName);
                Files.copy(imageFile.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

                // Lưu đường dẫn theo chuẩn trong DB (car_images -> sẽ được JS đổi sang car-images)
                String dbPath = "car_images/" + fileName;
                car.setImageUrl(dbPath);
            }

            // Giá trị mặc định cho một số field nếu chưa nhập
            if (car.getName() == null || car.getName().isEmpty()) {
                car.setName(car.getModel());
            }
            if (car.getStockQuantity() <= 0) {
                car.setStockQuantity(1);
            }
            if (car.getDiscount() < 0) {
                car.setDiscount(0);
            }

            carService.saveCar(car);
            return "redirect:/?carSaved=true";
        } catch (IOException e) {
            model.addAttribute("error", "Có lỗi khi lưu hình ảnh xe: " + e.getMessage());
            return "customer-view";
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi khi lưu thông tin xe: " + e.getMessage());
            return "customer-view";
        }
    }
}