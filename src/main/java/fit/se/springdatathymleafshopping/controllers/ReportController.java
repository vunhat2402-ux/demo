package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Booking;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.repositories.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/report")
public class ReportController {

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String showRevenueReport(Model model) {
        // Lấy tất cả booking, lọc những booking có status hợp lệ (PAID hoặc DEPOSITED)
        List<Booking> validBookings = bookingRepository.findAll().stream()
                .filter(b -> {
                    BookingStatus s = b == null ? null : b.getStatus();
                    return s == BookingStatus.PAID || s == BookingStatus.DEPOSITED;
                })
                .collect(Collectors.toList());

        // Tính tổng doanh thu (totalAmount) bằng BigDecimal
        BigDecimal totalRevenue = validBookings.stream()
                .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính thực thu (paidAmount) bằng BigDecimal
        BigDecimal actualCashReceived = validBookings.stream()
                .map(b -> b.getPaidAmount() == null ? BigDecimal.ZERO : b.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("bookings", validBookings);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("actualReceived", actualCashReceived);

        return "admin/report";
    }
}
