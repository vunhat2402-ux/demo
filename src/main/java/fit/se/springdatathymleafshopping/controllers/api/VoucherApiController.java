package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.repositories.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherApiController {

    @Autowired
    private VoucherRepository voucherRepository;

    @GetMapping("/check")
    public ResponseEntity<?> checkVoucher(@RequestParam String code) {
        // Tận dụng hàm findByCodeAndExpiryDateAfter
        return voucherRepository.findByCodeAndExpiryDateAfter(code, LocalDate.now())
                .map(v -> ResponseEntity.ok(Map.of(
                        "valid", true,
                        "discount", v.getDiscountAmount(),
                        "type", v.getDiscountType() // PERCENT or FIXED
                )))
                .orElse(ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Mã không hợp lệ hoặc đã hết hạn")));
    }
}