package com.pes.smartqueue.controller;

import com.pes.smartqueue.model.session.ServiceSessionStatus;
import com.pes.smartqueue.service.MetricsService;
import com.pes.smartqueue.service.QueueService;
import com.pes.smartqueue.service.ReportService;
import com.pes.smartqueue.service.SystemConfigService;
import com.pes.smartqueue.service.UserManagementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final SystemConfigService systemConfigService;
    private final UserManagementService userManagementService;
    private final MetricsService metricsService;
    private final ReportService reportService;
    private final QueueService queueService;

    public AdminController(SystemConfigService systemConfigService,
                           UserManagementService userManagementService,
                           MetricsService metricsService,
                           ReportService reportService,
                           QueueService queueService) {
        this.systemConfigService = systemConfigService;
        this.userManagementService = userManagementService;
        this.metricsService = metricsService;
        this.reportService = reportService;
        this.queueService = queueService;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/config")
    public String config(Model model) {
        model.addAttribute("slotDurationMinutes", systemConfigService.getSlotDurationMinutes());
        model.addAttribute("activeStrategy", systemConfigService.getActiveQueueStrategy());
        model.addAttribute("strategies", queueService.getAvailableStrategyKeys());
        return "admin/config";
    }

    @PostMapping("/config/slot-duration")
    public String updateSlotDuration(@RequestParam int slotDurationMinutes,
                                     RedirectAttributes redirectAttributes) {
        try {
            systemConfigService.setSlotDurationMinutes(slotDurationMinutes);
            redirectAttributes.addFlashAttribute("success", "Slot duration updated");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/config";
    }

    @PostMapping("/config/strategy")
    public String updateStrategy(@RequestParam String strategy,
                                 RedirectAttributes redirectAttributes) {
        try {
            systemConfigService.setActiveQueueStrategy(strategy);
            redirectAttributes.addFlashAttribute("success", "Queue strategy updated to " + strategy);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/config";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userManagementService.listUsers());
        model.addAttribute("roles", userManagementService.allowedRoles());
        return "admin/users";
    }

    @PostMapping("/users/{username}/approve")
    public String approveUser(@PathVariable String username, RedirectAttributes redirectAttributes) {
        return userAction(() -> userManagementService.approve(username), "User approved: " + username, redirectAttributes);
    }

    @PostMapping("/users/{username}/reactivate")
    public String reactivateUser(@PathVariable String username, RedirectAttributes redirectAttributes) {
        return userAction(() -> userManagementService.reactivate(username), "User reactivated: " + username, redirectAttributes);
    }

    @PostMapping("/users/{username}/deactivate")
    public String deactivateUser(@PathVariable String username, RedirectAttributes redirectAttributes) {
        return userAction(() -> userManagementService.deactivate(username), "User deactivated: " + username, redirectAttributes);
    }

    @PostMapping("/users/add")
    public String addUser(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String role,
                          RedirectAttributes redirectAttributes) {
        return userAction(() -> userManagementService.addUser(username, password, role), "User added: " + username, redirectAttributes);
    }

    @PostMapping("/users/{username}/delete")
    public String deleteUser(@PathVariable String username, RedirectAttributes redirectAttributes) {
        return userAction(() -> userManagementService.deleteUser(username), "User deleted: " + username, redirectAttributes);
    }

    @GetMapping("/metrics")
    public String metrics(Model model) {
        model.addAttribute("appointmentBreakdown", metricsService.getAppointmentStatusBreakdown());
        model.addAttribute("queueBreakdown", metricsService.getQueueStatusBreakdown());

        Map<String, Long> sessionBreakdown = new LinkedHashMap<>();
        for (ServiceSessionStatus status : ServiceSessionStatus.values()) {
            sessionBreakdown.put(status.name(), metricsService.countSessionByStatus(status));
        }
        model.addAttribute("sessionBreakdown", sessionBreakdown);
        return "admin/metrics";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        if (!model.containsAttribute("reportType")) {
            model.addAttribute("reportType", "daily");
        }
        if (!model.containsAttribute("reportDate")) {
            model.addAttribute("reportDate", LocalDate.now().toString());
        }
        return "admin/reports";
    }

    @PostMapping("/reports/generate")
    public String generateReport(@RequestParam String type,
                                 @RequestParam String date,
                                 RedirectAttributes redirectAttributes) {
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            String report = resolveReport(type, parsedDate);
            redirectAttributes.addFlashAttribute("reportResult", report);
            redirectAttributes.addFlashAttribute("reportType", type);
            redirectAttributes.addFlashAttribute("reportDate", date);
            redirectAttributes.addFlashAttribute("success", "Report generated");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("reportType", type);
            redirectAttributes.addFlashAttribute("reportDate", date);
        }
        return "redirect:/admin/reports";
    }

    @PostMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(@RequestParam String type,
                                               @RequestParam String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        String report = resolveReport(type, parsedDate);
        String normalizedType = type.equalsIgnoreCase("weekly") ? "weekly" : "daily";
        String filename = "smartqueue-" + normalizedType + "-" + parsedDate.format(DateTimeFormatter.ISO_DATE) + ".txt";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(report.getBytes(StandardCharsets.UTF_8));
    }

    private String userAction(Runnable action, String successMessage, RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private String resolveReport(String type, LocalDate date) {
        if (type == null) {
            throw new IllegalArgumentException("Report type is required");
        }
        return switch (type.toLowerCase()) {
            case "daily" -> reportService.generateDailyReport(date);
            case "weekly" -> reportService.generateWeeklyReport(date);
            default -> throw new IllegalArgumentException("Unsupported report type: " + type);
        };
    }
}