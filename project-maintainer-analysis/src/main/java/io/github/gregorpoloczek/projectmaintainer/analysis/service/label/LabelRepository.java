package io.github.gregorpoloczek.projectmaintainer.analysis.service.label;

import io.github.gregorpoloczek.projectmaintainer.core.common.repository.GenericProjectRelatableRepository;
import java.util.SortedSet;
import org.springframework.stereotype.Repository;

@Repository
public class LabelRepository extends GenericProjectRelatableRepository<SortedSet<Label>> {

}
