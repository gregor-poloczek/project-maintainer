package de.gregorpoloczek.projectmaintainer.analysis.fulltext;

import de.gregorpoloczek.projectmaintainer.git.service.WorkingCopy;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

@RequiredArgsConstructor
public abstract class ProjectIndexFacadeBase implements AutoCloseable {


    @Getter(AccessLevel.PROTECTED)
    private final WorkingCopy workingCopy;


    protected String getPath() {
        return "./.lucene/" + workingCopy.getFQPN().toString().replaceAll("::", "/");
    }

}