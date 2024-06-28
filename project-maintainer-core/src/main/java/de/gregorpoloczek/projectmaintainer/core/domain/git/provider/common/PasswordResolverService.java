package de.gregorpoloczek.projectmaintainer.core.domain.git.provider.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PasswordResolverService {

    @Value("file:./.credentials/passwords.properties")
    private Resource credentials;

    final ConversionService conversionService;
    private Properties passwords;

    public PasswordResolverService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public String getPassword(String group, String username) {
        return Optional.ofNullable(this.getPasswords().get(group + "." + username))
                .map(String.class::cast)
                .orElseThrow(() -> new IllegalStateException("Cannot find password for user " + username));
    }

    private Properties getPasswords() {
        if (this.passwords != null) {
            return this.passwords;
        }

        try {
            this.passwords = this.conversionService.convert(
                    this.credentials.getContentAsString(StandardCharsets.UTF_8),
                    Properties.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this.passwords;
    }

}
