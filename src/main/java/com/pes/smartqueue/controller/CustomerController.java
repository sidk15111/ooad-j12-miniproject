package com.pes.smartqueue.controller;

import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.service.AppointmentService;
import org.springframework.security.core.Authentication;
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
    public String appointments(Authentication authentication, Model model) {
        appointmentService.expirePastDue();
        model.addAttribute("appointments", appointmentService.listByCustomerName(authentication.getName()));
        return "customer/appointments";
    }

    @PostMapping("/appointments")
    public String create(Authentication authentication,
                         @RequestParam String slotTime,
                         RedirectAttributes redirectAttributes) {
        try {
            appointmentService.create(authentication.getName(), LocalDateTime.parse(slotTime));
            redirectAttributes.addFlashAttribute("success", "Appointment created");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customer/appointments";
    }

    @PostMapping("/appointments/{id}/confirm")
    public String confirm(Authentication authentication,
                          @PathVariable long id,
                          RedirectAttributes redirectAttributes) {
        if (!isOwner(authentication.getName(), id, redirectAttributes)) {
            return "redirect:/customer/appointments";
        }
        return transition(() -> appointmentService.confirm(id), "Appointment confirmed", redirectAttributes);
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(Authentication authentication,
                         @PathVariable long id,
                         RedirectAttributes redirectAttributes) {
        if (!isOwner(authentication.getName(), id, redirectAttributes)) {
            return "redirect:/customer/appointments";
        }
        return transition(() -> appointmentService.cancel(id), "Appointment cancelled", redirectAttributes);
    }

    @PostMapping("/appointments/{id}/reschedule")
    public String reschedule(Authentication authentication,
                            @PathVariable long id,
                            @RequestParam String newSlotTime,
                            RedirectAttributes redirectAttributes) {
        if (!isOwner(authentication.getName(), id, redirectAttributes)) {
            return "redirect:/customer/appointments";
        }
        return transition(
            () -> appointmentService.reschedule(id, LocalDateTime.parse(newSlotTime)),
            "Appointment rescheduled — please confirm your new slot",
            redirectAttributes
        );
    }

    private boolean isOwner(String username, long appointmentId, RedirectAttributes redirectAttributes) {
        Appointment appointment = appointmentService.get(appointmentId);
        if (!username.equals(appointment.getCustomerName())) {
            redirectAttributes.addFlashAttribute("error", "You can only manage your own appointments");
            return false;
        }
        return true;
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
