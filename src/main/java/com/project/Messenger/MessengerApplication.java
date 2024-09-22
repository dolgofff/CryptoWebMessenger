package com.project.Messenger;


import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Push
public class MessengerApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(MessengerApplication.class, args);
    }

}
