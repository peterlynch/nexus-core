/**
 * Copyright (c) 2007-2008 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.sonatype.nexus.index;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.lucene.search.Query;
import org.sonatype.nexus.index.context.IndexingContext;

/**
 * A grouped search request.
 * 
 * @see NexusIndexer#searchGrouped(GroupedSearchRequest)
 */
public class GroupedSearchRequest
    extends AbstractSearchRequest
{
    private Grouping grouping;

    private Comparator<String> groupKeyComparator;

    public GroupedSearchRequest( Query query, Grouping grouping )
    {
        this( query, grouping, String.CASE_INSENSITIVE_ORDER );
    }

    public GroupedSearchRequest( Query query, Grouping grouping, Comparator<String> groupKeyComparator )
    {
        this( query, grouping, groupKeyComparator, null );
    }

    public GroupedSearchRequest( Query query, Grouping grouping, IndexingContext context )
    {
        this( query, grouping, String.CASE_INSENSITIVE_ORDER, context );
    }

    public GroupedSearchRequest( Query query, Grouping grouping, Comparator<String> groupKeyComparator,
                                 IndexingContext context )
    {
        super( query, context != null ? Arrays.asList( new IndexingContext[] { context } ) : null );

        this.grouping = grouping;

        this.groupKeyComparator = groupKeyComparator;
    }

    public Grouping getGrouping()
    {
        return grouping;
    }

    public void setGrouping( Grouping grouping )
    {
        this.grouping = grouping;
    }

    public Comparator<String> getGroupKeyComparator()
    {
        return groupKeyComparator;
    }

    public void setGroupKeyComparator( Comparator<String> groupKeyComparator )
    {
        this.groupKeyComparator = groupKeyComparator;
    }
}
