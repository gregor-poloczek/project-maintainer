package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

public class ProjectMetaDataBuilder {

    @NotNull
    private FQPN fqpn;

    @NotNull
    private URI uri;

    @NotNull
    private String name;
    @NotNull
    private String owner;

    private Optional<String> browserLink;

    public ProjectMetaDataBuilder fqpn(final String segmet, final String... segments) {
        return this.fqpn(FQPN.of(segmet, segments));
    }

    public ProjectMetaDataBuilder fqpn(final FQPN fqpn) {
        this.fqpn = fqpn;
        return this;
    }

    private void validate() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final Set<ConstraintViolation<ProjectMetaDataBuilder>> violations = validator.validate(this);
        if (!violations.isEmpty()) {
            throw new IllegalStateException(violations.toString());
        }
    }

    public ProjectMetaData build() {
        this.validate();
        return new ProjectMetaData() {
            @Override
            public String getOwner() {
                return ProjectMetaDataBuilder.this.owner;
            }

            public String getName() {
                return ProjectMetaDataBuilder.this.name;
            }

            @Override
            public URI getURI() {
                return ProjectMetaDataBuilder.this.uri;
            }

            @Override
            public Optional<String> getBrowserLink() {
                return ProjectMetaDataBuilder.this.browserLink;
            }

            @Override
            public FQPN getFQPN() {
                return ProjectMetaDataBuilder.this.fqpn;
            }
        };
    }

    public ProjectMetaDataBuilder uri(final URI uri) {
        this.uri = uri;
        return this;
    }

    public ProjectMetaDataBuilder owner(final String owner) {
        this.owner = owner;
        return this;
    }

    public ProjectMetaDataBuilder name(final String name) {
        this.name = name;
        return this;
    }

    public ProjectMetaDataBuilder browserLink(Optional<String> browserLink) {
        this.browserLink = browserLink;
        return this;
    }
}
