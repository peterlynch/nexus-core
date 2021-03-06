/**
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://www.sonatype.com/products/nexus/attributions/.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.security.ldap.realms.testharness.nxcm58;

import java.io.IOException;

import org.restlet.data.Response;
import org.restlet.data.Status;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.security.ldap.realms.testharness.AbstractLdapIntegrationIT;
import org.testng.Assert;
import org.testng.annotations.Test;


public class Nxcm58NexusStatusIT
    extends AbstractLdapIntegrationIT
{

    @Test
    public void getStatus()
        throws IOException
    {
        Response response = RequestFacade.doGetRequest( "service/local/status" );
        Status status = response.getStatus();
        Assert.assertTrue( status.isSuccess(), "Unable to get nexus status" + status );
    }

    @Test
    public void getLdapInfo()
        throws IOException
    {
        Response response = RequestFacade.doGetRequest( "service/local/ldap/conn_info" );
        Status status = response.getStatus();
        Assert.assertTrue( status.isSuccess(), "Unable to reach ldap services\n" + status + "\n"
                + response.getEntity().getText() );
    }

}
