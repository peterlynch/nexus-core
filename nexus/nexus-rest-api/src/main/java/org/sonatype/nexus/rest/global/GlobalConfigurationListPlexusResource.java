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
package org.sonatype.nexus.rest.global;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.rest.model.GlobalConfigurationListResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationListResourceResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

/**
 * The GlobalConfigurationList resource. This is a read only resource that simply returns a list of known configuration
 * resources.
 * 
 * @author cstamas
 * @author tstevens
 */
@Component( role = PlexusResource.class, hint = "GlobalConfigurationListPlexusResource" )
@Path( GlobalConfigurationListPlexusResource.RESOURCE_URI )
@Produces( { "application/xml", "application/json" } )
public class GlobalConfigurationListPlexusResource
    extends AbstractGlobalConfigurationPlexusResource
{
    public static final String RESOURCE_URI = "/global_settings";
    
    @Override
    public Object getPayloadInstance()
    {
        return null;
    }

    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( getResourceUri(), "authcBasic,perms[nexus:settings]" );
    }

    /**
     * Get the list of global configuration objects in nexus.
     */
    @Override
    @GET
    @ResourceMethodSignature( output = GlobalConfigurationListResourceResponse.class )
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        GlobalConfigurationListResourceResponse result = new GlobalConfigurationListResourceResponse();

        GlobalConfigurationListResource data = new GlobalConfigurationListResource();

        data.setName( GlobalConfigurationPlexusResource.DEFAULT_CONFIG_NAME );

        data.setResourceURI( createChildReference( request, this, data.getName() ).toString() );

        result.addData( data );

        data = new GlobalConfigurationListResource();

        data.setName( GlobalConfigurationPlexusResource.CURRENT_CONFIG_NAME );

        data.setResourceURI( createChildReference( request, this, data.getName() ).toString() );

        result.addData( data );

        return result;
    }

}
