package com.fnb.carhub.repository;

import com.fnb.carhub.entity.Admin;
import com.fnb.carhub.entity.Customer;
import com.fnb.carhub.entity.WarrantyTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyTicketRepository extends JpaRepository<WarrantyTicket, Long> {
    List<WarrantyTicket> findByCustomer(Customer customer);
    List<WarrantyTicket> findByCustomerId(Long customerId);
    List<WarrantyTicket> findByAdmin(Admin admin);
    List<WarrantyTicket> findByAdminId(Long adminId);
    List<WarrantyTicket> findByStatus(String status);
    Optional<WarrantyTicket> findByLicensePlate(String licensePlate);
    List<WarrantyTicket> findByReceivedDateBetween(Date startDate, Date endDate);
}