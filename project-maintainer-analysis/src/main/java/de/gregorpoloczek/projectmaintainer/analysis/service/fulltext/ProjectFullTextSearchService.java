package de.gregorpoloczek.projectmaintainer.analysis.service.fulltext;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.ProjectFileLocationImpl;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectFullTextSearchService {


    private final WorkingCopyService workingCopyService;

    public void index(ProjectRelatable projectRelatable, Collection<? extends ProjectFileLocation> locations) {
        WorkingCopy workingCopy = this.workingCopyService.require(projectRelatable);

        try (IndexWriter writer = createWriter(workingCopy)) {
            for (ProjectFileLocation location : locations) {
                this.indexFile(writer, location);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public List<ProjectFileLocation> search(ProjectRelatable projectRelatable, String fileNameQuery) {
        WorkingCopy workingCopy = this.workingCopyService.require(projectRelatable);

        try (IndexReader reader = this.createReader(workingCopy)) {
            Term term = new Term(DocumentConstants.PATH, fileNameQuery);
            Query query = new RegexpQuery(term);

            // TODO change limit
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs results = searcher.search(query, 10);
            return this.toProjectFileLocations(workingCopy, searcher, results);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public List<ProjectFileLocation> search(ProjectRelatable projectRelatable, String fileNameQueryString,
            String contentQueryString) {
        WorkingCopy workingCopy = this.workingCopyService.require(projectRelatable);

        try (IndexReader reader = this.createReader(workingCopy)) {
            // Create a RegexpQuery for the path
            Query fileNameQuery = new RegexpQuery(new Term(DocumentConstants.PATH, fileNameQueryString));

            // Create a QueryParser for the content
            QueryParser parser = new QueryParser(DocumentConstants.CONTENT, new StandardAnalyzer());
            Query contentQuery = parser.parse(contentQueryString);

            // Combine the queries using a BooleanQuery
            BooleanQuery query = new BooleanQuery.Builder()
                    .add(fileNameQuery, BooleanClause.Occur.MUST)
                    .add(contentQuery, BooleanClause.Occur.MUST)
                    .build();
            // TODO change limit
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs results = searcher.search(query, 10);
            return this.toProjectFileLocations(workingCopy, searcher, results);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private IndexWriter createWriter(WorkingCopy workingCopy) throws IOException {
        return new IndexWriter(getIndexDirectory(workingCopy), new IndexWriterConfig(new StandardAnalyzer()));
    }

    private IndexReader createReader(WorkingCopy workingCopy) throws IOException {
        return DirectoryReader.open(getIndexDirectory(workingCopy));
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


    private void indexFile(IndexWriter writer, ProjectFileLocation projectFileLocation) throws IOException {
        Document doc = createDocument(projectFileLocation);
        writer.updateDocument(new Term(DocumentConstants.ID, doc.get(DocumentConstants.ID)), doc);
    }

    private FSDirectory getIndexDirectory(WorkingCopy workingCopy) {
        try {
            Path path = getPath(workingCopy).resolve("index");
            return FSDirectory.open(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path getPath(WorkingCopy workingCopy) {
        return Paths.get("./.lucene/" + workingCopy.getFQPN().toString().replaceAll("::", "/"));
    }


    private List<ProjectFileLocation> toProjectFileLocations(WorkingCopy workingCopy, IndexSearcher searcher,
            TopDocs results)
            throws IOException {
        ScoreDoc[] hits = results.scoreDocs;

        List<ProjectFileLocation> result = new ArrayList<>();
        for (ScoreDoc hit : hits) {
            Document doc = searcher.doc(hit.doc);
            result.add(toProjectFileLocation(workingCopy, doc));
        }
        return result;
    }


    private ProjectFileLocation toProjectFileLocation(WorkingCopy workingCopy, Document doc) {
        File file = workingCopy.getDirectory().toPath().resolve(Path.of(doc.get("path"))).toFile();
        // TODO nicht hier erzeugen
        return ProjectFileLocationImpl.of(workingCopy, file);
    }

}
