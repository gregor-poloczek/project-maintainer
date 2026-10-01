package io.github.gregorpoloczek.projectmaintainer.integrationtests.support;

import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

/**
 * Connection used in integration tests. Discovering projects via this connection yields one project per entry in
 * {@link #getProjectNames()}.
 */
@Builder(toBuilder = true)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestProjectConnection implements ProjectConnection {

    public static final String TYPE = "integration-tests-in-memory";

    @NonNull
    @Builder.Default
    String id = UUID.randomUUID().toString();

    @Singular
    List<String> projectNames;

    @Override
    public String getType() {
        return TYPE;
    }
}
