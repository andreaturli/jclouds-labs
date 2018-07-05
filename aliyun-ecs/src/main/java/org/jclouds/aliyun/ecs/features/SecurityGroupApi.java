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
import org.jclouds.aliyun.ecs.domain.IpProtocol;
import org.jclouds.aliyun.ecs.domain.Permission;
import org.jclouds.aliyun.ecs.domain.Request;
import org.jclouds.aliyun.ecs.domain.SecurityGroup;
import org.jclouds.aliyun.ecs.domain.SecurityGroupRequest;
import org.jclouds.aliyun.ecs.domain.SecurityGroups;
import org.jclouds.aliyun.ecs.domain.options.CreateSecurityGroupOptions;
import org.jclouds.aliyun.ecs.domain.options.ListSecurityGroupsOptions;
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
import org.jclouds.rest.annotations.SelectJson;
import org.jclouds.rest.annotations.Transform;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@RequestFilters(FormSign.class)
@QueryParams(keys = { "Version", "Format", "SignatureVersion", "ServiceCode", "SignatureMethod" },
             values = {"{" + Constants.PROPERTY_API_VERSION + "}", "JSON", "1.0", "ecs", "HMAC-SHA1"})
public interface SecurityGroupApi {

   @Named("securityGroup:list")
   @GET
   @QueryParams(keys = "Action", values = "DescribeSecurityGroups")
   @ResponseParser(ParseSecurityGroups.class)
   @Fallback(Fallbacks.EmptyIterableWithMarkerOnNotFoundOr404.class)
   IterableWithMarker<SecurityGroup> list(@QueryParam("RegionId") String region, ListSecurityGroupsOptions options);

   @Named("securityGroup:list")
   @GET
   @QueryParams(keys = "Action", values = "DescribeSecurityGroups")
   @ResponseParser(ParseSecurityGroups.class)
   @Transform(ParseSecurityGroups.ToPagedIterable.class)
   @Fallback(Fallbacks.EmptyPagedIterableOnNotFoundOr404.class)
   PagedIterable<SecurityGroup> list(@QueryParam("RegionId") String region);

   @Singleton
   final class ParseSecurityGroups extends ParseJson<SecurityGroups> {

      @Inject
      ParseSecurityGroups(final Json json) {
         super(json, TypeLiteral.get(SecurityGroups.class));
      }

      static class ToPagedIterable extends Arg0ToPagedIterable<SecurityGroup, ParseSecurityGroups.ToPagedIterable> {

         private final ECSComputeServiceApi api;

         @Inject
         ToPagedIterable(ECSComputeServiceApi api) {
            this.api = api;
         }

         @Override
         protected Function<Object, IterableWithMarker<SecurityGroup>> markerToNextForArg0(final Optional<Object> arg0) {
            return new Function<Object, IterableWithMarker<SecurityGroup>>() {
               @Override
               public IterableWithMarker<SecurityGroup> apply(Object input) {
                  String regionId = arg0.get().toString();
                  ListSecurityGroupsOptions listSecurityGroupsOptions = ListSecurityGroupsOptions.Builder.paginationOptions(PaginationOptions.class.cast(input));
                  return api.securityGroupApi().list(regionId, listSecurityGroupsOptions);
               }
            };
         }
      }
   }

   @Named("securityGroup:get")
   @GET
   @QueryParams(keys = "Action", values = "DescribeSecurityGroupAttribute")
   @SelectJson("Permission")
   List<Permission> get(@QueryParam("RegionId") String region, @QueryParam("SecurityGroupId") String securityGroupId);

   @Named("securityGroup:create")
   @POST
   @QueryParams(keys = "Action", values = "CreateSecurityGroup")
   @Fallback(Fallbacks.EmptyListOnNotFoundOr404.class)
   SecurityGroupRequest create(@QueryParam("RegionId") String region);

   @Named("securityGroup:create")
   @POST
   @QueryParams(keys = "Action", values = "CreateSecurityGroup")
   @Fallback(Fallbacks.EmptyListOnNotFoundOr404.class)
   SecurityGroupRequest create(@QueryParam("RegionId") String region, CreateSecurityGroupOptions options);

   @Named("securityGroup:addInbound")
   @POST
   @QueryParams(keys = "Action", values = "AuthorizeSecurityGroup")
   @Fallback(Fallbacks.EmptyListOnNotFoundOr404.class)
   Request addInboundRule(@QueryParam("RegionId") String region, @QueryParam("SecurityGroupId") String securityGroupId,
                          @QueryParam("IpProtocol") IpProtocol ipProtocol, @QueryParam("PortRange") String portRange,
                          @QueryParam("SourceCidrIp") String sourceCidrIp);

   @Named("securityGroup:delete")
   @POST
   @QueryParams(keys = "Action", values = "DeleteSecurityGroup")
   @Fallback(Fallbacks.EmptyListOnNotFoundOr404.class)
   Request delete(@QueryParam("RegionId") String region, @QueryParam("SecurityGroupId") String securityGroupId);
}

