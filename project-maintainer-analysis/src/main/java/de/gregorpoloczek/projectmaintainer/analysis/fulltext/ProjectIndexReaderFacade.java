package de.gregorpoloczek.projectmaintainer.analysis.fulltext;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.ProjectFileLocationImpl;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
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

public class ProjectIndexReaderFacade extends ProjectIndexFacadeBase {

    private final DirectoryReader reader;
    private final IndexSearcher searcher;
    private final Analyzer analyzer;

    public ProjectIndexReaderFacade(WorkingCopy workingCopy) {
        super(workingCopy);

        try {
            // TODO close dir?
            FSDirectory dir = FSDirectory.open(Paths.get(getPath()));
            this.analyzer = new StandardAnalyzer();
            this.reader = DirectoryReader.open(dir);
            this.searcher = new IndexSearcher(this.reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }


    public List<ProjectFileLocation> search(String fileNameQueryString, String contentQueryString) {
        try {
            // Create a RegexpQuery for the path
            Query fileNameQuery = new RegexpQuery(new Term(DocumentConstants.PATH, fileNameQueryString));

            // Create a QueryParser for the content
            QueryParser parser = new QueryParser(DocumentConstants.CONTENT, analyzer);
            Query contentQuery = parser.parse(contentQueryString);

            // Combine the queries using a BooleanQuery
            BooleanQuery combinedQuery = new BooleanQuery.Builder()
                    .add(fileNameQuery, BooleanClause.Occur.MUST)
                    .add(contentQuery, BooleanClause.Occur.MUST)
                    .build();

            // Execute the search
            // TODO change limit
            TopDocs results = searcher.search(combinedQuery, 10);
            return toProjectFileLocations(results);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<ProjectFileLocation> search(String fileNameQuery) {
        Term term = new Term(DocumentConstants.PATH, fileNameQuery);
        Query query = new RegexpQuery(term);

        // Execute the search
        try {
            // TODO change limit
            TopDocs results = searcher.search(query, 10);
            return toProjectFileLocations(results);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<ProjectFileLocation> toProjectFileLocations(TopDocs results) throws IOException {
        ScoreDoc[] hits = results.scoreDocs;

        List<ProjectFileLocation> result = new ArrayList<>();
        for (ScoreDoc hit : hits) {
            Document doc = searcher.doc(hit.doc);
            result.add(toProjectFileLocation(doc));
        }
        return result;
    }

    private ProjectFileLocation toProjectFileLocation(Document doc) {
        File file = this.getWorkingCopy().getDirectory().toPath().resolve(Path.of(doc.get("path"))).toFile();
        // TODO nicht hier erzeugen
        return ProjectFileLocationImpl.of(this.getWorkingCopy(), file);
    }
}