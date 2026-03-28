package com.keylock.KEY_LOCK.controller;

import com.keylock.KEY_LOCK.model.FileRecord;
import com.keylock.KEY_LOCK.model.User;
import com.keylock.KEY_LOCK.service.FileService;
import com.keylock.KEY_LOCK.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private FileService fileService;

    @Autowired
    private UserService userService;

    // -------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        String email = auth.getName();
        User admin = userService.findByEmail(email).orElseThrow();

        List<FileRecord> files = fileService.getAllFiles();
        List<User> employees = userService.getAllEmployees();

        long activeCount = employees.stream().filter(u -> !u.isRevoked()).count();
        long revokedCount = employees.stream().filter(User::isRevoked).count();

        model.addAttribute("admin", admin);
        model.addAttribute("files", files);
        model.addAttribute("employees", employees);
        model.addAttribute("fileCount", files.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("revokedCount", revokedCount);
        model.addAttribute("activeTab", "files");

        return "admin/dashboard";
    }

    // -------------------------------------------------------
    // Upload File
    // -------------------------------------------------------

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/admin/dashboard";
        }

        try {
            fileService.uploadFile(file, auth.getName());
            redirectAttributes.addFlashAttribute("success",
                "\"" + file.getOriginalFilename() + "\" uploaded and encrypted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    // -------------------------------------------------------
    // Delete File
    // -------------------------------------------------------

    @PostMapping("/delete-file/{id}")
    public String deleteFile(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            fileService.deleteFile(id);
            redirectAttributes.addFlashAttribute("success", "File deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // -------------------------------------------------------
    // Users Management Tab
    // -------------------------------------------------------

    @GetMapping("/users")
    public String usersPage(Authentication auth, Model model) {
        String email = auth.getName();
        User admin = userService.findByEmail(email).orElseThrow();

        List<FileRecord> files = fileService.getAllFiles();
        List<User> employees = userService.getAllEmployees();

        long activeCount = employees.stream().filter(u -> !u.isRevoked()).count();
        long revokedCount = employees.stream().filter(User::isRevoked).count();

        model.addAttribute("admin", admin);
        model.addAttribute("files", files);
        model.addAttribute("employees", employees);
        model.addAttribute("fileCount", files.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("revokedCount", revokedCount);
        model.addAttribute("activeTab", "users");

        return "admin/dashboard";
    }

    // -------------------------------------------------------
    // Revoke User
    // -------------------------------------------------------

    @PostMapping("/revoke/{userId}")
    public String revokeUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.revokeUser(userId);
            redirectAttributes.addFlashAttribute("success", "User access revoked successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not revoke user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // -------------------------------------------------------
    // Restore User
    // -------------------------------------------------------

    @PostMapping("/restore/{userId}")
    public String restoreUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.restoreUser(userId);
            redirectAttributes.addFlashAttribute("success", "User access restored successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not restore user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
