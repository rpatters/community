/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.server.rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AdminEndpointsFunctionalTest extends FunctionalTestBase
{
    @Test
    public void shouldRespondWithIndexes() throws Exception
    {
//        ClientResponse response = Client.create().resource( managementUri() + "properties/neo4j-servers" ).get( ClientResponse.class );
//        System.out.println(response.getEntity( String.class ));
//        assertEquals( 200, response.getStatus() );
    }

}
