package org.atomserver.app;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.atomserver.categories.CategoryQuery;
import org.compass.needle.terracotta.TerracottaDirectory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;

public abstract class LuceneCollectionIndex<E extends EntryNode>
        implements CollectionIndex<E> {

    private Directory directory;

    protected abstract E newEntryNode(String entryId);

    private final Map<String, E> entries = new HashMap<String, E>();
    private final Map<Integer, E> docIdToEntry = new HashMap<Integer, E>();

    protected LuceneCollectionIndex() {
        directory = new TerracottaDirectory();
    }

    public E getEntry(String entryId) {
        return this.entries.get(entryId);
    }

    public E removeEntryNodeFromIndices(String entryId) {
        E entryNode = entries.get(entryId);
        if (entryNode != null) {

            try {
                IndexWriter writer = new IndexWriter(directory, new StandardAnalyzer(),
                                                     IndexWriter.MaxFieldLength.UNLIMITED);
                writer.deleteDocuments(new Term("entryId", entryId));
                writer.commit();
                writer.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

        } else {
            entries.put(entryId, entryNode = newEntryNode(entryId));
        }
        return entryNode;
    }

    public void updateEntryNodeIntoIndices(E entryNode) {
        try {
            IndexWriter writer = new IndexWriter(directory, new StandardAnalyzer(),
                                                 IndexWriter.MaxFieldLength.UNLIMITED);
            int docId = writer.maxDoc();
            writer.addDocument(toDoc(entryNode));
            writer.commit();
            entries.put(entryNode.getEntryId(), entryNode);
            docIdToEntry.put(docId, entryNode);
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    public Iterator<E> buildIterator(CategoryQuery categoryQuery, final long timestamp) {

        final Query luceneCategoryQuery = buildQuery(categoryQuery);

        try {

            final IndexSearcher searcher = new IndexSearcher(IndexReader.open(directory));

            return new Iterator<E>() {
                long lastTimestamp = timestamp;
                TopDocs topDocs;
                int pos;

                { advance(); }

                void advance() {
                    BooleanQuery query = new BooleanQuery();
                    query.add(new RangeQuery(new Term("timestamp", l2s(lastTimestamp)), null, false),
                              BooleanClause.Occur.MUST);
                    query.add(luceneCategoryQuery, BooleanClause.Occur.MUST);

                    try {
                        topDocs = searcher.search(query, 100);
                    } catch (IOException e) {
                        // TODO: no!
                        throw new RuntimeException(e);
                    }
                    if (topDocs.scoreDocs == null || topDocs.scoreDocs.length == 0) {
                        topDocs = null;
                    } else {
                        try {
                            lastTimestamp = s2l(
                                    searcher.doc(topDocs.scoreDocs[topDocs.scoreDocs.length - 1].doc)
                                            .getField("timestamp").stringValue());
                        } catch (IOException e) {
                            // TODO: no!
                            throw new RuntimeException(e);
                        }

                        pos = 0;
                    }
                }


                public boolean hasNext() {
                    if (topDocs == null) {
                        return false;
                    }
                    if (pos < topDocs.scoreDocs.length) {
                        return true;
                    } else {
                        advance();
                        return hasNext();
                    }
                }

                public E next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException("no such element");
                    }
                    return docIdToEntry.get(topDocs.scoreDocs[pos++].doc);
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove not supported");
                }
            };
        } catch (Exception e) {
            // TODO: no!
            return Collections.EMPTY_SET.iterator();
        }
    }

    private Document toDoc(E entryNode) {
        Document doc = new Document();
        doc.add(new Field("entryId", entryNode.getEntryId(),
                          Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("timestamp", l2s(entryNode.getTimestamp()),
                          Field.Store.YES, Field.Index.NOT_ANALYZED));
        for (EntryCategory category : entryNode.getCategories()) {
            doc.add(new Field("category", category.getCategory().toString(),
                              Field.Store.YES, Field.Index.NOT_ANALYZED));
        }
        return doc;
    }

    private static final ThreadLocal<DecimalFormat> LONG_FORMATTER =
            new ThreadLocal<DecimalFormat>() {
                protected DecimalFormat initialValue() {
                    return new DecimalFormat("000000000000000");
                }
            };

    private static String l2s(Long value) {
        return LONG_FORMATTER.get().format(value);
    }

    private static Long s2l(String value) {
        try {
            return LONG_FORMATTER.get().parse(value).longValue();
        } catch (ParseException e) {
            return 0L;
        }
    }

    private Query buildQuery(CategoryQuery categoryQuery) {
        if (categoryQuery == null) {
            return new MatchAllDocsQuery();
        } else {
            BooleanQuery query;
            switch (categoryQuery.type) {
            case SIMPLE:
                CategoryQuery.SimpleCategoryQuery simpleCategoryQuery =
                        (CategoryQuery.SimpleCategoryQuery) categoryQuery;
                return new TermQuery(new Term("category",
                                              String.format("(%s)%s",
                                                            simpleCategoryQuery.scheme,
                                                            simpleCategoryQuery.term)));
            case AND:
                CategoryQuery.AndQuery andQuery = (CategoryQuery.AndQuery) categoryQuery;
                query = new BooleanQuery();
                query.add(buildQuery(andQuery.left), BooleanClause.Occur.MUST);
                query.add(buildQuery(andQuery.right), BooleanClause.Occur.MUST);
                return query;
            case OR:
                CategoryQuery.OrQuery orQuery = (CategoryQuery.OrQuery) categoryQuery;
                query = new BooleanQuery();
                query.add(buildQuery(orQuery.left), BooleanClause.Occur.SHOULD);
                query.add(buildQuery(orQuery.right), BooleanClause.Occur.SHOULD);
                return query;
            case NOT:
                CategoryQuery.NotQuery notQuery = (CategoryQuery.NotQuery) categoryQuery;
                query = new BooleanQuery();
                query.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
                query.add(buildQuery(notQuery.inner), BooleanClause.Occur.MUST_NOT);
                return query;
            default:
                throw new IllegalStateException(
                        String.format("invalid category query %s", categoryQuery));
            }
        }
    }

}
