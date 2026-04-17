package com.pes.smartqueue.controller;

import com.pes.smartqueue.model.queue.EntryType;
import com.pes.smartqueue.service.QueueService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reception")
public class ReceptionistController {
    private final QueueService queueService;

    public ReceptionistController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/queue")
    public String queueDesk(Model model) {
        model.addAttribute("entries", queueService.getAllEntries());
        model.addAttribute("orderedQueue", queueService.getOrderedQueue());
        model.addAttribute("entryTypes", EntryType.values());
        model.addAttribute("activeStrategy", queueService.getActiveStrategyKey());
        model.addAttribute("strategies", queueService.getAvailableStrategyKeys());
        return "reception/queue";
    }

    @PostMapping("/queue")
    public String addEntry(@RequestParam @NotBlank String customerName,
                           @RequestParam EntryType type,
                           RedirectAttributes redirectAttributes) {
        queueService.addEntry(customerName, type);
        redirectAttributes.addFlashAttribute("success", "Queue entry added");
        return "redirect:/reception/queue";
    }

    @PostMapping("/queue/start-next")
    public String startNext(RedirectAttributes redirectAttributes) {
        try {
            queueService.startNext();
            redirectAttributes.addFlashAttribute("success", "Started next waiting entry");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reception/queue";
    }

    @PostMapping("/queue/{id}/start")
    public String startById(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> queueService.startEntry(id), "Entry moved to IN_PROGRESS", redirectAttributes);
    }

    @PostMapping("/queue/{id}/complete")
    public String completeById(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> queueService.completeEntry(id), "Entry moved to COMPLETED", redirectAttributes);
    }

    @PostMapping("/queue/{id}/cancel")
    public String cancelById(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> queueService.cancelEntry(id), "Entry moved to CANCELLED", redirectAttributes);
    }

    @PostMapping("/queue/strategy")
    public String changeStrategy(@RequestParam String strategy,
                                 RedirectAttributes redirectAttributes) {
        try {
            queueService.setActiveStrategy(strategy);
            redirectAttributes.addFlashAttribute("success", "Queue strategy changed to " + strategy);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reception/queue";
    }

    private String transition(Runnable transitionAction, String successMessage,
                              RedirectAttributes redirectAttributes) {
        try {
            transitionAction.run();
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reception/queue";
    }
}
