package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.entities.Comment;
import fit.se.springdatathymleafshopping.repositories.CommentRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CommentService {
    private final CommentRepository repo;
    public CommentService(CommentRepository repo) { this.repo = repo; }
    public List<Comment> findAll() { return repo.findAll(); }
    public Comment save(Comment c) { return repo.save(c); }
}
