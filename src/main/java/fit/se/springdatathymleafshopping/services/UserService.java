package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.entities.User;
import fit.se.springdatathymleafshopping.repositories.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {
    private final UserRepository repo;
    public UserService(UserRepository repo) { this.repo = repo; }
    public List<User> findAll() { return repo.findAll(); }
    public User findByEmail(String username) { return repo.findByEmail(username).orElse(null); }
    public User save(User u) { return repo.save(u); }
}
