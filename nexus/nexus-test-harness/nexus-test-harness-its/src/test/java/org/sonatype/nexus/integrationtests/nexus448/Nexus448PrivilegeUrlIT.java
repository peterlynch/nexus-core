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
package org.sonatype.nexus.integrationtests.nexus448;

import java.io.IOException;

import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeDescriptor;
import org.sonatype.security.rest.model.PrivilegeStatusResource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * GETS for application privileges where returning an error, so this is a really simple test to make sure the GET will
 * work.
 */
public class Nexus448PrivilegeUrlIT
    extends AbstractNexusIntegrationTest
{

    private PrivilegesMessageUtil messageUtil;

    public Nexus448PrivilegeUrlIT()
    {
    }
    
    @BeforeClass
    public void setSecureTest(){ 
    	this.messageUtil = new PrivilegesMessageUtil( this, this.getXMLXStream(), MediaType.APPLICATION_XML );
        TestContainer.getInstance().getTestContext().setSecureTest( true );
    }

    @Test
    public void testUrls()
        throws IOException
    {

        PrivilegeStatusResource resource = this.messageUtil.getPrivilegeResource( "T2" );
        Assert.assertEquals( resource.getType(), TargetPrivilegeDescriptor.TYPE, "Type" );

        resource = this.messageUtil.getPrivilegeResource( "1" );
        Assert.assertEquals( resource.getType(), ApplicationPrivilegeDescriptor.TYPE, "Type" );

    }

}
