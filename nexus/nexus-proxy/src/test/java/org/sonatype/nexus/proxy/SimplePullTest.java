/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.proxy;

import java.util.Collection;

import org.codehaus.plexus.util.FileUtils;
import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.events.RepositoryItemEventRetrieve;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;

public class SimplePullTest
    extends AbstractProxyTestEnvironment
{

    private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

    @Override
    protected EnvironmentBuilder getEnvironmentBuilder()
        throws Exception
    {
        ServletServer ss = (ServletServer) lookup( ServletServer.ROLE );
        this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder( ss );
        return jettyTestsuiteEnvironmentBuilder;
    }

    public void testSimplePull()
        throws Exception
    {
        StorageItem item = null;

        try
        {
            item = getRootRouter().retrieveItem(
                new ResourceStoreRequest(
                    "/repositories/repo1/activemq/activemq-core/1.2/broken/activemq-core-1.2",
                    false ) );

            fail();
        }
        catch ( ItemNotFoundException e )
        {
            // good, the layout says this is not a file!
        }

        item = getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar", false ) );
        checkForFileAndMatchContents( item );
        assertEquals( RepositoryItemEventCache.class, getTestEventListener().getFirstEvent().getClass() );
        assertEquals( RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass() );
        getTestEventListener().reset();

        item = getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/repositories/repo2/xstream/xstream/1.2.2/xstream-1.2.2.pom", false ) );
        checkForFileAndMatchContents( item );
        assertEquals( RepositoryItemEventCache.class, getTestEventListener().getFirstEvent().getClass() );
        assertEquals( RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass() );
        getTestEventListener().reset();

        item = getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/groups/test/activemq/activemq-core/1.2/activemq-core-1.2.jar", false ) );
        checkForFileAndMatchContents( item );
        assertEquals( RepositoryItemEventRetrieve.class, getTestEventListener().getFirstEvent().getClass() );
        assertEquals( 2, getTestEventListener().getEvents().size() );
        getTestEventListener().reset();

        item = getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/groups/test/xstream/xstream/1.2.2/xstream-1.2.2.pom", false ) );
        checkForFileAndMatchContents( item );
        assertEquals( RepositoryItemEventRetrieve.class, getTestEventListener().getFirstEvent().getClass() );
        assertEquals( 2, getTestEventListener().getEvents().size() );
        getTestEventListener().reset();

        item = getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/groups/test/rome/rome/0.9/rome-0.9.pom", false ) );
        checkForFileAndMatchContents( item );
        assertEquals( RepositoryItemEventCache.class, getTestEventListener().getFirstEvent().getClass() );
        assertEquals( RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass() );
        getTestEventListener().reset();

        item = getRootRouter().retrieveItem( new ResourceStoreRequest( "/groups/test/repo3.txt", false ) );
        checkForFileAndMatchContents( item );
        assertEquals( RepositoryItemEventCache.class, getTestEventListener().getFirstEvent().getClass() );
        assertEquals( RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass() );
        getTestEventListener().reset();

        item = getRootRouter().retrieveItem( new ResourceStoreRequest( "/groups/test/", false ) );
        Collection<StorageItem> dir = ( (StorageCollectionItem) item ).list();
        // we should have listed in root only those things/dirs we pulled, se above!
        assertEquals( 4, dir.size() );
    }

    public void testSimplePullWithRegardingToPathEnding()
        throws Exception
    {

        // pull the stuff from remote, to play with it below
        StorageItem item = getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar", false ) );
        checkForFileAndMatchContents( item );

        item = getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/groups/test/activemq/activemq-core/1.2/activemq-core-1.2.jar", false ) );
        checkForFileAndMatchContents( item );

        // new test regarding item properties and path endings.
        // All resource storage implementations should behave the same way.
        item = getRootRouter().retrieveItem( new ResourceStoreRequest( "/groups/test/activemq", false ) );
        assertEquals( "/groups/test/activemq", item.getPath() );
        assertEquals( "/groups/test", item.getParentPath() );
        assertEquals( "activemq", item.getName() );

        item = getRootRouter().retrieveItem( new ResourceStoreRequest( "/groups/test/activemq/", false ) );
        assertEquals( "/groups/test/activemq", item.getPath() );
        assertEquals( "/groups/test", item.getParentPath() );
        assertEquals( "activemq", item.getName() );

        // against reposes
        item = getRepositoryRegistry().getRepository( "repo1" ).retrieveItem(
            new ResourceStoreRequest( "/activemq", false ) );
        assertEquals( "/activemq", item.getPath() );
        assertEquals( "/", item.getParentPath() );
        assertEquals( "activemq", item.getName() );

        item = getRepositoryRegistry().getRepository( "repo1" ).retrieveItem(
            new ResourceStoreRequest( "/activemq", false ) );
        assertEquals( "/activemq", item.getPath() );
        assertEquals( "/", item.getParentPath() );
        assertEquals( "activemq", item.getName() );

        item = getRepositoryRegistry().getRepository( "repo1" ).retrieveItem(
            new ResourceStoreRequest( "/activemq/activemq-core/1.2", false ) );
        assertEquals( "/activemq/activemq-core/1.2", item.getPath() );
        assertEquals( "/activemq/activemq-core", item.getParentPath() );
        assertEquals( "1.2", item.getName() );
        assertTrue( StorageCollectionItem.class.isAssignableFrom( item.getClass() ) );

        StorageCollectionItem coll = (StorageCollectionItem) item;
        Collection<StorageItem> items = coll.list();
        assertEquals( 1, items.size() );
        StorageItem collItem = items.iterator().next();
        assertEquals( "/activemq/activemq-core/1.2/activemq-core-1.2.jar", collItem.getPath() );
        assertEquals( "activemq-core-1.2.jar", collItem.getName() );
        assertEquals( "/activemq/activemq-core/1.2", collItem.getParentPath() );
    }

    public void testSimplePush()
        throws Exception
    {

        ResourceStoreRequest request = new ResourceStoreRequest(
            "/repositories/inhouse/activemq/activemq-core/1.2/activemq-core-1.2.jar",
            true );
        StorageFileItem item = (StorageFileItem) getRootRouter().retrieveItem(
            new ResourceStoreRequest( "/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar", false ) );

        getRootRouter().storeItem( request, item.getInputStream(), null );

        assertTrue( FileUtils.contentEquals( getFile(
            getRepositoryRegistry().getRepository( "repo1" ),
            "/activemq/activemq-core/1.2/activemq-core-1.2.jar" ), getFile( getRepositoryRegistry().getRepository(
            "inhouse" ), "/activemq/activemq-core/1.2/activemq-core-1.2.jar" ) ) );
    }

    public void testSimplePullOfNonexistent()
        throws Exception
    {
        try
        {
            getRootRouter().retrieveItem(
                new ResourceStoreRequest(
                    "/groups/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar-there-is-no-such",
                    false ) );
            fail();
        }
        catch ( ItemNotFoundException e )
        {
            // good, this is what we need
        }

        try
        {
            getRootRouter().retrieveItem(
                new ResourceStoreRequest( "/groups/test/rome/rome/0.9/rome-0.9.pom-there-is-no-such", false ) );
            fail();
        }
        catch ( ItemNotFoundException e )
        {
            // good, this is what we need
        }
    }

    public void testSimplePullOfSlashEndedFilePaths()
        throws Exception
    {
        try
        {
            getRootRouter()
                .retrieveItem(
                    new ResourceStoreRequest(
                        "/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar",
                        false ) );
        }
        catch ( ItemNotFoundException e )
        {
            fail("Should get the file!");
        }

        try
        {
            getRootRouter().retrieveItem(
                new ResourceStoreRequest(
                    "/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar/",
                    false ) );

            fail( "The path ends with slash '/'!" );
        }
        catch ( ItemNotFoundException e )
        {
            // good
        }
    }
}
