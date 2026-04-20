package com.pes.smartqueue.controller;

import com.pes.smartqueue.service.AppointmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/customer")
public class CustomerController {
    private final AppointmentService appointmentService;

    public CustomerController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/appointments")
    public String appointments(Model model) {
        appointmentService.expirePastDue();
        model.addAttribute("appointments", appointmentService.list());
        return "customer/appointments";
    }

    @PostMapping("/appointments")
    public String create(@RequestParam String customerName,
                         @RequestParam String slotTime,
                         RedirectAttributes redirectAttributes) {
        try {
            appointmentService.create(customerName, LocalDateTime.parse(slotTime));
            redirectAttributes.addFlashAttribute("success", "Appointment created");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customer/appointments";
    }

    @PostMapping("/appointments/{id}/confirm")
    public String confirm(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> appointmentService.confirm(id), "Appointment confirmed", redirectAttributes);
    }

    @PostMapping("/appointments/{id}/checkin")
    public String checkIn(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> appointmentService.checkIn(id), "Appointment checked in", redirectAttributes);
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return transition(() -> appointmentService.cancel(id), "Appointment cancelled", redirectAttributes);
    }

    private String transition(Runnable transitionAction, String successMessage,
                              RedirectAttributes redirectAttributes) {
        try {
            transitionAction.run();
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customer/appointments";
    }
}
