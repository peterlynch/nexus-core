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
package org.sonatype.nexus.integrationtests.nexus429;


import static org.sonatype.nexus.integrationtests.ITGroups.SECURITY;

import java.io.File;
import java.util.Date;

import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test the privilege for manual artifact upload.
 */
@Test(groups = SECURITY)
public class Nexus429UploadArtifactPrivilegeIT
    extends AbstractPrivilegeTest
{
    private static final String TEST_RELEASE_REPO = "nexus-test-harness-release-repo";

    public Nexus429UploadArtifactPrivilegeIT()
    {
        super( TEST_RELEASE_REPO );
    }
    
    @BeforeClass(alwaysRun = true)
    public void setSecureTest(){
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    public void deployPrivWithPom()
        throws Exception
    {
        // GAV
        Gav gav =
            new Gav( this.getTestId(), "uploadWithGav", "1.0.0", null, "xml", 0, new Date().getTime(), "", false,
                     false, null, false, null );

        // file to deploy
        File fileToDeploy = this.getTestFile( gav.getArtifactId() + "." + gav.getExtension() );

        File pomFile = this.getTestFile( "pom.xml" );

        // deploy
        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        // url to upload to
        String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

        // with pom should fail
        int status =
            getDeployUtils().deployUsingPomWithRest( uploadURL, TEST_RELEASE_REPO, fileToDeploy, pomFile, null, null );
        Assert.assertEquals( status, 403, "Status should have been 403" );

        // give deployment role
        TestContainer.getInstance().getTestContext().useAdminForRequests();
        this.giveUserPrivilege( "test-user", "65" );
        this.giveUserRole( "test-user", "repo-all-full" );

        // try again
        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        status =
            getDeployUtils().deployUsingPomWithRest( uploadURL, TEST_RELEASE_REPO, fileToDeploy, pomFile, null, null );
        Assert.assertEquals( status, 201, "Status should have been 201" );
    }

    public void deployPrivWithGav()
        throws Exception
    {
        // GAV
        Gav gav =
            new Gav( this.getTestId(), "uploadWithGav", "1.0.0", null, "xml", 0, new Date().getTime(), "", false,
                     false, null, false, null );

        // file to deploy
        File fileToDeploy = this.getTestFile( gav.getArtifactId() + "." + gav.getExtension() );

        // deploy
        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        // url to upload to
        String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

        // with gav should fail
        int status = getDeployUtils().deployUsingGavWithRest( uploadURL, TEST_RELEASE_REPO, gav, fileToDeploy );
        Assert.assertEquals( status, 403, "Status should have been 403" );

        // give deployment role
        TestContainer.getInstance().getTestContext().useAdminForRequests();
        this.giveUserPrivilege( "test-user", "65" );
        this.giveUserRole( "test-user", "repo-all-full" );

        // try again
        TestContainer.getInstance().getTestContext().setUsername( "test-user" );
        TestContainer.getInstance().getTestContext().setPassword( "admin123" );

        status = getDeployUtils().deployUsingGavWithRest( uploadURL, TEST_RELEASE_REPO, gav, fileToDeploy );
        Assert.assertEquals( status, 201, "Status should have been 201" );

    }

}
