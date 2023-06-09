package be.thomasmore.toydoc.controllers;

import be.thomasmore.toydoc.model.AppUser;
import be.thomasmore.toydoc.model.Liking;
import be.thomasmore.toydoc.model.Post;
import be.thomasmore.toydoc.repositories.AppUserRepository;
import be.thomasmore.toydoc.repositories.LikingRepository;
import be.thomasmore.toydoc.repositories.PostRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
public class PostController {

    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private LikingRepository likingRepository;

    @GetMapping({"/postlist", "/postlist{something}", "/postlist/{filter}"})
    public String postList(Model model, @RequestParam(required = false)String keyword,
                                        @RequestParam(required = false)String speciality) {

        List<Post> allPosts;

        if( keyword == null && speciality == null){
            allPosts = postRepository.findAll();
        }
        else{
            allPosts = postRepository.findBySpecialty(speciality.trim());
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("speciality",speciality);
        model.addAttribute("posts", allPosts);
        return "postlist";
    }

    @GetMapping({"/postdetail/{id}" , "/postdetail"})
    public String postDetails(Model model, @PathVariable(required = false) Integer id) {
        if (id == null) return "postdetail";
        Optional<Post> optionalPost = postRepository.findById(id);
        Optional<Post> optionalPrev = postRepository.findFirstByIdLessThanOrderByIdDesc(id);
        Optional<Post> optionalNext = postRepository.findFirstByIdGreaterThanOrderById(id);

        if (optionalPost.isPresent()) {
            model.addAttribute("post", optionalPost.get());
        }

        if (optionalPrev.isPresent()){
            model.addAttribute("prev", optionalPrev.get().getId());
        }else {
            model.addAttribute("prev", postRepository.findFirstByOrderByIdDesc().get().getId());
        }

        if (optionalNext.isPresent()){
            model.addAttribute("next", optionalNext.get().getId());
        }else {
            model.addAttribute("next", postRepository.findFirstByOrderByIdAsc().get().getId());
        }
        return "postdetail";
    }




    @GetMapping("/posts/new")
    public String createNewPost(Model model) {
        Post post = new Post();
        model.addAttribute("post", post);
        return "post_new";
    }

    @PostMapping("/posts/postnew")
    public String saveNewPost(Model model, HttpServletRequest request,
                              @RequestParam(required = false) String title,
                              @RequestParam(required = false) String intro,
                              @RequestParam(required = false) String body,
                              @RequestParam(required = false) String beforeUrl,
                              @RequestParam(required = false) String afterUrl,
                              @RequestParam(required = false) String specialty,
                              @RequestParam(required = false) Date date){
        AppUser appUser = (AppUser) request.getAttribute("appUser");
        Post post = new Post(title, beforeUrl, afterUrl, intro, body, specialty, Date.from(java.time.LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        postRepository.save(post);
        return "redirect:/postlist";
    }

    @PostMapping("/like")
    public ResponseEntity<Integer> likeUpload(@RequestParam("postId") int postId, HttpServletRequest request){

        AppUser appUser = (AppUser) request.getAttribute("appUser");
        Post post = postRepository.findById(postId).orElse(null);

        //Upload upload = uploadRepository.findById(uploadId).orElse(null);
        boolean alreadyLiked = likingRepository.existsByPostIdAndAppUser(post.getId(), appUser.getId());
        //boolean alreadyLiked = likeRepository.existsByUploadIdAndUserId(upload.getId(), loggedInUser.getId());

        if (alreadyLiked) {
            List<Liking> likes = likingRepository.findByPostIdAndAppUser(post.getId(), appUser.getId());
            likingRepository.deleteAll(likes);
            post.setLikeCount(post.getLikeCount() - 1);
            System.out.println("-1");
            post.setLikedByCurrentAppUser(false);

        } else {
            Liking like = new Liking();
            like.setAppUser(appUser);
            like.setPost(post);
            likingRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            System.out.println("+1");
            post.setLikedByCurrentAppUser(true);
        }
        postRepository.save(post);
        int updatedLikeCount = postRepository.findLikeCountById(postId);
        return ResponseEntity.ok().body(updatedLikeCount);
    }
}
