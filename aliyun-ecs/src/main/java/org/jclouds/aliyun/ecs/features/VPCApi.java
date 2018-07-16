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
import org.jclouds.aliyun.ecs.domain.Request;
import org.jclouds.aliyun.ecs.domain.VPC;
import org.jclouds.aliyun.ecs.domain.VPCRequest;
import org.jclouds.aliyun.ecs.domain.VPCs;
import org.jclouds.aliyun.ecs.domain.options.CreateVPCOptions;
import org.jclouds.aliyun.ecs.domain.options.ListVPCsOptions;
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
public interface VPCApi {

   @Named("vpc:list")
   @GET
   @QueryParams(keys = "Action", values = "DescribeVpcs")
   @ResponseParser(ParseVPCs.class)
   @Fallback(Fallbacks.EmptyIterableWithMarkerOnNotFoundOr404.class)
   IterableWithMarker<VPC> list(@QueryParam("RegionId") String region, ListVPCsOptions options);

   @Named("vpc:list")
   @GET
   @QueryParams(keys = "Action", values = "DescribeVpcs")
   @ResponseParser(ParseVPCs.class)
   @Transform(ParseVPCs.ToPagedIterable.class)
   @Fallback(Fallbacks.EmptyPagedIterableOnNotFoundOr404.class)
   PagedIterable<VPC> list(@QueryParam("RegionId") String region);

   @Singleton
   final class ParseVPCs extends ParseJson<VPCs> {

      @Inject
      ParseVPCs(final Json json) {
         super(json, TypeLiteral.get(VPCs.class));
      }

      static class ToPagedIterable extends Arg0ToPagedIterable<VPC, ToPagedIterable> {

         private final ECSComputeServiceApi api;

         @Inject
         ToPagedIterable(ECSComputeServiceApi api) {
            this.api = api;
         }

         @Override
         protected Function<Object, IterableWithMarker<VPC>> markerToNextForArg0(final Optional<Object> arg0) {
            return new Function<Object, IterableWithMarker<VPC>>() {
               @Override
               public IterableWithMarker<VPC> apply(Object input) {
                  String regionId = arg0.get().toString();
                  ListVPCsOptions listVPCsOptions = ListVPCsOptions.Builder.paginationOptions(PaginationOptions.class.cast(input));
                  return api.vpcApi().list(regionId, listVPCsOptions);
               }
            };
         }
      }
   }

   @Named("vpc:create")
   @POST
   @QueryParams(keys = "Action", values = "CreateVpc")
   @Fallback(Fallbacks.NullOnNotFoundOr404.class)
   VPCRequest create(@QueryParam("RegionId") String region);

   @Named("vpc:create")
   @POST
   @QueryParams(keys = "Action", values = "CreateVpc")
   @Fallback(Fallbacks.NullOnNotFoundOr404.class)
   VPCRequest create(@QueryParam("RegionId") String region, CreateVPCOptions vpcOptions);

   @Named("vpc:delete")
   @POST
   @QueryParams(keys = "Action", values = "DeleteVpc")
   @Fallback(Fallbacks.NullOnNotFoundOr404.class)
   Request delete(@QueryParam("RegionId") String region,
                  @QueryParam("VpcId") String vpcId);
}

