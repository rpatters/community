/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Performs requests and retrieves the responses to create asciidoc-based
 * documentation.
 */
public class DocumentationOutput implements MethodRule
{
    private static final List<String> RESPONSE_HEADERS = Arrays.asList( new String[] {
            "Content-Type", "Location" } );

    private static final List<String> REQUEST_HEADERS = Arrays.asList( new String[] {
            "Content-Type", "Accept" } );

    public DocsBuilder builder( final String title )
    {
        return new DocsBuilder( title );
    }

    public DocsBuilder builder( final String title, final String description )
    {
        return new DocsBuilder( title, description );
    }

    public class DocsBuilder
    {
        private final String title;
        private String description;
        private Response.Status expectedResponseStatus = Response.Status.OK;
        private MediaType expectedMediaType = MediaType.APPLICATION_JSON_TYPE;
        private MediaType payloadMediaType = MediaType.APPLICATION_JSON_TYPE;
        private String expectedHeaderField;
        private String payload;

        private DocsBuilder( final String title )
        {
            this.title = title;
        }

        private DocsBuilder( final String title, final String description )
        {
            this.title = title;
            this.description = description;
        }

        public DocsBuilder description( final String description )
        {
            this.description = description;
            return this;
        }

        public DocsBuilder status( final Response.Status expectedResponseStatus )
        {
            this.expectedResponseStatus = expectedResponseStatus;
            return this;
        }

        public DocsBuilder type( final MediaType expectedMediaType )
        {
            this.expectedMediaType = expectedMediaType;
            return this;
        }

        public DocsBuilder payloadType( final MediaType payloadMediaType )
        {
            this.payloadMediaType = payloadMediaType;
            return this;
        }

        public DocsBuilder payload( final String payload )
        {
            this.payload = payload;
            return this;
        }

        public DocsBuilder header( final String exppectedHeaderField )
        {
            this.expectedHeaderField = exppectedHeaderField;
            return this;
        }

        public ClientResponse request( final ClientRequest request )
        {
            return retrieveResponse( title, description,
                    request.getURI().toString(), expectedResponseStatus,
                    expectedMediaType, expectedHeaderField, request );
        }

        public ClientResponse get( final String uri )
        {
            return retrieveResponseFromRequest( title, null, "GET",
                    uri, expectedResponseStatus,
                    expectedMediaType, expectedHeaderField );
        }

        public ClientResponse post( final String uri )
        {
            return retrieveResponseFromRequest( title, description, "POST",
                    uri,
                    payload, payloadMediaType,
                    expectedResponseStatus, expectedMediaType, expectedHeaderField );
        }

        public ClientResponse put( final String uri )
        {
            return retrieveResponseFromRequest( title, description, "PUT", uri,
                    payload, payloadMediaType,
                    expectedResponseStatus, expectedMediaType, expectedHeaderField );
        }

        public ClientResponse delete( final String uri )
        {
            return retrieveResponseFromRequest( title, description, "DELETE",
                    uri,
                    payload, payloadMediaType,
                    expectedResponseStatus, expectedMediaType, expectedHeaderField );
        }
    }

    private ClientResponse retrieveResponseFromRequest( final String title,
            final String description, final String method, final String uri,
            final Status responseCode, final MediaType accept, final String headerField )
    {
        ClientRequest request;
        try
        {
            System.out.println( "URI syntax exception: '" + uri + "'" );
            request = ClientRequest.create().accept( accept ).build(
                    new URI( uri ), method );
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( e );
        }
        return retrieveResponse( title, description, uri, responseCode, accept,
                headerField, request );
    }

    private ClientResponse retrieveResponseFromRequest( final String title,
            final String description, final String method, final String uri,
            final String payload, final MediaType payloadType,
            final Status responseCode, final MediaType accept, final String headerField )
    {
        ClientRequest request;
        try
        {
            request = ClientRequest.create().type( payloadType ).accept( accept ).entity(
                    payload ).build( new URI( uri ), method );
        }
        catch ( URISyntaxException e )
        {
            System.out.println( "URI syntax exception: '" + uri + "'" );
            throw new RuntimeException( e );
        }
        return retrieveResponse( title, description, uri, responseCode, accept,
                headerField, request );
    }

    private ClientResponse retrieveResponse( final String title,
            final String description, final String uri,
            final Response.Status responseCode, final MediaType type,
            final String headerField, final ClientRequest request )
    {
        DocumentationData data = new DocumentationData();
        getRequestHeaders( data, request.getHeaders() );
        if ( request.getEntity() != null )
        {
            data.setPayload( String.valueOf( request.getEntity() ) );
        }
        Client client = new Client();
        ClientResponse response = client.handle( request );
        assertEquals( responseCode.getStatusCode(), response.getStatus() );
        if ( response.getType() != null )
        {
            assertEquals( type, response.getType() );
        }
        if ( headerField != null )
        {
            assertNotNull( response.getHeaders().get( headerField ) );
        }
        data.setTitle( title );
        data.setDescription( description );
        data.setMethod( request.getMethod() );
        data.setRelUri( uri.substring( functionalTestHelper.dataUri().length() - 9 ) );
        data.setUri( uri );
        data.setStatus( responseCode );
        if ( response.hasEntity() && response.getStatus() != 204 )
        {
            data.setEntity( response.getEntity( String.class ) );
        }
        data.setHeaderField( headerField );
        if ( headerField != null )
        {
            getResponseHeaders( data, response.getHeaders(),
                    Arrays.asList( new String[] { headerField } ) );
        }
        else
        {
            getResponseHeaders( data, response.getHeaders(),
                    Collections.<String>emptyList() );
        }
        document( data );
        return response;
    }

    private void getResponseHeaders( final DocumentationData data,
            final MultivaluedMap<String, String> headers,
            final List<String> additionalFilter )
    {
        data.setResponseHeaders( getHeaders( headers, RESPONSE_HEADERS,
                additionalFilter ) );
    }

    private void getRequestHeaders( final DocumentationData data,
            final MultivaluedMap<String, Object> headers )
    {
        data.setRequestHeaders( getHeaders( headers, REQUEST_HEADERS,
                Collections.<String>emptyList() ) );
    }

    private <T> Map<String, String> getHeaders(
            final MultivaluedMap<String, T> headers, final List<String> filter,
            final List<String> additionalFilter )
            {
        Map<String, String> filteredHeaders = new TreeMap<String, String>();
        for ( Entry<String, List<T>> header : headers.entrySet() )
        {
            if ( filter.contains( header.getKey() )
                    || additionalFilter.contains( header.getKey() ) )
            {
                String values = "";
                for ( T value : header.getValue() )
                {
                    if ( !values.isEmpty() )
                    {
                        values += ", ";
                    }
                    values += String.valueOf( value );
                }
                filteredHeaders.put( header.getKey(), values );
            }
        }
        return filteredHeaders;
            }

    private class DocumentationData
    {
        public String headerField;
        public String payload;
        public String title;
        public String description;
        public String uri;
        public String method;
        public Status status;
        public String entity;
        public String relUri;
        public Map<String, String> requestHeaders;
        public Map<String, String> responseHeaders;

        public void setHeaderField( final String headerField )
        {
            this.headerField = headerField;
        }

        public void setPayload( final String payload )
        {
            this.payload = payload;
        }

        public void setDescription( final String description )
        {
            this.description = description;
        }

        public void setTitle( final String title )
        {
            this.title = title;
        }

        public void setUri( final String uri )
        {
            this.uri = uri;
        }

        public void setMethod( final String method )
        {
            this.method = method;
        }

        public void setStatus( final Status responseCode )
        {
            this.status = responseCode;

        }

        public void setEntity( final String entity )
        {
            this.entity = entity;
        }

        public void setRelUri( final String relUri )
        {
            this.relUri = relUri;

        }

        public void setResponseHeaders( final Map<String, String> response )
        {
            responseHeaders = response;
        }

        public void setRequestHeaders( final Map<String, String> request )
        {
            requestHeaders = request;
        }
    }

    private FileWriter fw;

    private final FunctionalTestHelper functionalTestHelper;

    public DocumentationOutput( final FunctionalTestHelper functionalTestHelper )
    {
        this.functionalTestHelper = functionalTestHelper;
    }

    protected void document( final DocumentationData data )
    {
        try
        {
            if ( data.title == null )
            {
                return;
            }
            File dirs = new File( "target/rest-api" );
            if ( !dirs.exists() )
            {
                dirs.mkdirs();
            }
            String name = data.title.replace( " ", "-" ).toLowerCase();
            File out = new File( dirs, name + ".txt" );
            out.createNewFile();

            fw = new FileWriter( out, false );

            line( "[[rest-api-" + name + "]]" );
            line( "== " + data.title + " ==" );
            line( "" );
            line( "_Example request_" );
            line( "" );
            line( "* *+" + data.method + "+*  +" + data.uri + "+" );
            if ( data.requestHeaders != null )
            {
                for ( Entry<String, String> header : data.requestHeaders.entrySet() )
                {
                    line( "* *+" + header.getKey() + ":+* +"
                            + header.getValue() + "+" );
                }
            }
            if ( data.payload != null && !data.payload.equals( "null" ) )
            {
                line( "[source,javascript]" );
                line( "----" );
                line( data.payload );
                line( "----" );
            }
            line( "" );
            line( "_Example response_" );
            line( "" );
            line( "* *+" + data.status.getStatusCode() + ":+* +"
                    + data.status.name() + "+" );
            if ( data.responseHeaders != null )
            {
                for ( Entry<String, String> header : data.responseHeaders.entrySet() )
                {
                    line( "* *+" + header.getKey() + ":+* +"
                            + header.getValue() + "+" );
                }
            }
            if ( data.entity != null )
            {
                line( "[source,javascript]" );
                line( "----" );
                line( data.entity );
                line( "----" );
                line( "" );
            }
            fw.flush();
            fw.close();
        }
        catch ( IOException e )
        {
            fail();
            e.printStackTrace();
        }

    }

    private void line( final String string )
    {
        try
        {
            fw.append( string + "\n" );
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public Statement apply( final Statement base, final FrameworkMethod method,
            final Object target )
    {
        System.out.println( String.format( "%s %s %s", base, method.getName(),
                target ) );
        return new Statement()
        {

            @Override
            public void evaluate() throws Throwable
            {
                // TODO Auto-generated method stub

            }
        };
    }
}