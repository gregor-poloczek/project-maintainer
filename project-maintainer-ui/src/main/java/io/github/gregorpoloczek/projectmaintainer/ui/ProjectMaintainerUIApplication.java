package io.github.gregorpoloczek.projectmaintainer.ui;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Push
@NpmPackage(value = "diff2html", version = "3.4.55")
@Theme(value = "project-maintainer-theme", variant = Lumo.DARK)
@SpringBootApplication(scanBasePackages = "io.github.gregorpoloczek.projectmaintainer")
public class ProjectMaintainerUIApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(ProjectMaintainerUIApplication.class, args);
    }
}
