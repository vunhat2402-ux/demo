package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.entities.enums.BookingStatus; // 👈 Nhớ Import dòng này
import fit.se.springdatathymleafshopping.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminStatisticsService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private ConsultationRequestRepository requestRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Tổng doanh thu tháng này
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        Double revenue = bookingRepository.calculateRevenue(startOfMonth, now);
        stats.put("monthlyRevenue", revenue != null ? revenue : 0.0);

        // 2. Đếm yêu cầu tư vấn chưa xử lý
        long pendingRequests = requestRepository.countByIsProcessedFalse();
        stats.put("pendingRequests", pendingRequests);

        // 3. Đếm đơn đặt Tour đang chờ duyệt
        long pendingOrders = bookingRepository.countByStatus(BookingStatus.PENDING);

        stats.put("pendingOrders", pendingOrders);

        return stats;
    }
}