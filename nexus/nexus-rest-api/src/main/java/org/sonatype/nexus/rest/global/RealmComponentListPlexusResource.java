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
import org.apache.shiro.realm.Realm;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.rest.component.AbstractComponentListPlexusResource;
import org.sonatype.nexus.rest.model.PlexusComponentListResourceResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

/**
 * Get the list of security realms available.
 */
@Component( role = PlexusResource.class, hint = "RealmComponentListPlexusResource" )
@Path( RealmComponentListPlexusResource.RESOURCE_URI )
@Produces( { "application/xml", "application/json" } )
public class RealmComponentListPlexusResource
    extends AbstractComponentListPlexusResource
{
    public static final String RESOURCE_URI = "/components/realm_types";
    
    @Override
    public String getResourceUri()
    {
        return RESOURCE_URI;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( getResourceUri(), "authcBasic,perms[nexus:componentrealmtypes]" );
    }

    @Override
    protected String getRole( Request request )
    {
        return Realm.class.getName();
    }
    
    /**
     * Get the list of security realms available for use in nexus (authentication and authorization).
     */
    @Override
    @GET
    @ResourceMethodSignature( output = PlexusComponentListResourceResponse.class )
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        return super.get( context, request, response, variant );
    }
}
