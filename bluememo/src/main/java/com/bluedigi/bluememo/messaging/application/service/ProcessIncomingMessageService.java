package com.bluedigi.bluememo.messaging.application.service;

import org.springframework.stereotype.Service;

import com.bluedigi.bluememo.messaging.application.port.in.ProcessIncomingMessageUseCase;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;

@Service
public class ProcessIncomingMessageService implements ProcessIncomingMessageUseCase {
    @Override
    public void process(IncomingMessage message) {
        System.out.println("Processing incoming message: " + message.text());
        System.out.println("Channel Type: " + message.channelType());
    }
    
}
 