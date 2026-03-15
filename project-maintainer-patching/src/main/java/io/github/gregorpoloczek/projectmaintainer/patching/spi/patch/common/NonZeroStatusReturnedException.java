package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NonZeroStatusReturnedException extends RuntimeException {
    List<String> command;
    int status;


    public NonZeroStatusReturnedException(List<String> command, int status) {
        super("Failed to run command \"%s\" returning status code %d".formatted(String.join(" ", command), status));
        this.command = List.copyOf(command);
        this.status = status;
    }
}
