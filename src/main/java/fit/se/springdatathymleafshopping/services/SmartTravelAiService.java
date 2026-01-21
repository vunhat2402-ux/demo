package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.entities.DepartureSchedule;
import fit.se.springdatathymleafshopping.entities.Tour;
import fit.se.springdatathymleafshopping.repositories.TourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmartTravelAiService {

    @Value("${spring.ai.gemini.api-key}")
    private String apiKey;

    @Autowired
    private TourRepository tourRepository;

    private final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    public String getSmartResponse(String userQuestion) {
        RestTemplate restTemplate = new RestTemplate();
        String finalUrl = API_URL + apiKey;

        // 1. CHUẨN BỊ DỮ LIỆU CHI TIẾT (Lấy cả Lịch trình + Điểm đến)
        List<Tour> tours = tourRepository.findAll();

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("DANH SÁCH TOUR HIỆN CÓ:\n");

        for (Tour t : tours) {
            if (t.getIsActive() != null && t.getIsActive()) {
                String priceStr = "Liên hệ để biết giá";

                // Lấy giá nhỏ nhất (BigDecimal) an toàn với null
                if (t.getSchedules() != null && !t.getSchedules().isEmpty()) {
                    BigDecimal minPrice = t.getSchedules().stream()
                            .map(DepartureSchedule::getPriceAdult)
                            .filter(Objects::nonNull)
                            .min(Comparator.naturalOrder())
                            .orElse(BigDecimal.ZERO);
                    if (minPrice.compareTo(BigDecimal.ZERO) > 0) {
                        priceStr = formatMoney(minPrice);
                    }
                }

                // Lấy tóm tắt lịch trình (Ví dụ: Ngày 1: ..., Ngày 2: ...)
                String itinerarySummary = "Chưa cập nhật lịch trình";
                if (t.getItineraries() != null && !t.getItineraries().isEmpty()) {
                    itinerarySummary = t.getItineraries().stream()
                            .map(i -> "Ngày " + i.getDayNumber() + ": " + i.getTitle())
                            .collect(Collectors.joining("; "));
                }

                contextBuilder.append(String.format("""
                    ---
                    [ID: %d] Tên Tour: %s
                    - Điểm đến: %s
                    - Thời lượng: %s
                    - Giá từ: %s
                    - Lịch trình tóm tắt: %s
                    - Mô tả: %s
                    """,
                        t.getId(),
                        t.getName(),
                        (t.getDestination() != null ? t.getDestination().getName() : "Nhiều điểm"),
                        t.getDuration(),
                        priceStr,
                        itinerarySummary,
                        (t.getDescription() != null && t.getDescription().length() > 100)
                                ? t.getDescription().substring(0, 100) + "..."
                                : (t.getDescription() == null ? "" : t.getDescription())
                ));
            }
        }

        // 2. KỊCH BẢN "TRAINING" (Prompt Engineering)
        String systemInstruction = """
            VAI TRÒ: Bạn là Chuyên gia tư vấn du lịch cao cấp của Smart Travel.
            
            NGUYÊN TẮC TRẢ LỜI:
            1. Luôn tỏ ra thân thiện, nhiệt tình và chuyên nghiệp.
            2. Chỉ tư vấn dựa trên "DANH SÁCH TOUR HIỆN CÓ" ở trên. Tuyệt đối không bịa đặt tour không có trong dữ liệu.
            3. Nếu khách hỏi chung chung (ví dụ: "đi đâu chơi?"), hãy hỏi ngược lại về sở thích hoặc ngân sách để tư vấn chính xác.
            4. Khi báo giá, luôn kèm câu "Giá có thể thay đổi tùy ngày khởi hành, bạn hãy bấm vào chi tiết để xem chính xác nhé".
            5. Cuối câu trả lời nên có câu kêu gọi hành động (Call to Action), ví dụ: "Bạn có muốn mình gửi link đặt tour này không?".
            6. Trả lời ngắn gọn, súc tích (dưới 150 từ) để khách dễ đọc trên điện thoại.
            
            DỮ LIỆU ĐẦU VÀO:
            """ + contextBuilder.toString();

        // 3. Gửi Request
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", systemInstruction + "\n\nKhách hàng: " + userQuestion);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", Collections.singletonList(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(parts));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(finalUrl, entity, Map.class);
            return extractResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return "Hệ thống AI đang bận. Bạn vui lòng liên hệ hotline 1900 1234 để được hỗ trợ ngay lập tức.";
        }
    }

    private String extractResponse(ResponseEntity<Map> response) {
        try {
            Map body = response.getBody();
            if (body != null && body.containsKey("candidates")) {
                List<Map> candidates = (List<Map>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Object contentObj = candidates.get(0).get("content");
                    if (contentObj instanceof Map) {
                        Map contentRes = (Map) contentObj;
                        Object partsObj = contentRes.get("parts");
                        if (partsObj instanceof List) {
                            List parts = (List) partsObj;
                            if (!parts.isEmpty() && parts.get(0) instanceof Map) {
                                Map firstPart = (Map) parts.get(0);
                                Object text = firstPart.get("text");
                                if (text instanceof String) return (String) text;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore and fallback
        }
        return "Xin lỗi, tôi chưa hiểu ý bạn. Bạn có thể hỏi lại rõ hơn không?";
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "";
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }
}
