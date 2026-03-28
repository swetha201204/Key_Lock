package com.keylock.KEY_LOCK;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class KeylockApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeylockApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║       KeyLock Cloud is Running!          ║");
        System.out.println("║  Open: http://localhost:8080             ║");
        System.out.println("║  H2 DB Console: http://localhost:8080    ║");
        System.out.println("║           /h2-console                    ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("\n");
    }
}
