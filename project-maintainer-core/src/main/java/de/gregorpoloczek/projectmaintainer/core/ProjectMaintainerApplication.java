package de.gregorpoloczek.projectmaintainer.core;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Push
@SpringBootApplication()
public class ProjectMaintainerApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(ProjectMaintainerApplication.class, args);
    }
}
