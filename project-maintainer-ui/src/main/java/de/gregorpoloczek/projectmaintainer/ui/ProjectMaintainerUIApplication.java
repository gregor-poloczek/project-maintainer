package de.gregorpoloczek.projectmaintainer.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Push
@SpringBootApplication(scanBasePackages = "de.gregorpoloczek.projectmaintainer")
public class ProjectMaintainerUIApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(ProjectMaintainerUIApplication.class, args);
    }
}
