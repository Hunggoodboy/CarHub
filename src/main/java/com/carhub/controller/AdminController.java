package com.carhub.controller;

import com.carhub.dto.CarDTO;
import com.carhub.dto.UserDTO;
import com.carhub.service.Car.CarService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CarService carService;

    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/users";
    }

    @GetMapping("/users")
    public String adminUsers(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("activePage", "users");
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("users", userService.getUsersForAdmin(keyword));
        return "admin-users";
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
            return "redirect:/admin/users";
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
            redirectAttributes.addFlashAttribute("successMessage", "XÃ³a user thÃ nh cÃ´ng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/cars")
    public String adminCars(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long brandId,
                            Model model) {
        model.addAttribute("activePage", "cars");
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("selectedBrandId", brandId);
        model.addAttribute("brands", carService.getBrandsForAdmin());
        model.addAttribute("cars", carService.getCarsForAdmin(keyword, brandId));
        return "admin-cars";
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
            redirectAttributes.addFlashAttribute("successMessage", "Cáº­p nháº­t xe thÃ nh cÃ´ng.");
            return "redirect:/admin/cars/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/cars";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/cars/" + id + "/edit";
        }
    }

    @PostMapping("/cars/{id}/delete")
    public String deleteCar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            carService.deleteCarForAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "XÃ³a xe thÃ nh cÃ´ng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/cars";
    }
    
}
