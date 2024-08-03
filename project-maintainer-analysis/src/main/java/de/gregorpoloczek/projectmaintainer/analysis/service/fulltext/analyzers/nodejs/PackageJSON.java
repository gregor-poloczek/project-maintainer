package de.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.nodejs;

import java.util.Map;
import java.util.Optional;


public record PackageJSON(
        Optional<String> name,
        Optional<Map<String, String>> dependencies,
        Optional<Map<String, String>> devDependencies,
        Optional<Map<String, String>> volta) {

}
