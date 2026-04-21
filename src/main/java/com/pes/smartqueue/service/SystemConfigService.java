package com.pes.smartqueue.service;

import org.springframework.stereotype.Service;

@Service
public class SystemConfigService {
    private final QueueService queueService;
    private int slotDurationMinutes = 30;

    public SystemConfigService(QueueService queueService) {
        this.queueService = queueService;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        if (slotDurationMinutes <= 0) {
            throw new IllegalArgumentException("Slot duration must be greater than 0");
        }
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public String getActiveQueueStrategy() {
        return queueService.getActiveStrategyKey();
    }

    public void setActiveQueueStrategy(String strategy) {
        queueService.setActiveStrategy(strategy);
    }
}