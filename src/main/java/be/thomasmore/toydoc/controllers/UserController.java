package be.thomasmore.toydoc.controllers;
import be.thomasmore.toydoc.model.AppUser;
import be.thomasmore.toydoc.model.Appointment;
import be.thomasmore.toydoc.model.Role;
import be.thomasmore.toydoc.repositories.AppUserRepository;
import be.thomasmore.toydoc.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


import java.security.Principal;
import java.util.List;
import java.util.Optional;


@Controller
@RequestMapping("/user")
public class UserController {

    // Logger voor deze klasse
    private Logger logger = LoggerFactory.getLogger(UserController.class);

    EmailService emailService;

    public UserController(EmailService emailService) {
        this.emailService = emailService;
    }

    @Autowired
    AppUserRepository appUserRepository;


    @PostMapping("/login/success")
    public String loginSuccess(Principal principal, Model model,HttpServletRequest request) {

        AppUser loggedinUser = (AppUser) request.getAttribute("appUser");

        if(loggedinUser.getRole().equals(Role.ADMIN)){
            return "redirect:/admin/dashboard";
        }
        if (loggedinUser.getRole().equals(Role.DOCTOR)){
            return "redirect:/dashboard/profile/" + loggedinUser.getId();
        }
        if (loggedinUser.getRole().equals(Role.CLIENT)){
            return "redirect:/dashboard/profile/" + loggedinUser.getId();
        }

        model.addAttribute("user",new AppUser());
        return "/home";
    }

    @GetMapping("/login")
    public String register(Principal principal, Model model) {

        //===========CREDENTIALS VOOR DE DEVELOPERS OP DE LOGIN PAGE
        List<AppUser> userList = (List<AppUser>) appUserRepository.findAll();
        AppUser[] userArray = userList.toArray(new AppUser[userList.size()]);
        model.addAttribute("APPUSERS",userArray);
        //===========CREDENTIALS VOOR DE DEVELOPERS OP DE LOGIN PAGE

        // Als er al een gebruiker ingelogd is, ga dan naar home pagina

        if (principal != null) return "redirect:/home";

        // Toon de login pagina
//        model.addAttribute("user",new AppUser());
        return "user/login";
    }

    @GetMapping("/signup")
    public String loginReversed(Principal principal, Model model) {


        // Als er al een gebruiker ingelogd is, ga dan naar home pagina
        if (principal != null){
            return "redirect:/home";
        }






        // Toon de login pagina
        model.addAttribute("user",new AppUser());
        return "user/signup";
    }



    @PostMapping("/signup-user")
    public String signUp(@RequestParam("birthDateStr") String birthDateStr, AppUser user, Model model) {

        Date birthDate;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            birthDate = dateFormat.parse(birthDateStr);
        } catch (ParseException e) {
            // Handle parsing exception
            return "error";
        }


        user.setBirthDate(birthDate);

        AppUser existingUser = appUserRepository.findByUsername(user.getUsername());
        AppUser existingUserEmail = appUserRepository.findByEmail(user.getEmail());

        if (existingUser != null) {
            model.addAttribute("errorMessage", "Username already taken");
            return "user/signup";
        }
        if (existingUserEmail != null) {
            model.addAttribute("errorMessage", "Email already taken");
            return "user/signup";
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole(Role.CLIENT);
        appUserRepository.save(user);
        return "home";
    }





    // Uitloggen van gebruiker
    @GetMapping("/logout")
    public String logout(Principal principal, HttpServletRequest request, HttpServletResponse response) {

        //
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());

        if (principal == null) return "redirect:/home";
        // Toon home pagina
        return "/home";
    }






    @GetMapping("/forgot-password")
    public String forgotPassword(Principal principal, Model model) {

        String emailUser = "" ;
        model.addAttribute("emailUser", emailUser);


        return "/user/forgotpassword";
    }


    @PostMapping("/forgot-password/send-mail")
    public String forgotPasswordSendMail(@RequestParam("emailUser") String emailUser, Principal principal, Model model) {


        AppUser appUser = appUserRepository.findByEmail(emailUser);
        if (appUser != null){
            logger.info("=========FOUND===========");
            logger.info(appUser.getFirstName() + "  " + appUser.getLastName() + "  " + appUser.getPassword());
            logger.info("Generating Secret One time Use PasswordResetKey");
            appUser.generateSecretPasswordResetKey(appUser.getId().toString());
            appUserRepository.save(appUser);
            logger.info("====================");
            emailService.sendPasswordResetEmail(appUser.getEmail(),appUser.getPasswordResetKey());
        }
        else
        {
            logger.info("USER NOT FOUND");
        }


        return "redirect:/user/forgot-password/sent";
    }



    @GetMapping("/forgot-password/sent")
    public String sendPasswordResetEmail(Model model) {

        String errorMessage = "Email Has Been send";
        model.addAttribute("errorMessage", errorMessage);

        return "/user/forgotpassword";
    }


    @GetMapping("/password-reset/{secretKey}")
    public String managepassword(@PathVariable String secretKey, Model model) {

        AppUser appuser = appUserRepository.findByPasswordResetKey(secretKey);

        if (appuser != null) {

            String errorMessage = "PASSWORD RESET HTML MOET NOG AANGEMAAKT WORDEN SECRET KEY IS JUST";
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute("secretKey",secretKey);



            return "/user/changepassword"; //wijzig nadien de passw reset html pagina is gemaakt
        } else {
            String errorMessage = "Invalid Key. Please contact support";
            model.addAttribute("errorMessage", errorMessage);
            return "error";
        }
    }

    @PostMapping("/password-reset/{secretKey}/change-password")
    public String managepasswordChange(@PathVariable String secretKey,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       Model model) {

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        AppUser appuser = appUserRepository.findByPasswordResetKey(secretKey);
        if (appuser != null) {
            appuser.removePasswordResetKey();
            appuser.setPassword(passwordEncoder.encode(confirmPassword));
            appUserRepository.save(appuser);


            return "redirect:/user/login?message=Password reset successful";



        } else {
            String errorMessage = "Invalid Key. Please contact support";
            model.addAttribute("errorMessage", errorMessage);
            return "error";
        }
    }



}




