package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Tour;
import fit.se.springdatathymleafshopping.entities.TourImage;
import fit.se.springdatathymleafshopping.entities.TourItinerary;
import fit.se.springdatathymleafshopping.repositories.TourImageRepository;
import fit.se.springdatathymleafshopping.repositories.TourItineraryRepository;
import fit.se.springdatathymleafshopping.repositories.TourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Controller
@RequestMapping("/admin/tour-extras")
public class AdminTourExtraController {

    @Autowired private TourRepository tourRepo;
    @Autowired private TourImageRepository imageRepo;
    @Autowired private TourItineraryRepository itineraryRepo;

    // --- QUẢN LÝ LỊCH TRÌNH (ITINERARY) ---
    @GetMapping("/itinerary/{tourId}")
    public String viewItinerary(@PathVariable Integer tourId, Model model) {
        Tour tour = tourRepo.findById(tourId).orElseThrow();
        model.addAttribute("tour", tour);
        model.addAttribute("itineraries", itineraryRepo.findByTourIdOrderByDayNumberAsc(tourId));
        model.addAttribute("newItinerary", new TourItinerary()); // Object form
        return "admin/tour-itinerary";
    }

    @PostMapping("/itinerary/save")
    public String saveItinerary(@ModelAttribute TourItinerary itinerary, @RequestParam("tourId") Integer tourId) {
        itinerary.setTour(tourRepo.findById(tourId).get());
        itineraryRepo.save(itinerary);
        return "redirect:/admin/tour-extras/itinerary/" + tourId;
    }

    // --- QUẢN LÝ THƯ VIỆN ẢNH (GALLERY) ---
    @GetMapping("/gallery/{tourId}")
    public String viewGallery(@PathVariable Integer tourId, Model model) {
        model.addAttribute("tour", tourRepo.findById(tourId).get());
        model.addAttribute("images", imageRepo.findByTourId(tourId));
        return "admin/tour-gallery";
    }

    @PostMapping("/gallery/upload")
    public String uploadImages(@RequestParam("tourId") Integer tourId,
                               @RequestParam("files") List<MultipartFile> files) throws IOException {
        Tour tour = tourRepo.findById(tourId).get();
        Path uploadPath = Paths.get("./uploads/gallery");
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

                TourImage img = new TourImage();
                img.setImageUrl(fileName);
                img.setTour(tour);
                imageRepo.save(img);
            }
        }
        return "redirect:/admin/tour-extras/gallery/" + tourId;
    }
}