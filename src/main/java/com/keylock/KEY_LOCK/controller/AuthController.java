package com.keylock.KEY_LOCK.controller;

import com.keylock.KEY_LOCK.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // -------------------------------------------------------
    // Landing / Home
    // -------------------------------------------------------

    @GetMapping("/")
    public String home(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            String role = auth.getAuthorities().iterator().next().getAuthority();
            return role.equals("ROLE_ADMIN") ? "redirect:/admin/dashboard" : "redirect:/employee/dashboard";
        }
        return "index";
    }

    // -------------------------------------------------------
    // Register
    // -------------------------------------------------------

    @GetMapping("/register")
    public String registerPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/";
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String role,
            HttpSession session,
            Model model) {

        // Validation
        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Name is required.");
            return "register";
        }
        if (email == null || !email.contains("@")) {
            model.addAttribute("error", "Valid email is required.");
            return "register";
        }
        if (password == null || password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "register";
        }
        if (role == null || (!role.equals("ADMIN") && !role.equals("EMPLOYEE"))) {
            model.addAttribute("error", "Please select a valid role.");
            return "register";
        }

        try {
            // Register user — returns the ONE-TIME private key
            String privateKeyPem = userService.registerUser(name, email, password, role);

            // Store private key in session temporarily to show once
            session.setAttribute("oneTimeKey", privateKeyPem);
            session.setAttribute("registeredEmail", email);
            session.setAttribute("registeredName", name);

            return "redirect:/show-key";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed. Please try again.");
            return "register";
        }
    }

    // -------------------------------------------------------
    // Show RSA Key (one time after registration)
    // -------------------------------------------------------

    @GetMapping("/show-key")
    public String showKey(HttpSession session, Model model) {
        String key = (String) session.getAttribute("oneTimeKey");
        String email = (String) session.getAttribute("registeredEmail");
        String name = (String) session.getAttribute("registeredName");

        if (key == null) {
            // Key already shown or session expired
            return "redirect:/login";
        }

        model.addAttribute("rsaKey", key);
        model.addAttribute("email", email);
        model.addAttribute("name", name);

        // Clear key from session so it can't be seen again
        session.removeAttribute("oneTimeKey");

        return "show-key";
    }

    // -------------------------------------------------------
    // Login
    // -------------------------------------------------------

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Authentication auth,
                            Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/";
        if (error != null) model.addAttribute("error", "Invalid email or password.");
        if (logout != null) model.addAttribute("message", "You have been logged out.");
        return "login";
    }
}
