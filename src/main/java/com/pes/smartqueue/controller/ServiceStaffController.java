package com.pes.smartqueue.controller;

import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.service.QueueService;
import com.pes.smartqueue.service.ServiceSessionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
public class ServiceStaffController {
    private final ServiceSessionService serviceSessionService;
    private final QueueService queueService;

    public ServiceStaffController(ServiceSessionService serviceSessionService, QueueService queueService) {
        this.serviceSessionService = serviceSessionService;
        this.queueService = queueService;
    }

    @GetMapping("/sessions")
    public String sessions(Authentication authentication, Model model) {
        ServiceSession serviceSession = serviceSessionService.requireStaffSession(authentication.getName());
        QueueEntry assignedEntry = serviceSessionService.getAssignedQueueEntryForStaff(authentication.getName());
        model.addAttribute("serviceSession", serviceSession);
        model.addAttribute("assignedEntry", assignedEntry);
        model.addAttribute("queueSnapshot", queueService.getOrderedQueue());
        return "staff/sessions";
    }

    @PostMapping("/availability/available")
    public String markAvailable(Authentication authentication, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.setStaffAvailability(authentication.getName(), true),
            "You are marked as AVAILABLE", redirectAttributes);
    }

    @PostMapping("/availability/unavailable")
    public String markUnavailable(Authentication authentication, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.setStaffAvailability(authentication.getName(), false),
            "You are marked as UNAVAILABLE", redirectAttributes);
    }

    @PostMapping("/assigned/complete")
    public String completeAssigned(Authentication authentication, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.completeAssignedQueueEntryForStaff(authentication.getName()),
            "Assigned patient marked as completed", redirectAttributes);
    }

    private String transition(Runnable transitionAction, String successMessage,
                              RedirectAttributes redirectAttributes) {
        try {
            transitionAction.run();
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/staff/sessions";
    }
}
