/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.azurecompute.arm.features;

import java.net.URI;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.jclouds.Fallbacks.EmptyListOnNotFoundOr404;
import org.jclouds.Fallbacks.NullOnNotFoundOr404;
import org.jclouds.azurecompute.arm.domain.Key;
import org.jclouds.azurecompute.arm.domain.Vault;
import org.jclouds.azurecompute.arm.domain.VaultProperties;
import org.jclouds.azurecompute.arm.filters.ApiVersionFilter;
import org.jclouds.azurecompute.arm.functions.URIParser;
import org.jclouds.oauth.v2.filters.OAuthFilter;
import org.jclouds.rest.annotations.EndpointParam;
import org.jclouds.rest.annotations.Fallback;
import org.jclouds.rest.annotations.MapBinder;
import org.jclouds.rest.annotations.PayloadParam;
import org.jclouds.rest.annotations.RequestFilters;
import org.jclouds.rest.annotations.ResponseParser;
import org.jclouds.rest.annotations.SelectJson;
import org.jclouds.rest.binders.BindToJsonPayload;

@RequestFilters({ OAuthFilter.class, ApiVersionFilter.class })
@Consumes(MediaType.APPLICATION_JSON)
public interface VaultApi {

   @Named("vault:list")
   @SelectJson("value")
   @GET
   @Path("/resourcegroups/{resourcegroup}/providers/Microsoft.KeyVault/vaults")
   @Fallback(EmptyListOnNotFoundOr404.class)
   List<Vault> listVaults();

   @Named("vault:create_or_update")
   @PUT
   @MapBinder(BindToJsonPayload.class)
   @Path("/resourcegroups/{resourcegroup}/providers/Microsoft.KeyVault/vaults/{vaultName}")
   Vault createOrUpdateVault(@PathParam("vaultName") String vaultName,
                             @PayloadParam("location") String location,
                             @PayloadParam("properties") VaultProperties properties);

   @Named("vault:get")
   @Path("/resourcegroups/{resourcegroup}/providers/Microsoft.KeyVault/vaults/{vaultName}")
   @GET
   @Fallback(NullOnNotFoundOr404.class)
   Vault getVault(@PathParam("vaultName") String vaultName);

   @Named("vault:delete")
   @Path("/resourcegroups/{resourcegroup}/providers/Microsoft.KeyVault/vaults/{vaultName}")
   @DELETE
   @ResponseParser(URIParser.class)
   @Fallback(NullOnNotFoundOr404.class)
   URI deleteVault(@PathParam("vaultName") String vaultName);

   @Named("key:list")
   @SelectJson("value")
   @GET
   @Fallback(EmptyListOnNotFoundOr404.class)
   @Path("/keys")
   List<Key> listKeys(@EndpointParam URI vaultBaseUrl);

   @Named("key:create")
   @POST
   @MapBinder(BindToJsonPayload.class)
   @Path("/keys/{keyName}/create")
   Key createKey(@EndpointParam URI vaultBaseUrl, @PathParam("keyName") String keyName);

   @Named("key:get")
   @Path("/keys/{keyName}")
   @GET
   @Fallback(NullOnNotFoundOr404.class)
   Key getKey(@EndpointParam URI vaultBaseUrl, @PathParam("keyName") String keyName);

   @Named("key:delete")
   @Path("/keys/{keyName}")
   @DELETE
   @ResponseParser(URIParser.class)
   @Fallback(NullOnNotFoundOr404.class)
   URI deleteKey(@PathParam("keyName") String keyName);
}
