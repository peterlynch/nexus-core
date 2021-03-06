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

import static org.sonatype.nexus.integrationtests.ITGroups.SECURITY;

import java.io.IOException;

import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test Group CRUD privileges.
 */
public class Nexus532GroupCrudPermissionIT
    extends AbstractPrivilegeTest
{
	
    @BeforeClass(alwaysRun = true)
    public void setSecureTest(){
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }
    
    @Test(groups = SECURITY)
    public void testCreatePermission()
        throws IOException
    {
        this.giveUserPrivilege( TEST_USER_NAME, "repository-all" );

        RepositoryGroupResource group = new RepositoryGroupResource();
        group.setId( "testCreatePermission" );
        group.setName( "testCreatePermission" );
        group.setFormat( "maven2" );
        group.setProvider( "maven2" );

        RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
        member.setId( "nexus-test-harness-repo" );
        group.addRepository( member );

        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        Response response = this.groupUtil.sendMessage( Method.POST, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // use admin
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        // now give create
        this.giveUserPrivilege( "test-user", "13" );

        // now.... it should work...
        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        response = this.groupUtil.sendMessage( Method.POST, group );
        Assert.assertEquals( response.getStatus().getCode(), 201, "Response status: " );
        group = this.groupUtil.getGroup( group.getId() );

        // read should succeed (inherited)
        response = this.groupUtil.sendMessage( Method.GET, group );
        Assert.assertEquals( response.getStatus().getCode(), 200, "Response status: " );

        // update should fail
        response = this.groupUtil.sendMessage( Method.PUT, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // delete should fail
        response = this.groupUtil.sendMessage( Method.DELETE, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

    }

    @Test(groups = SECURITY)
    public void testUpdatePermission()
        throws IOException
    {
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        this.giveUserPrivilege( TEST_USER_NAME, "repository-all" );

        RepositoryGroupResource group = new RepositoryGroupResource();
        group.setId( "testUpdatePermission" );
        group.setName( "testUpdatePermission" );
        group.setFormat( "maven2" );
        group.setProvider( "maven2" );

        RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
        member.setId( "nexus-test-harness-repo" );
        group.addRepository( member );

        Response response = this.groupUtil.sendMessage( Method.POST, group );
        Assert.assertEquals( response.getStatus().getCode(), 201, "Response status: " );
        group = this.groupUtil.getGroup( group.getId() );

        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        // update repo
        group.setName( "tesUpdatePermission2" );
        response = this.groupUtil.sendMessage( Method.PUT, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // use admin
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        // now give update
        this.giveUserPrivilege( "test-user", "15" );

        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        // should work now...
        response = this.groupUtil.sendMessage( Method.PUT, group );
        Assert.assertEquals( response.getStatus().getCode(), 200, "Response status: " );

        // read should succeed (inherited)
        response = this.groupUtil.sendMessage( Method.GET, group );
        Assert.assertEquals( response.getStatus().getCode(), 200, "Response status: " );

        // update should fail
        response = this.groupUtil.sendMessage( Method.POST, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // delete should fail
        response = this.groupUtil.sendMessage( Method.DELETE, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

    }

    @Test(groups = SECURITY)
    public void testReadPermission()
        throws IOException
    {
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        this.giveUserPrivilege( TEST_USER_NAME, "repository-all" );

        RepositoryGroupResource group = new RepositoryGroupResource();
        group.setId( "testReadPermission" );
        group.setName( "testReadPermission" );
        group.setFormat( "maven2" );
        group.setProvider( "maven2" );

        RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
        member.setId( "nexus-test-harness-repo" );
        group.addRepository( member );

        Response response = this.groupUtil.sendMessage( Method.POST, group );
        Assert.assertEquals( response.getStatus().getCode(), 201, "Response status: " );
        group = this.groupUtil.getGroup( group.getId() );

        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        // update repo
        group.setName( "tesUpdatePermission2" );
        response = this.groupUtil.sendMessage( Method.PUT, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // use admin
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        // now give read
        this.giveUserPrivilege( "test-user", "14" );

        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        // read should fail
        response = this.groupUtil.sendMessage( Method.GET, group );
        Assert.assertEquals( response.getStatus().getCode(), 200, "Response status: " );

        // update should fail
        response = this.groupUtil.sendMessage( Method.POST, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // delete should fail
        response = this.groupUtil.sendMessage( Method.PUT, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // should work now...
        response = this.groupUtil.sendMessage( Method.DELETE, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

    }

    @Test(groups = SECURITY)
    public void testDeletePermission()
        throws IOException
    {
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        this.giveUserPrivilege( TEST_USER_NAME, "repository-all" );

        RepositoryGroupResource group = new RepositoryGroupResource();
        group.setId( "testDeletePermission" );
        group.setName( "testDeletePermission" );
        group.setFormat( "maven2" );
        group.setProvider( "maven2" );

        RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
        member.setId( "nexus-test-harness-repo" );
        group.addRepository( member );

        Response response = this.groupUtil.sendMessage( Method.POST, group );
        Assert.assertEquals( response.getStatus().getCode(), 201, "Response status: " );
        group = this.groupUtil.getGroup( group.getId() );

        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        // update repo
        group.setName( "tesUpdatePermission2" );
        response = this.groupUtil.sendMessage( Method.DELETE, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // use admin
        TestContainer.getInstance().getTestContext().useAdminForRequests();

        // now give delete
        this.giveUserPrivilege( "test-user", "16" );

        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        // read should succeed (inherited)
        response = this.groupUtil.sendMessage( Method.GET, group );
        Assert.assertEquals( response.getStatus().getCode(), 200, "Response status: " );

        // update should fail
        response = this.groupUtil.sendMessage( Method.POST, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // delete should fail
        response = this.groupUtil.sendMessage( Method.PUT, group );
        Assert.assertEquals( response.getStatus().getCode(), 403, "Response status: " );

        // should work now...
        response = this.groupUtil.sendMessage( Method.DELETE, group );
        Assert.assertEquals( response.getStatus().getCode(), 204, "Response status: " );

    }

}
