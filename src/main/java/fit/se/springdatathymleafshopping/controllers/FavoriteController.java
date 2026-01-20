package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Favorite;
import fit.se.springdatathymleafshopping.entities.User;
import fit.se.springdatathymleafshopping.repositories.FavoriteRepository;
import fit.se.springdatathymleafshopping.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class FavoriteController {
    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/favorites")
    public String myFavorites(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).get();

        List<Favorite> favorites = favoriteRepository.findByUserId(user.getId());
        model.addAttribute("favorites", favorites);
        return "favorites"; // file favorites.html
    }
}