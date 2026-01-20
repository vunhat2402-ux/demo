package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.services.SmartTravelAiService; // Import service mới
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
public class AiController {

    @Autowired
    private SmartTravelAiService aiService; // Dùng service mới

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String answer = aiService.getSmartResponse(question);

        Map<String, String> response = new HashMap<>();
        response.put("answer", answer);
        return ResponseEntity.ok(response);
    }

    // 2. Xử lý câu hỏi từ Form (Method GET theo file chat-form.html của bạn)
    @GetMapping("/ask")
    public String askAI(@RequestParam("prompt") String prompt, Model model) {
        String response = aiService.getSmartResponse(prompt);
        model.addAttribute("prompt", prompt);
        model.addAttribute("response", response);
        return "ai/ai-chat";
    }
}