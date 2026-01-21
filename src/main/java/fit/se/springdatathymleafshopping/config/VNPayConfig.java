package fit.se.springdatathymleafshopping.config;

import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
public class VNPayConfig {
    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    // 👇 MÃ CẤU HÌNH CỦA BẠN (Giữ nguyên như bạn đã gửi)
    public static String vnp_TmnCode = "4Q8FAARK";
    public static String vnp_HashSecret = "Y44ZJ7AUBVWZ21WHT68KHVKK1BT3AMQ8";

    // Hàm mã hóa HmacSHA512
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) throw new NullPointerException();
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    // Hàm tạo số ngẫu nhiên (nếu cần dùng)
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    // 👇 [QUAN TRỌNG] HÀM BẠN ĐANG THIẾU ĐÂY 👇
    // Hàm này dùng để sắp xếp và hash lại dữ liệu trả về từ VNPAY để kiểm tra bảo mật
    public static String hashAllFields(Map fields) {
        List fieldNames = new ArrayList(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                try {
                    // VNPAY yêu cầu encode giá trị bằng UTF-8 trước khi hash
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        // Hash chuỗi vừa tạo với SecretKey của bạn
        return hmacSHA512(vnp_HashSecret, sb.toString());
    }
}