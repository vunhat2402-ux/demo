package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Booking;
import fit.se.springdatathymleafshopping.repositories.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/report")
public class ReportController {

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String showRevenueReport(Model model) {
        // Chỉ lấy các đơn đã hoàn tất thanh toán hoặc đã cọc
        List<Booking> validBookings = bookingRepository.findAll().stream()
                .filter(b -> "PAID_FULL".equals(b.getStatus()) || "DEPOSITED".equals(b.getStatus()))
                .collect(Collectors.toList());

        // Tính tổng dựa trên trường totalAmount đã được tính sẵn lúc đặt tour
        // Không cần loop tính lại từng người nữa -> Nhanh và chính xác hơn
        Double totalRevenue = validBookings.stream()
                .mapToDouble(Booking::getTotalAmount)
                .sum();

        // Tính thực thu (Tiền mặt đã cầm)
        Double actualCashReceived = validBookings.stream()
                .mapToDouble(Booking::getPaidAmount)
                .sum();

        model.addAttribute("bookings", validBookings);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("actualReceived", actualCashReceived);

        return "admin/report";
    }
}