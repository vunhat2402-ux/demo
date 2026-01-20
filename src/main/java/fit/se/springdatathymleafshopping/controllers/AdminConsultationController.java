package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.ConsultationRequest;
import fit.se.springdatathymleafshopping.repositories.ConsultationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/consultations")
public class AdminConsultationController {

    @Autowired private ConsultationRequestRepository consultationRepo;

    @GetMapping
    public String listRequests(Model model) {
        // Lấy danh sách, mới nhất lên đầu
        model.addAttribute("requests", consultationRepo.findAll(Sort.by(Sort.Direction.DESC, "createdDate")));
        return "admin/consultation-list";
    }

    // Đánh dấu là đã xử lý (đã gọi điện)
    @GetMapping("/process/{id}")
    public String markAsProcessed(@PathVariable Integer id) {
        ConsultationRequest req = consultationRepo.findById(id).orElse(null);
        if(req != null) {
            req.setIsProcessed(true);
            consultationRepo.save(req);
        }
        return "redirect:/admin/consultations";
    }
}