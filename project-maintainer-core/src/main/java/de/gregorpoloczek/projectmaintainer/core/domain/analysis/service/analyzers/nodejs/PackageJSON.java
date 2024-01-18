package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.nodejs;

import java.util.Map;
import java.util.Optional;


public record PackageJSON(Optional<Map<String, String>> dependencies,
                          Optional<Map<String, String>> devDependencies,
                          Optional<Map<String, String>> volta) {

}
