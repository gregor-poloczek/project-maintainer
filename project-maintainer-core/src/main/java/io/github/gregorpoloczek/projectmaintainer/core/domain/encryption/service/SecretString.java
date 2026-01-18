package io.github.gregorpoloczek.projectmaintainer.core.domain.encryption.service;

import lombok.Value;

@Value
public class SecretString {
    String value;

    public String toString() {
        return "SecretString[value=%s]".formatted(value != null ? "***" : null);
    }
}
