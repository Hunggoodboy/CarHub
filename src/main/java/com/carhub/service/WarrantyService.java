package com.carhub.service;

import com.carhub.entity.WarrantyTicket;
import com.carhub.entity.OrderDetail;
import com.carhub.entity.Customer;
import com.carhub.repository.WarrantyTicketRepository;
import com.carhub.repository.OrderDetailRepository;
import com.carhub.dto.Request.WarrantyRequest;
import com.carhub.dto.Response.WarrantyResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarrantyService {

    private final WarrantyTicketRepository warrantyRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Transactional
    public void createWarrantyTicket(WarrantyRequest request, Long userId, Customer customer) {

        OrderDetail orderDetail = orderDetailRepository
                .findCompletedOrderDetailByCarIdAndUserId(userId, request.getCarId())
                .orElseThrow(() -> new RuntimeException("Điều kiện bảo hành không thỏa mãn."));

        WarrantyTicket ticket = new WarrantyTicket();
        ticket.setCar(orderDetail.getCar());
        ticket.setCustomer(customer);
        ticket.setOrder(orderDetail.getOrder());
        ticket.setPayment(orderDetail.getOrder().getPayment());

        ticket.setStreet(request.getStreet());
        ticket.setWard(request.getWard());
        ticket.setCity(request.getCity());
        ticket.setPhone(request.getPhone());
        ticket.setDefectDescription(request.getDefectDescription());

        ticket.setStatus("PENDING");
        ticket.setReceivedDate(new Date());

        warrantyRepository.save(ticket);
    }

    private WarrantyResponseDTO mapToDTO(WarrantyTicket w) {
        WarrantyResponseDTO dto = new WarrantyResponseDTO();

        dto.setId(w.getId());
        dto.setCarModel(w.getCar().getModel());
        dto.setCustomerName(w.getCustomer().getFullName());
        dto.setPhone(w.getPhone());
        dto.setStreet(w.getStreet());
        dto.setWard(w.getWard());
        dto.setCity(w.getCity());
        dto.setDefectDescription(w.getDefectDescription());
        dto.setStatus(w.getStatus());
        dto.setReceivedDate(w.getReceivedDate());
        dto.setCustomerConfirmed(w.getCustomerConfirmed());
        dto.setSellerConfirmed(w.getSellerConfirmed());

        return dto;
    }

    public List<WarrantyResponseDTO> getMyWarranty(Long customerId) {
        return warrantyRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<WarrantyResponseDTO> getSellerWarranty(Long sellerId) {
        return warrantyRepository.findAll()
                .stream()
                .filter(w -> w.getCar().getSeller().getId().equals(sellerId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    @Transactional
public void confirmSeller(Long id) {
    WarrantyTicket ticket = warrantyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Not found"));

    ticket.setSellerConfirmed(true);

    // Khi seller xác nhận ->bảo hành thành công
    ticket.setStatus("SUCCESS");

    // Nếu cả 2 đã xác nhận ->COMPLETED
    if (Boolean.TRUE.equals(ticket.getCustomerConfirmed())) {
        ticket.setStatus("COMPLETED");
    }

    warrantyRepository.save(ticket);
}
@Transactional
public void confirmCustomer(Long id) {
    WarrantyTicket ticket = warrantyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Not found"));

    ticket.setCustomerConfirmed(true);

    // Nếu cả 2 đã xác nhận -> COMPLETED
    if (Boolean.TRUE.equals(ticket.getSellerConfirmed())) {
        ticket.setStatus("COMPLETED");
    }

    warrantyRepository.save(ticket);
}
}