package de.gregorpoloczek.projectmaintainer.analysis;

import de.gregorpoloczek.projectmaintainer.core.common.repository.GenericProjectRelatableRepository;
import java.util.SortedSet;
import org.springframework.stereotype.Repository;

@Repository
public class LabelRepository extends GenericProjectRelatableRepository<SortedSet<Label>> {

}
