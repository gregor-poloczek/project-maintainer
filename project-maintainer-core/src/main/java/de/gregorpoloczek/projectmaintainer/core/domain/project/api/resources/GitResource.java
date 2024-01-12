package de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources;

import java.net.URI;

public record GitResource(URI uri, GitProvider provider, WorkingCopyResource workingCopy) {

}
