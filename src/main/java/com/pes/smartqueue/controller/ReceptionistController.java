package com.pes.smartqueue.controller;

import com.pes.smartqueue.model.appointment.Appointment;
import com.pes.smartqueue.model.appointment.AppointmentStatus;
import com.pes.smartqueue.model.queue.EntryType;
import com.pes.smartqueue.model.queue.QueueEntry;
import com.pes.smartqueue.service.AppointmentService;
import com.pes.smartqueue.service.QueueService;
import com.pes.smartqueue.service.ServiceSessionService;
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
    private final AppointmentService appointmentService;
    private final ServiceSessionService serviceSessionService;

    public ReceptionistController(QueueService queueService,
                                  AppointmentService appointmentService,
                                  ServiceSessionService serviceSessionService) {
        this.queueService = queueService;
        this.appointmentService = appointmentService;
        this.serviceSessionService = serviceSessionService;
    }

    @GetMapping("/queue")
    public String queueDesk(Model model) {
        appointmentService.expirePastDue();
        model.addAttribute("entries", queueService.getAllEntries());
        model.addAttribute("orderedQueue", queueService.getOrderedQueue());
        model.addAttribute("confirmedAppointments", appointmentService.listConfirmed());
        model.addAttribute("availableStaff", serviceSessionService.listAvailableStaffUsernames());
        model.addAttribute("assignedStaffByQueueEntry", serviceSessionService.assignedStaffByQueueEntryId());
        model.addAttribute("checkedInAppointments", appointmentService.listCheckedIn().stream()
            .filter(appointment -> !queueService.isAppointmentQueued(appointment.getId()))
            .toList());
        return "reception/queue";
    }

    @PostMapping("/queue/walk-in")
    public String addWalkIn(@RequestParam @NotBlank String customerName,
                            RedirectAttributes redirectAttributes) {
        try {
            queueService.addEntry(customerName, EntryType.WALK_IN);
            redirectAttributes.addFlashAttribute("success", "Walk-in added to waiting queue");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reception/queue";
    }

    @PostMapping("/queue/from-appointment/{appointmentId}")
    public String addFromAppointment(@PathVariable long appointmentId,
                                     RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = appointmentService.get(appointmentId);
            if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
                throw new IllegalStateException("Appointment is not CHECKED_IN yet");
            }
            queueService.addAppointmentEntry(appointment.getId(), appointment.getCustomerName());
            redirectAttributes.addFlashAttribute("success", "Checked-in appointment added to waiting queue");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reception/queue";
    }

    @PostMapping("/queue/{id}/assign")
    public String assignWaitingEntry(@PathVariable long id,
                                     @RequestParam String staffUsername,
                                     RedirectAttributes redirectAttributes) {
        try {
            QueueEntry entry = queueService.getById(id);
            if (entry.getStatus() != com.pes.smartqueue.model.queue.QueueStatus.WAITING) {
                throw new IllegalStateException("Only WAITING entries can be assigned");
            }
            queueService.startEntry(entry.getId());
            serviceSessionService.assignQueueEntryToStaff(staffUsername, entry.getId());
            redirectAttributes.addFlashAttribute("success", "Queue entry assigned to " + staffUsername + " and moved to IN_PROGRESS");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reception/queue";
    }

    @PostMapping("/appointments/{id}/checkin")
    public String checkInAppointment(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = appointmentService.get(id);
            if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                throw new IllegalStateException("Only CONFIRMED appointments can be checked in");
            }
            appointmentService.checkIn(id);
            redirectAttributes.addFlashAttribute("success", "Appointment checked in");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reception/queue";
    }

    @PostMapping("/appointments/{id}/no-show")
    public String markNoShowForConfirmed(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = appointmentService.get(id);
            if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                throw new IllegalStateException("No-show can only be applied to CONFIRMED appointments waiting for check-in");
            }
            appointmentService.cancel(id);
            redirectAttributes.addFlashAttribute("success", "Appointment marked as no-show");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reception/queue";
    }
}
