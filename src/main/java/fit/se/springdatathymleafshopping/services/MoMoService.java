package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.entities.Booking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class MoMoService {

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.redirect-url}")
    private String redirectUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    public String createPaymentUrl(Booking booking) {
        try {
            // Tạo mã đơn hàng duy nhất bằng cách thêm thời gian vào sau mã BK
            String orderId = booking.getBookingCode() + "_" + System.currentTimeMillis();
            String requestId = orderId;
            String orderInfo = "Thanh toan tour " + booking.getBookingCode();

            // Số tiền phải là số nguyên (không có thập phân)
            String amount = String.valueOf(booking.getTotalAmount().longValue());

            String requestType = "captureWallet"; // Luồng chuẩn cho Web: Hiện mã QR
            String extraData = "";
            String lang = "vi";

            // 1. Tạo chuỗi dữ liệu thô để ký (Raw Signature)
            // QUAN TRỌNG: Thứ tự các tham số phải đúng alphabet a->z
            String rawSignature = "accessKey=" + accessKey
                    + "&amount=" + amount
                    + "&extraData=" + extraData
                    + "&ipnUrl=" + ipnUrl
                    + "&orderId=" + orderId
                    + "&orderInfo=" + orderInfo
                    + "&partnerCode=" + partnerCode
                    + "&redirectUrl=" + redirectUrl
                    + "&requestId=" + requestId
                    + "&requestType=" + requestType;

            // 2. Ký bằng HMAC-SHA256
            String signature = hmacSHA256(rawSignature, secretKey);

            // 3. Tạo JSON Body gửi đi
            Map<String, Object> map = new HashMap<>();
            map.put("partnerCode", partnerCode);
            map.put("partnerName", "Smart Travel");
            map.put("storeId", "SmartTravelStore");
            map.put("requestId", requestId);
            map.put("amount", amount);
            map.put("orderId", orderId);
            map.put("orderInfo", orderInfo);
            map.put("redirectUrl", redirectUrl);
            map.put("ipnUrl", ipnUrl);
            map.put("lang", lang);
            map.put("requestType", requestType);
            map.put("autoCapture", true);
            map.put("extraData", extraData);
            map.put("signature", signature);

            // 4. Gửi Request sang MoMo
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(map, headers);

            System.out.println("Gửi yêu cầu MoMo: " + map); // Log để kiểm tra

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
            Map<String, Object> body = response.getBody();

            System.out.println("Kết quả từ MoMo: " + body); // Log kết quả

            if (body != null && body.containsKey("payUrl")) {
                return body.get("payUrl").toString(); // Trả về link thanh toán
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSHA256.init(secretKeySpec);
        byte[] bytes = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}