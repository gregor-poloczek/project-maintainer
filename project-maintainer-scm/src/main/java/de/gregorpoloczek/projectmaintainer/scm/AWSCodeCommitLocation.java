package de.gregorpoloczek.projectmaintainer.scm;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AWSCodeCommitLocation {

    @NotNull
    String profile;
    @NotNull
    String username;
    @NotNull
    @NotEmpty
    List<String> regions;
}
