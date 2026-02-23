package io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.nodejs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;
import java.util.Optional;


@JsonIgnoreProperties(ignoreUnknown = true)
public record PackageJSON(
        Optional<String> name,
        Optional<Map<String, String>> dependencies,
        Optional<Map<String, String>> devDependencies,
        Optional<Map<String, String>> volta) {

}
