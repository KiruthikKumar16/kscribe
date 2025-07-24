package com.kscribe;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Component
public class BrowserLauncher {
    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:8080/index.html"));
        } catch (Exception e) {
            System.out.println("Could not open browser: " + e.getMessage());
        }
    }
} 