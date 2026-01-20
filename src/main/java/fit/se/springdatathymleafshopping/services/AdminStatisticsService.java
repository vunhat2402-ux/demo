package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminStatisticsService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private ConsultationRequestRepository requestRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Tổng doanh thu tháng này (Dùng hàm calculateRevenue)
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        Double revenue = bookingRepository.calculateRevenue(startOfMonth, now);
        stats.put("monthlyRevenue", revenue != null ? revenue : 0.0);

        // 2. Đếm yêu cầu tư vấn chưa xử lý (Dùng hàm findByIsProcessedFalse...)
        long pendingRequests = requestRepository.countByIsProcessedFalse(); // Bạn cần thêm hàm count này hoặc dùng list.size()
        stats.put("pendingRequests", pendingRequests);

        return stats;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        // 1. Doanh thu tháng (Tận dụng hàm calculateRevenue)
        Double revenue = bookingRepository.calculateRevenue(start, end);
        stats.put("revenue", revenue != null ? revenue : 0);

        // 2. Đơn cần xử lý (Tận dụng hàm findByStatus)
        stats.put("pendingBookings", bookingRepository.countByStatus("PENDING"));

        return stats;
    }
}