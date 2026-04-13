package com.carhub.controller;

import com.carhub.dto.CarDTO;
import com.carhub.dto.UserDTO;
import com.carhub.dto.Response.OrderAdminDTO;
import com.carhub.service.Car.CarService;
import com.carhub.service.Car.OrderService;
import com.carhub.service.authentication.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;

import java.util.List; 
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CarService carService;
    private final OrderService orderService;

    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/manage";
    }

    @GetMapping("/manage")
    public String adminManage() {
        return "admin-manage"; 
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        UserDTO user = userService.getUserById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        model.addAttribute("activePage", "users");
        model.addAttribute("user", user);
        return "admin-edit-user";
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute("user") UserDTO userDTO,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserForAdmin(id, userDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Cáº­p nháº­t user thÃ nh cÃ´ng.");
            return "redirect:/admin/users/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/manage";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            Long adminId = userService.getId(authentication);
            userService.deleteCustomerByAdmin(adminId, id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa user thành công.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/manage";
    }



    @GetMapping("/cars/{id}/edit")
    public String editCar(@PathVariable Long id, Model model) {
        CarDTO car;
        try {
            car = carService.getCarById(id);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found", e);
        }
        model.addAttribute("activePage", "cars");
        model.addAttribute("brands", carService.getBrandsForAdmin());
        model.addAttribute("car", car);
        return "admin-edit-car";
    }

    @PostMapping("/cars/{id}/update")
    public String updateCar(@PathVariable Long id,
                            @ModelAttribute("car") CarDTO carDTO,
                            RedirectAttributes redirectAttributes) {
        try {
            carService.updateCarForAdmin(id, carDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật xe thành công.");
            return "redirect:/admin/cars/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/manage#products";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/cars/" + id + "/edit";
        }
    }

    @PostMapping("/cars/{id}/delete")
    public String deleteCar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            carService.deleteCarForAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xoá xe thành công");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/manage#products";
    }
    @GetMapping("/orders")
    @ResponseBody
    public List<OrderAdminDTO> getAllOrders() {
        return orderService.getAllOrdersForAdmin();
    }
    @PutMapping("/orders/{id}/confirm")
    @ResponseBody
    public ResponseEntity<String> confirmOrder(@PathVariable Long id) {
        try {
            orderService.confirmPaymentByAdmin(id);
            return ResponseEntity.ok("OK");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/orders/{id}/cancel")
    @ResponseBody
    public ResponseEntity<String> cancelOrder(@PathVariable Long id) {
        try {
            orderService.cancelOrderByAdmin(id);
            return ResponseEntity.ok("OK");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/users")
    @ResponseBody
    public List<UserDTO> getAdminUsersApi(@RequestParam(required = false) String keyword) {
        return userService.getUsersForAdmin(keyword);
    }

    @DeleteMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteUserApi(@PathVariable Long id, Authentication authentication) {
        try {
            Long adminId = userService.getId(authentication);
            userService.deleteCustomerByAdmin(adminId, id);
            return ResponseEntity.ok("Deleted");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/cars")
    @ResponseBody
    public List<CarDTO> getAdminCarsApi(@RequestParam(required = false) String keyword, @RequestParam(required = false) Long brandId) {
        return carService.getCarsForAdmin(keyword, brandId);
    }

    @DeleteMapping("/api/cars/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteCarApi(@PathVariable Long id) {
        try {
            carService.deleteCarForAdmin(id);
            return ResponseEntity.ok("Deleted");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
