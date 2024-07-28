package de.gregorpoloczek.projectmaintainer.analysis.fulltext;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopyService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectFullTextSearchService {

    private final WorkingCopyService workingCopyService;


    public void index(ProjectRelatable projectRelatable, Collection<? extends ProjectFileLocation> locations) {
        WorkingCopy workingCopy = this.workingCopyService.find(projectRelatable)
                .orElseThrow(IllegalStateException::new);

        try (ProjectIndexWriterFacade indexWriterFacade = new ProjectIndexWriterFacade(workingCopy)) {
            for (ProjectFileLocation location : locations) {
                indexWriterFacade.indexFile(location);
            }
        }
    }

    public List<ProjectFileLocation> search(ProjectRelatable projectRelatable, String fileNameQuery) {
        WorkingCopy workingCopy = this.workingCopyService.find(projectRelatable)
                .orElseThrow(IllegalStateException::new);
        try (ProjectIndexReaderFacade indexReaderFacade = new ProjectIndexReaderFacade(workingCopy)) {
            return indexReaderFacade.search(fileNameQuery);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<ProjectFileLocation> search(ProjectRelatable projectRelatable, String fileNameQuery,
            String contentQuery) {
        WorkingCopy workingCopy = this.workingCopyService.find(projectRelatable)
                .orElseThrow(IllegalStateException::new);
        try (ProjectIndexReaderFacade indexReaderFacade = new ProjectIndexReaderFacade(workingCopy)) {
            return indexReaderFacade.search(fileNameQuery, contentQuery);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
