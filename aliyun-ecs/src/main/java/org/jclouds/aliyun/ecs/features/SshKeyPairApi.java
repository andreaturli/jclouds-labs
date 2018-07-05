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
package org.jclouds.aliyun.ecs.features;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.inject.TypeLiteral;
import org.jclouds.Constants;
import org.jclouds.Fallbacks;
import org.jclouds.aliyun.ecs.ECSComputeServiceApi;
import org.jclouds.aliyun.ecs.domain.KeyPair;
import org.jclouds.aliyun.ecs.domain.KeyPairRequest;
import org.jclouds.aliyun.ecs.domain.KeyPairs;
import org.jclouds.aliyun.ecs.domain.Request;
import org.jclouds.aliyun.ecs.domain.options.DeleteKeyPairOptions;
import org.jclouds.aliyun.ecs.domain.options.ListKeyPairsOptions;
import org.jclouds.aliyun.ecs.domain.options.PaginationOptions;
import org.jclouds.aliyun.ecs.filters.FormSign;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.collect.PagedIterable;
import org.jclouds.collect.internal.Arg0ToPagedIterable;
import org.jclouds.http.functions.ParseJson;
import org.jclouds.json.Json;
import org.jclouds.rest.annotations.Fallback;
import org.jclouds.rest.annotations.QueryParams;
import org.jclouds.rest.annotations.RequestFilters;
import org.jclouds.rest.annotations.ResponseParser;
import org.jclouds.rest.annotations.Transform;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@RequestFilters(FormSign.class)
@QueryParams(keys = { "Version", "Format", "SignatureVersion", "ServiceCode", "SignatureMethod" },
             values = {"{" + Constants.PROPERTY_API_VERSION + "}", "JSON", "1.0", "ecs", "HMAC-SHA1"})
public interface SshKeyPairApi {

   @Named("sshKeyPair:list")
   @GET
   @QueryParams(keys = "Action", values = "DescribeKeyPairs")
   @ResponseParser(ParseKeyPairs.class)
   @Fallback(Fallbacks.EmptyIterableWithMarkerOnNotFoundOr404.class)
   IterableWithMarker<KeyPair> list(@QueryParam("RegionId") String region, ListKeyPairsOptions options);

   @Named("sshKeyPair:list")
   @GET
   @QueryParams(keys = "Action", values = "DescribeKeyPairs")
   @ResponseParser(ParseKeyPairs.class)
   @Transform(ParseKeyPairs.ToPagedIterable.class)
   @Fallback(Fallbacks.EmptyPagedIterableOnNotFoundOr404.class)
   PagedIterable<KeyPair> list(@QueryParam("RegionId") String region);

   @Singleton
   final class ParseKeyPairs extends ParseJson<KeyPairs> {

      @Inject
      ParseKeyPairs(final Json json) {
         super(json, TypeLiteral.get(KeyPairs.class));
      }

      static class ToPagedIterable extends Arg0ToPagedIterable<KeyPair, ParseKeyPairs.ToPagedIterable> {

         private final ECSComputeServiceApi api;

         @Inject
         ToPagedIterable(ECSComputeServiceApi api) {
            this.api = api;
         }

         @Override
         protected Function<Object, IterableWithMarker<KeyPair>> markerToNextForArg0(final Optional<Object> arg0) {
            return new Function<Object, IterableWithMarker<KeyPair>>() {
               @Override
               public IterableWithMarker<KeyPair> apply(Object input) {
                  String regionId = arg0.get().toString();
                  ListKeyPairsOptions listKeyPairsOptions = ListKeyPairsOptions.Builder.paginationOptions(PaginationOptions.class.cast(input));
                  return api.sshKeyPairApi().list(regionId, listKeyPairsOptions);
               }
            };
         }
      }
   }

   @Named("sshKeyPair:create")
   @POST
   @QueryParams(keys = "Action", values = "CreateKeyPair")
   KeyPairRequest create(@QueryParam("RegionId") String region, @QueryParam("KeyPairName") String keyPairName);

   @Named("sshKeyPair:import")
   @POST
   @QueryParams(keys = "Action", values = "ImportKeyPair")
   KeyPair importKeyPair(@QueryParam("RegionId") String region,
                         @QueryParam("PublicKeyBody") String publicKeyBody,
                         @QueryParam("KeyPairName") String keyPairName);

   @Named("sshKeyPair:delete")
   @POST
   @QueryParams(keys = "Action", values = "DeleteKeyPairs")
   Request delete(@QueryParam("RegionId") String region);

   @Named("sshKeyPair:delete")
   @POST
   @QueryParams(keys = "Action", values = "DeleteKeyPairs")
   Request delete(@QueryParam("RegionId") String region, DeleteKeyPairOptions deleteOptions);
}

