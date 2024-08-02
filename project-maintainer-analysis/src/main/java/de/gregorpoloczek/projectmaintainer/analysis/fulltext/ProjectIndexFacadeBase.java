package de.gregorpoloczek.projectmaintainer.analysis.fulltext;

import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ProjectIndexFacadeBase implements AutoCloseable {


    @Getter(AccessLevel.PROTECTED)
    private final WorkingCopy workingCopy;


    protected String getPath() {
        return "./.lucene/" + workingCopy.getFQPN().toString().replaceAll("::", "/");
    }

}