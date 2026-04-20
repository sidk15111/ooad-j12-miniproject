package com.pes.smartqueue.controller;

import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.model.session.ServiceSession;
import com.pes.smartqueue.service.QueueService;
import com.pes.smartqueue.service.ServiceSessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public String sessions(Model model) {
        List<ServiceSession> sessions = serviceSessionService.list();
        Map<Long, QueueEntry> assignedBySession = new java.util.HashMap<>();
        for (ServiceSession session : sessions) {
            QueueEntry entry = serviceSessionService.getAssignedQueueEntry(session.getId());
            if (entry != null) {
                assignedBySession.put(session.getId(), entry);
            }
        }
        model.addAttribute("sessions", sessions);
        model.addAttribute("assignedBySession", assignedBySession);
        model.addAttribute("queueSnapshot", queueService.getOrderedQueue());
        return "staff/sessions";
    }

    @PostMapping("/sessions")
    public String create(@RequestParam String staffUsername, RedirectAttributes redirectAttributes) {
        try {
            serviceSessionService.create(staffUsername);
            redirectAttributes.addFlashAttribute("success", "Session created");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/staff/sessions";
    }

    @PostMapping("/sessions/{id}/activate")
    public String activate(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.activate(id), "Session activated", redirectAttributes);
    }

    @PostMapping("/sessions/{id}/pause")
    public String pause(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.pause(id), "Session paused", redirectAttributes);
    }

    @PostMapping("/sessions/{id}/resume")
    public String resume(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.resume(id), "Session resumed", redirectAttributes);
    }

    @PostMapping("/sessions/{id}/complete")
    public String complete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.complete(id), "Session completed", redirectAttributes);
    }

    @PostMapping("/sessions/{id}/start-next")
    public String startNextForSession(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.startNextQueueEntry(id), "Started next queue entry for session", redirectAttributes);
    }

    @PostMapping("/sessions/{id}/complete-current")
    public String completeCurrentForSession(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.completeAssignedQueueEntry(id), "Completed assigned queue entry", redirectAttributes);
    }

    @PostMapping("/sessions/{id}/release-current")
    public String releaseCurrentForSession(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> serviceSessionService.releaseAssignedQueueEntry(id), "Released assigned queue entry back to waiting", redirectAttributes);
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
