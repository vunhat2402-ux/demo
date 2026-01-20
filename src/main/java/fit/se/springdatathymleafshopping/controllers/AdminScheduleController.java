package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.DepartureSchedule;
import fit.se.springdatathymleafshopping.entities.Tour;
import fit.se.springdatathymleafshopping.repositories.DepartureScheduleRepository;
import fit.se.springdatathymleafshopping.repositories.TourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/schedules")
public class AdminScheduleController {

    @Autowired private DepartureScheduleRepository scheduleRepo;
    @Autowired private TourRepository tourRepo;

    // 1. Xem danh sách lịch khởi hành của 1 Tour cụ thể
    @GetMapping("/tour/{tourId}")
    public String viewSchedules(@PathVariable Integer tourId, Model model) {
        Tour tour = tourRepo.findById(tourId).orElseThrow();
        model.addAttribute("tour", tour);
        // Lấy danh sách lịch, sắp xếp ngày gần nhất lên đầu (Logic trong Repo)
        model.addAttribute("schedules", scheduleRepo.findFutureSchedulesByTour(tourId));
        return "admin/schedule-list"; // Cần tạo view này
    }

    // 2. Hiện form thêm ngày khởi hành mới
    @GetMapping("/add/{tourId}")
    public String showAddForm(@PathVariable Integer tourId, Model model) {
        DepartureSchedule schedule = new DepartureSchedule();
        schedule.setTour(tourRepo.findById(tourId).get()); // Gán tour trước
        model.addAttribute("schedule", schedule);
        return "admin/schedule-form"; // Cần tạo view này
    }

    // 4. Xóa lịch (Chỉ xóa khi chưa có ai đặt)
    @GetMapping("/delete/{id}")
    public String deleteSchedule(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        DepartureSchedule schedule = scheduleRepo.findById(id).orElseThrow();
        if (schedule.getBooked() > 0) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa lịch đã có khách đặt!");
        } else {
            scheduleRepo.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Đã xóa lịch khởi hành.");
        }
        return "redirect:/admin/schedules/tour/" + schedule.getTour().getId();
    }
}