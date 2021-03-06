package org.sonatype.nexus.index.context;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

/**
 * An extended Searcher, that holds reference to the IndexingContext that is a searcher for. Needed to provide "extra"
 * data for search hits, that are not on index, and suppot ArtifactInfoPostprocessor's.
 * 
 * @author cstamas
 */
public class NexusIndexSearcher
    extends IndexSearcher
{
    private final IndexingContext indexingContext;

    public NexusIndexSearcher( final IndexingContext indexingContext )
        throws IOException
    {
        this( indexingContext, indexingContext.getIndexReader() );
    }

    public NexusIndexSearcher( final IndexingContext indexingContext, final IndexReader reader )
        throws IOException
    {
        super( reader );
        
        this.indexingContext = indexingContext;
    }

    public IndexingContext getIndexingContext()
    {
        return indexingContext;
    }
}
