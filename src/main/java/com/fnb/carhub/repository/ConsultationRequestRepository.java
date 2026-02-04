package com.fnb.carhub.repository;

import com.fnb.carhub.entity.Admin;
import com.fnb.carhub.entity.ConsultationRequest;
import com.fnb.carhub.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {
    List<ConsultationRequest> findByCustomer(Customer customer);
    List<ConsultationRequest> findByCustomerId(Long customerId);
    List<ConsultationRequest> findByAdmin(Admin admin);
    List<ConsultationRequest> findByAdminId(Long adminId);
    List<ConsultationRequest> findByStatus(String status);
    List<ConsultationRequest> findBySendDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<ConsultationRequest> findByStatusOrderBySendDateDesc(String status);
}