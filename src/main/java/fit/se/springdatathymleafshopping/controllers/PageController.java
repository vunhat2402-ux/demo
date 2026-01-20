package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Contact;
import fit.se.springdatathymleafshopping.repositories.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {

    @Autowired
    private ContactRepository contactRepository;

    @GetMapping("/about")
    public String about() { return "pages/about"; }

    // --- XỬ LÝ TRANG LIÊN HỆ ---
    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("contact", new Contact()); // Để hứng dữ liệu form
        return "pages/contact";
    }

    @PostMapping("/contact/send")
    public String sendContact(@ModelAttribute Contact contact) {
        contactRepository.save(contact);
        return "redirect:/contact?success"; // Reload trang và báo thành công
    }

    // --- XỬ LÝ TRANG CHÍNH SÁCH ---
    @GetMapping("/policy")
    public String policy() { return "pages/policy"; }
}