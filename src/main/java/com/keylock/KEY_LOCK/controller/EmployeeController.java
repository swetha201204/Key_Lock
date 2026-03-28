package com.keylock.KEY_LOCK.controller;

import com.keylock.KEY_LOCK.model.FileRecord;
import com.keylock.KEY_LOCK.model.User;
import com.keylock.KEY_LOCK.service.FileService;
import com.keylock.KEY_LOCK.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

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
        User employee = userService.findByEmail(email).orElseThrow();

        List<FileRecord> files = fileService.getAllFiles();

        model.addAttribute("employee", employee);
        model.addAttribute("files", files);
        model.addAttribute("fileCount", files.size());
        model.addAttribute("isRevoked", employee.isRevoked());

        return "employee/dashboard";
    }

    // -------------------------------------------------------
    // Download File (requires RSA key verification)
    // -------------------------------------------------------

    @PostMapping("/download/{fileId}")
    public void downloadFile(@PathVariable Long fileId,
                             @RequestParam("rsaKey") String rsaKey,
                             Authentication auth,
                             HttpServletResponse response,
                             RedirectAttributes redirectAttributes) throws Exception {

        String email = auth.getName();

        try {
            // Attempt download with RSA key verification
            byte[] fileBytes = fileService.downloadFile(fileId, email, rsaKey);

            // Get file info for headers
            FileRecord record = fileService.getAllFiles().stream()
                    .filter(f -> f.getId().equals(fileId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("File not found"));

            // Set response headers for file download
            response.setContentType(record.getContentType() != null ? record.getContentType() : "application/octet-stream");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + record.getOriginalName() + "\"");
            response.setContentLength(fileBytes.length);

            // Write file to response
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();

        } catch (SecurityException e) {
            // Invalid RSA key or revoked access
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/html; charset=UTF-8");
            response.getWriter().write(buildErrorPage(e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/html; charset=UTF-8");
            response.getWriter().write(buildErrorPage("Download failed: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------
    // RSA Key Entry Form (shown when Download is clicked)
    // -------------------------------------------------------

    @GetMapping("/download-form/{fileId}")
    public String downloadForm(@PathVariable Long fileId, Authentication auth, Model model) {
        String email = auth.getName();
        User employee = userService.findByEmail(email).orElseThrow();

        if (employee.isRevoked()) {
            model.addAttribute("error", "Your access has been revoked by the administrator.");
            return "employee/dashboard";
        }

        FileRecord record = fileService.getAllFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElse(null);

        model.addAttribute("file", record);
        model.addAttribute("fileId", fileId);
        model.addAttribute("employee", employee);
        return "employee/download-form";
    }

    // -------------------------------------------------------
    // Error page helper
    // -------------------------------------------------------

    private String buildErrorPage(String message) {
        return "<!DOCTYPE html><html><head><title>Access Denied</title>"
            + "<style>body{font-family:sans-serif;background:#0a0c10;color:#e8eaf0;display:flex;align-items:center;justify-content:center;height:100vh;margin:0}"
            + ".box{background:#111318;border:1px solid #ff4757;border-radius:12px;padding:32px;max-width:480px;text-align:center}"
            + "h2{color:#ff4757}p{color:#5a6070}a{color:#00d4ff;text-decoration:none}</style></head>"
            + "<body><div class='box'><h2>🔒 Access Denied</h2><p>" + message + "</p>"
            + "<br><a href='/employee/dashboard'>← Back to Dashboard</a></div></body></html>";
    }
}
