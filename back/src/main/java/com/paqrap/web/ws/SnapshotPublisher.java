package com.paqrap.web.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SnapshotPublisher {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void publicar(String runId, Object snapshotDto) {
        messagingTemplate.convertAndSend("/topic/sim/" + runId, snapshotDto);
    }
}
