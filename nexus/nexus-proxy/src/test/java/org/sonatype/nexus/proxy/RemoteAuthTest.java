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

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;

public class RemoteAuthTest
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

    public void testHttpAuths()
        throws Exception
    {
        // remote target of repo1 is not protected
        StorageItem item =
            getRepositoryRegistry().getRepository( "repo1" ).retrieveItem(
                                                                           new ResourceStoreRequest( "/repo1.txt",
                                                                                                     false ) );
        checkForFileAndMatchContents( item );

        // remote target of repo2 is protected with HTTP BASIC
        UsernamePasswordRemoteAuthenticationSettings settings2 =
            new UsernamePasswordRemoteAuthenticationSettings( "cstamas", "cstamas123" );
        DefaultRemoteStorageContext ctx2 = new DefaultRemoteStorageContext( null );
        ctx2.setRemoteAuthenticationSettings( settings2 );
        getRepositoryRegistry().getRepositoryWithFacet( "repo2", ProxyRepository.class ).setRemoteStorageContext( ctx2 );

        item =
            getRepositoryRegistry().getRepository( "repo2" ).retrieveItem(
                                                                           new ResourceStoreRequest( "/repo2.txt",
                                                                                                     false ) );
        checkForFileAndMatchContents( item );

        // remote target of repo3 is protected with HTTP DIGEST
        UsernamePasswordRemoteAuthenticationSettings settings3 = new UsernamePasswordRemoteAuthenticationSettings("brian", "brian123");
        DefaultRemoteStorageContext ctx3 = new DefaultRemoteStorageContext( null );
        ctx3.setRemoteAuthenticationSettings( settings3 );
        getRepositoryRegistry().getRepositoryWithFacet( "repo3", ProxyRepository.class ).setRemoteStorageContext( ctx3 );

        item =
            getRepositoryRegistry().getRepository( "repo3" ).retrieveItem(
                                                                           new ResourceStoreRequest( "/repo3.txt",
                                                                                                     false ) );
        checkForFileAndMatchContents( item );
    }
}
