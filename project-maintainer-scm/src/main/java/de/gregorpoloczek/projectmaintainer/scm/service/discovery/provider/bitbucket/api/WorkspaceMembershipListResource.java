package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api;

import java.util.List;
import lombok.Data;

@Data
public class WorkspaceMembershipListResource {

    List<WorkspaceMembershipResource> values;

    @Data
    public static class WorkspaceMembershipResource {

        WorkspaceResource workspace;
    }

    @Data
    public static class WorkspaceResource {

        String slug;
    }
}
