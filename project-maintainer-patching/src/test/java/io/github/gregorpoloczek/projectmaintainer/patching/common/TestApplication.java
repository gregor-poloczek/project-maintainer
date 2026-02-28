package io.github.gregorpoloczek.projectmaintainer.patching.common;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "io.github.gregorpoloczek.projectmaintainer")
public class TestApplication {
}
