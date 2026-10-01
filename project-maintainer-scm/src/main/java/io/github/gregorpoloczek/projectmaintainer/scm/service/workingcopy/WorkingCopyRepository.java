package io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy;

import io.github.gregorpoloczek.projectmaintainer.core.common.repository.GenericProjectRelatableRepository;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.exceptions.WorkingCopyNotFoundException;
import org.springframework.stereotype.Repository;

@Repository
public class WorkingCopyRepository extends GenericProjectRelatableRepository<WorkingCopy> {

    @Override
    protected RuntimeException createNotFoundException(ProjectRelatable projectRelatable) {
        return new WorkingCopyNotFoundException(projectRelatable.getFQPN());
    }
}
