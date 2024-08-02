package de.gregorpoloczek.projectmaintainer.analysis.fulltext;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

public class ProjectIndexWriterFacade extends ProjectIndexFacadeBase {

    private final IndexWriter writer;

    public ProjectIndexWriterFacade(WorkingCopy workingCopy) {
        super(workingCopy);

        try {
            // TODO close dir?
            FSDirectory dir = FSDirectory.open(Paths.get(getPath()));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            this.writer = new IndexWriter(dir, config);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            this.writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void indexFile(ProjectFileLocation projectFileLocation) {
        try {
            Document doc = createDocument(projectFileLocation);
            writer.updateDocument(new Term(DocumentConstants.ID, doc.get(DocumentConstants.ID)), doc);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Document createDocument(ProjectFileLocation projectFileLocation) throws IOException {
        String path = projectFileLocation.getRelativePath().toString();
        String id = path;
        String content = IOUtils.toString(projectFileLocation.getAbsolutePath().toUri(), StandardCharsets.UTF_8);

        Document doc = new Document();
        doc.add(new StringField(DocumentConstants.ID, id, Field.Store.YES));
        doc.add(new TextField(DocumentConstants.PATH, path, Field.Store.YES));
        doc.add(new TextField(DocumentConstants.CONTENT, content, Field.Store.YES));
        return doc;
    }
}