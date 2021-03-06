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
package org.sonatype.nexus.integrationtests.nexus532;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.rest.model.RepositoryGroupListResource;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * CRUD tests for XML request/response.
 */
public class Nexus532GroupsCrudXmlIT
    extends AbstractNexusIntegrationTest
{

    protected GroupMessageUtil messageUtil;

    public Nexus532GroupsCrudXmlIT()
    {
    }
    
    @BeforeClass
    public void setSecureTest(){
    	this.messageUtil = new GroupMessageUtil( this, this.getXMLXStream(), MediaType.APPLICATION_XML );
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    @Test
    public void createGroupTest()
        throws IOException
    {

        RepositoryGroupResource resource = new RepositoryGroupResource();

        resource.setId( "createTestGroup" );
        resource.setName( "createTestGroup" );
        resource.setFormat( "maven2" );
        resource.setProvider( "maven2" );

        createMembers( resource );

        // this also validates
        this.messageUtil.createGroup( resource );
    }

    @Test
    public void notFoundTest()
        throws Exception
    {
        String groupId = "nonexisted-group-from-mars";

        Response response = RequestFacade.doGetRequest( GroupMessageUtil.SERVICE_PART + "/" + groupId );

        Assert.assertEquals( 404, response.getStatus().getCode() );
    }

    @Test
    public void readTest()
        throws IOException
    {

        RepositoryGroupResource resource = new RepositoryGroupResource();

        resource.setId( "readTestGroup" );
        resource.setName( "readTestGroup" );
        resource.setFormat( "maven2" );
        resource.setProvider( "maven2" );

        createMembers( resource );

        // this also validates
        this.messageUtil.createGroup( resource );

        RepositoryGroupResource responseRepo = this.messageUtil.getGroup( resource.getId() );

        // validate they are the same
        this.messageUtil.validateResourceResponse( resource, responseRepo );

    }

    protected void createMembers( RepositoryGroupResource resource )
    {
        RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
        member.setId( "nexus-test-harness-repo" );
        resource.addRepository( member );
    }

    @Test
    public void updateTest()
        throws IOException
    {

        RepositoryGroupResource resource = new RepositoryGroupResource();

        resource.setId( "updateTestGroup" );
        resource.setName( "updateTestGroup" );
        resource.setFormat( "maven2" );
        resource.setProvider( "maven2" );

        createMembers( resource );

        // this also validates
        resource = this.messageUtil.createGroup( resource );

        // udpdate the group
        RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
        member.setId( "nexus-test-harness-repo2" );
        resource.addRepository( member );

        this.messageUtil.updateGroup( resource );

    }

    @Test
    public void deleteTest()
        throws IOException
    {
        RepositoryGroupResource resource = new RepositoryGroupResource();

        resource.setId( "deleteTestGroup" );
        resource.setName( "deleteTestGroup" );
        resource.setFormat( "maven2" );
        resource.setProvider( "maven2" );

        createMembers( resource );

        // this also validates
        resource = this.messageUtil.createGroup( resource );

        // now delete it...
        // use the new ID
        Response response = this.messageUtil.sendMessage( Method.DELETE, resource );

        if ( !response.getStatus().isSuccess() )
        {
            Assert.fail( "Could not delete Repository: " + response.getStatus() );
        }
        Assert.assertNull( getNexusConfigUtil().getRepo( resource.getId() ) );
    }

    @Test
    public void listTest()
        throws IOException
    {

        RepositoryGroupResource resource = new RepositoryGroupResource();

        resource.setId( "listTestGroup" );
        resource.setName( "listTestGroup" );
        resource.setFormat( "maven2" );
        resource.setProvider( "maven2" );

        createMembers( resource );

        // this also validates
        resource = this.messageUtil.createGroup( resource );

        // now get the lists
        List<RepositoryGroupListResource> groups = this.messageUtil.getList();

        for ( Iterator<RepositoryGroupListResource> iter = groups.iterator(); iter.hasNext(); )
        {
            RepositoryGroupListResource group = iter.next();
            M2GroupRepositoryConfiguration cGroup = getNexusConfigUtil().getGroup( group.getId() );

            Assert.assertNotNull( cGroup, "CRepositoryGroup" );
        }
    }
}
