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
import org.jclouds.aliyun.ecs.domain.AllocatePublicIpAddressRequest;
import org.jclouds.aliyun.ecs.domain.AvailableZone;
import org.jclouds.aliyun.ecs.domain.Instance;
import org.jclouds.aliyun.ecs.domain.InstanceRequest;
import org.jclouds.aliyun.ecs.domain.InstanceStatus;
import org.jclouds.aliyun.ecs.domain.InstanceType;
import org.jclouds.aliyun.ecs.domain.Instances;
import org.jclouds.aliyun.ecs.domain.Request;
import org.jclouds.aliyun.ecs.domain.internal.PaginatedCollection;
import org.jclouds.aliyun.ecs.domain.options.CreateInstanceOptions;
import org.jclouds.aliyun.ecs.domain.options.ListInstancesOptions;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * https://www.alibabacloud.com/help/doc-detail/25500.htm?spm=a2c63.p38356.b99.287.129a44a8RBMBLH
 */
@Consumes(MediaType.APPLICATION_JSON)
@RequestFilters(FormSign.class)
@QueryParams(keys = { "Version", "Format", "SignatureVersion", "ServiceCode", "SignatureMethod" },
             values = {"{" + Constants.PROPERTY_API_VERSION + "}", "JSON", "1.0", "ecs", "HMAC-SHA1"})
public interface InstanceApi {

   @Named("instance:list")
   @GET
   @QueryParams(keys = "Action", values = "DescribeInstances")
   @ResponseParser(ParseInstances.class)
   @Fallback(Fallbacks.EmptyIterableWithMarkerOnNotFoundOr404.class)
   PaginatedCollection<Instance> list(@QueryParam("RegionId") String region, ListInstancesOptions... options);

   @Named("instance:list")
   @GET
   @QueryParams(keys = "Action", values = "DescribeInstances")
   @ResponseParser(ParseInstances.class)
   @Transform(ParseInstances.ToPagedIterable.class)
   @Fallback(Fallbacks.EmptyPagedIterableOnNotFoundOr404.class)
   PagedIterable<Instance> list(@QueryParam("RegionId") String region);

   @Named("instanceType:list")
   @GET
   @QueryParams(keys = "Action", values = "DescribeInstanceTypes")
   @SelectJson("InstanceType")
   @Fallback(Fallbacks.EmptyListOnNotFoundOr404.class)
   List<InstanceType> listTypes();

   @Named("instanceType:list")
   @GET
   @QueryParams(keys = { "Action", "DestinationResource", "IoOptimized" },
                values = { "DescribeAvailableResource", "InstanceType", "optimized" })
   @SelectJson("AvailableZone")
   @Fallback(Fallbacks.EmptyListOnNotFoundOr404.class)
   List<AvailableZone> listInstanceTypesByAvailableZone(@QueryParam("RegionId") String regionId);

   @Named("instance:create")
   @POST
   @Produces(MediaType.APPLICATION_JSON)
   @QueryParams(keys = "Action", values = "CreateInstance")
   InstanceRequest create(@QueryParam("RegionId") String regionId,
                          @QueryParam("ImageId") String imageId,
                          @QueryParam("SecurityGroupId") String securityGroupId,
                          @QueryParam("HostName") String hostname,
                          @QueryParam("InstanceType") String instanceType);

   @Named("instance:create")
   @POST
   @Produces(MediaType.APPLICATION_JSON)
   @QueryParams(keys = "Action", values = "CreateInstance")
   InstanceRequest create(@QueryParam("RegionId") String regionId,
                          @QueryParam("ImageId") String imageId,
                          @QueryParam("SecurityGroupId") String securityGroupId,
                          @QueryParam("HostName") String hostname,
                          @QueryParam("InstanceType") String instanceType, CreateInstanceOptions options);

   @Named("instance:allocatePublicIpAddress")
   @POST
   @Produces(MediaType.APPLICATION_JSON)
   @QueryParams(keys = "Action", values = "AllocatePublicIpAddress")
   AllocatePublicIpAddressRequest allocatePublicIpAddress(@QueryParam("RegionId") String regionId,
                                                          @QueryParam("InstanceId") String instanceId);

   @Named("instance:getStatus")
   @GET
   @QueryParams(keys = "Action", values = "DescribeInstanceStatus")
   @SelectJson("InstanceStatus")
   @Fallback(Fallbacks.EmptyListOnNotFoundOr404.class)
   List<InstanceStatus> getStatus(@QueryParam("RegionId") String regionId);

   /**
    * You can only release an instance that is in the Stopped (Stopped) status.
    *
    * @param instanceId
    */
   @Named("instance:delete")
   @POST
   @Produces(MediaType.APPLICATION_JSON)
   @QueryParams(keys = "Action", values = "DeleteInstance")
   Request delete(@QueryParam("InstanceId") String instanceId);

   @Named("instance:powerOff")
   @POST
   @Produces(MediaType.APPLICATION_JSON)
   @QueryParams(keys = "Action", values = "StopInstance")
   Request powerOff(@QueryParam("InstanceId") String instanceId);

   @Named("instance:powerOn")
   @POST
   @Produces(MediaType.APPLICATION_JSON)
   @QueryParams(keys = "Action", values = "StartInstance")
   Request powerOn(@QueryParam("InstanceId") String instanceId);

   @Named("instance:reboot")
   @POST
   @Produces(MediaType.APPLICATION_JSON)
   @QueryParams(keys = "Action", values = "RebootInstance")
   Request reboot(@QueryParam("InstanceId") String instanceId);

   @Singleton
   final class ParseInstances extends ParseJson<Instances> {

      @Inject
      ParseInstances(final Json json) {
         super(json, TypeLiteral.get(Instances.class));
      }

      static class ToPagedIterable extends Arg0ToPagedIterable<Instance, ParseInstances.ToPagedIterable> {

         private ECSComputeServiceApi api;

         @Inject
         ToPagedIterable(final ECSComputeServiceApi api) {
            this.api = api;
         }

         @Override
         protected Function<Object, IterableWithMarker<Instance>> markerToNextForArg0(final Optional<Object> arg0) {
            return new Function<Object, IterableWithMarker<Instance>>() {
               @Override
               public IterableWithMarker<Instance> apply(Object input) {
                  String regionId = arg0.get().toString();
                  ListInstancesOptions listInstancesOptions = ListInstancesOptions.Builder.paginationOptions(PaginationOptions.class.cast(input));
                  return api.instanceApi().list(regionId, listInstancesOptions);
               }
            };
         }
      }
   }
}

