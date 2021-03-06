package org.sonatype.nexus.test.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.plugins.capabilities.internal.rest.CapabilitiesPlexusResource;
import org.sonatype.nexus.plugins.capabilities.internal.rest.dto.CapabilitiesListResponseResource;
import org.sonatype.nexus.plugins.capabilities.internal.rest.dto.CapabilityListItemResource;
import org.sonatype.nexus.plugins.capabilities.internal.rest.dto.CapabilityRequestResource;
import org.sonatype.nexus.plugins.capabilities.internal.rest.dto.CapabilityResource;
import org.sonatype.nexus.plugins.capabilities.internal.rest.dto.CapabilityResponseResource;
import org.sonatype.nexus.plugins.capabilities.internal.rest.dto.CapabilityStatusResponseResource;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.testng.Assert;

import com.thoughtworks.xstream.XStream;

public class CapabilitiesMessageUtil
{
    private static final Logger LOG = Logger.getLogger( TaskScheduleUtil.class );

    private static XStream xstream;

    static
    {
        xstream = XStreamFactory.getXmlXStream();
        new CapabilitiesPlexusResource( null, null ).configureXStream( xstream );
    }

    public static List<CapabilityListItemResource> list()
        throws IOException
    {
        Response response = RequestFacade.doGetRequest( "service/local/capabilities" );

        if ( response.getStatus().isError() )
        {
            LOG.error( response.getStatus().toString() );
            return Collections.emptyList();
        }

        XStreamRepresentation representation =
            new XStreamRepresentation( xstream, response.getEntity().getText(), MediaType.APPLICATION_XML );

        CapabilitiesListResponseResource scheduleResponse =
            (CapabilitiesListResponseResource) representation.getPayload( new CapabilitiesListResponseResource() );

        return scheduleResponse.getData();
    }

    public static CapabilityListItemResource create( CapabilityResource capability )
        throws IOException
    {
        XStreamRepresentation representation = new XStreamRepresentation( xstream, "", MediaType.APPLICATION_XML );

        CapabilityRequestResource envelope = new CapabilityRequestResource();
        envelope.setData( capability );

        representation.setPayload( envelope );

        Response response = RequestFacade.sendMessage( "service/local/capabilities", Method.POST, representation );

        if ( response.getStatus().isError() )
        {
            Assert.fail( response.getStatus().toString() );
        }

        representation = new XStreamRepresentation( xstream, response.getEntity().getText(), MediaType.APPLICATION_XML );

        CapabilityStatusResponseResource scheduleResponse =
            (CapabilityStatusResponseResource) representation.getPayload( new CapabilityStatusResponseResource() );

        return scheduleResponse.getData();
    }

    public static CapabilityResource read( String id )
        throws IOException
    {
        Response response = RequestFacade.doGetRequest( "service/local/capabilities/" + id );

        if ( response.getStatus().isError() )
        {
            Assert.fail( response.getStatus().toString() );
        }

        XStreamRepresentation representation =
            new XStreamRepresentation( xstream, response.getEntity().getText(), MediaType.APPLICATION_XML );

        CapabilityResponseResource scheduleResponse =
            (CapabilityResponseResource) representation.getPayload( new CapabilityResponseResource() );

        return scheduleResponse.getData();
    }

    public static CapabilityListItemResource update( CapabilityResource resource )
        throws IOException
    {
        XStreamRepresentation representation = new XStreamRepresentation( xstream, "", MediaType.APPLICATION_XML );

        CapabilityRequestResource envelope = new CapabilityRequestResource();
        envelope.setData( resource );

        representation.setPayload( envelope );

        Response response =
            RequestFacade.sendMessage( "service/local/capabilities/" + resource.getId(), Method.PUT, representation );

        if ( response.getStatus().isError() )
        {
            Assert.fail( response.getStatus().toString() );
        }

        representation = new XStreamRepresentation( xstream, response.getEntity().getText(), MediaType.APPLICATION_XML );

        CapabilityStatusResponseResource scheduleResponse =
            (CapabilityStatusResponseResource) representation.getPayload( new CapabilityStatusResponseResource() );

        return scheduleResponse.getData();
    }

    public static void delete( String id )
        throws IOException
    {
        Response response = RequestFacade.sendMessage( "service/local/capabilities/" + id, Method.DELETE );

        if ( response.getStatus().isError() )
        {
            Assert.fail( response.getStatus().toString() );
        }
    }

}
