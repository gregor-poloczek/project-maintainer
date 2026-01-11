package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceFileV1 {
    public static final String VERSION = "1";
    String version;
    WorkspaceV1 workspace;

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkspaceV1 {
        String id;
        String name;
        @Builder.Default
        List<ProjectConnectionV1> projectConnections = new ArrayList<>();
        @Builder.Default
        List<ProjectV1> projects = new ArrayList<>();
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectConnectionV1 {
        @NonNull
        String type;
        @NonNull
        String id;
        @NonNull
        String version;
        @NonNull
        ObjectNode settings;
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectV1 {
        FQPN fqpn;
        String connectionId;
        ProjectMetaDataV1 metaData;

        public FQPN getFQPN() {
            return fqpn;
        }
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectMetaDataV1 {
        String name;
        String owner;
        String description;
        URI uri;
        String websiteLink;
        String browserLink;
    }

}
