package com.carhub.controller;

import com.carhub.entity.OrderDetail;
import com.carhub.entity.WarrantyTicket;
import com.carhub.repository.OrderDetailRepository;
import com.carhub.repository.WarrantyTicketRepository;
import com.carhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WarrantyController {

    private final OrderDetailRepository orderDetailRepository;
    private final WarrantyTicketRepository warrantyTicketRepository;
    private final UserService userService;

    // Trang tạo yêu cầu bảo hành cho 1 xe cụ thể (chỉ cho xe đã mua và đơn đã hoàn tất)
    @GetMapping("/warranty/request")
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

    // API tạo phiếu bảo hành từ form trên trang warranty
    @PostMapping("/warranty/create")
    public ResponseEntity<?> createWarranty(@RequestBody Map<String, String> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);

        Long carId = Long.valueOf(body.get("carId"));
        String errorCar = body.get("errorCar");

        OrderDetail orderDetail = orderDetailRepository
                .findCompletedOrderDetailByCarIdAndUserId(userId, carId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa mua xe này hoặc đơn hàng chưa được hoàn tất, không thể bảo hành."));

        WarrantyTicket ticket = new WarrantyTicket();
        ticket.setDefectDescription(errorCar);
        ticket.setStatus("PENDING");
        ticket.setReceivedDate(new Date());
        ticket.setCustomer(orderDetail.getOrder().getCustomer());
        ticket.setPayment(orderDetail.getOrder().getPayment());

        warrantyTicketRepository.save(ticket);

        return ResponseEntity.ok(Map.of("message", "Tạo yêu cầu bảo hành thành công"));
    }
}

