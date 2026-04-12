package com.carhub.controller;

import com.carhub.dto.Request.WarrantyRequest;
import com.carhub.entity.Customer;
import com.carhub.entity.OrderDetail;
import com.carhub.repository.OrderDetailRepository;
import com.carhub.repository.WarrantyTicketRepository;
import com.carhub.service.Car.WarrantyService;

import com.carhub.service.authentication.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.carhub.dto.Response.WarrantyResponseDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.List;

@Controller
@RequestMapping("/api/warranty")
@RequiredArgsConstructor
public class WarrantyController {

    private final OrderDetailRepository orderDetailRepository;
    private final WarrantyTicketRepository warrantyTicketRepository;
    private final UserService userService;
    private final WarrantyService warrantyService;
    // Trang tạo yêu cầu bảo hành cho 1 xe cụ thể (chỉ cho xe đã mua và đơn đã hoàn tất)
    @GetMapping("/request")
    public String warrantyRequestPage(@RequestParam("carId") Long carId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);

        OrderDetail orderDetail = orderDetailRepository
                .findCompletedOrderDetailByCarIdAndUserId(userId, carId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa mua xe này hoặc đơn hàng chưa được hoàn tất, không thể bảo hành."));

        // Truyền thông tin cơ bản của xe sang view (nếu cần hiển thị)
        model.addAttribute("carId", carId);
        model.addAttribute("carModel", orderDetail.getCar().getModel());
        model.addAttribute("carBrand", orderDetail.getCar().getBrand().getName());
        model.addAttribute("carYear", orderDetail.getCar().getManufactureYear());

        return "warranty";
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createWarranty(@RequestBody WarrantyRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);

        Customer customer = userService.getCurrentCustomer(); // bạn cần có hàm này

        warrantyService.createWarrantyTicket(request, userId, customer);

        return ResponseEntity.ok(Map.of("message","Tạo yêu cầu bảo hành thành công"));
    }
    @GetMapping("/my")
    @ResponseBody
    public List<WarrantyResponseDTO> getMyWarranty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(auth);

        return warrantyService.getMyWarranty(userId);
    }
    @GetMapping("/seller")
    @ResponseBody
    public List<WarrantyResponseDTO> getSellerWarranty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long sellerId = userService.getId(auth);

        return warrantyService.getSellerWarranty(sellerId);
    }
    @PutMapping("/{id}/accept")
    @ResponseBody
    public ResponseEntity<?> acceptWarranty(@PathVariable Long id) {
        warrantyService.acceptWarranty(id);
        return ResponseEntity.ok("Accepted");
    }

    @PutMapping("/{id}/confirm-seller")
    @ResponseBody
    public ResponseEntity<?> confirmSeller(@PathVariable Long id) {
        warrantyService.confirmSeller(id);
        return ResponseEntity.ok("Seller confirmed");
    }
    
    @PutMapping("/{id}/confirm-customer")
    @ResponseBody
    public ResponseEntity<?> confirmCustomer(@PathVariable Long id) {
        warrantyService.confirmCustomer(id);
        return ResponseEntity.ok("Customer confirmed");
    }
}

