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
package org.jclouds.aliyun.ecs.compute.functions;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jclouds.aliyun.ecs.domain.Instance;
import org.jclouds.aliyun.ecs.domain.regionscoped.RegionAndId;
import org.jclouds.collect.Memoized;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.functions.GroupNamingConvention;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.Location;
import org.jclouds.location.predicates.LocationPredicates;
import org.jclouds.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

/**
 * Transforms an {@link Instance} to the jclouds portable model.
 */
@Singleton
public class InstanceToNodeMetadata implements Function<Instance, NodeMetadata> {

   private final InstanceTypeToHardware instanceTypeToHardware;
   private final Supplier<Map<String, ? extends Image>> images;
   private final Supplier<Set<? extends Location>> locations;
   private final Function<Instance.Status, NodeMetadata.Status> toPortableStatus;
   private final GroupNamingConvention groupNamingConvention;
   @Resource @Named(ComputeServiceConstants.COMPUTE_LOGGER) protected Logger logger = Logger.NULL;

   @Inject
   public InstanceToNodeMetadata(InstanceTypeToHardware instanceTypeToHardware,
                                 Supplier<Map<String, ? extends Image>> images, @Memoized Supplier<Set<? extends Location>> locations,
                                 Function<Instance.Status, NodeMetadata.Status> toPortableStatus,
                                 GroupNamingConvention.Factory groupNamingConvention) {
      this.instanceTypeToHardware = instanceTypeToHardware;
      this.images = checkNotNull(images, "images cannot be null");
      this.locations = locations;
      this.toPortableStatus = toPortableStatus;
      this.groupNamingConvention = groupNamingConvention.createWithoutPrefix();
   }

   @Override
   public NodeMetadata apply(Instance from) {
      // TODO remove
      String group = groupNamingConvention.extractGroup(from.instanceName());
      ImmutableMap<String, String> userMetadata = ImmutableMap.of("test", group);
      ImmutableSet<String> tags = ImmutableSet.of(group);
      // TODO end

      NodeMetadataBuilder builder = new NodeMetadataBuilder();

      Optional<? extends Image> image = findImage(from.imageId());
      if (image.isPresent()) {
         builder.imageId(image.get().getId());
         builder.operatingSystem(image.get().getOperatingSystem());
      } else {
         logger.info(">> image with id %s for instance %s was not found. "
                     + "This might be because the image that was used to create the instance has a new id.",
               from.instanceType(), from.instanceId());
      }

      builder.id(RegionAndId.slashEncodeRegionAndId(from.regionId(), from.instanceId()));
      builder.providerId(from.instanceId());
      builder.name(from.instanceName());
      builder.hostname(String.format("%s", from.hostname()));
      builder.group(groupNamingConvention.extractGroup(from.instanceName()));
      builder.status(toPortableStatus.apply(from.status()));
      builder.privateAddresses(from.innerIpAddress().entrySet().iterator().next().getValue());
      builder.publicAddresses(from.publicIpAddress().entrySet().iterator().next().getValue());
      builder.location(from(locations.get()).firstMatch(LocationPredicates.idEquals(from.regionId())).orNull());
      //            .hardware(instanceTypeToHardware.apply(input.plan()))
      //            .tags(Iterables.transform(from.tags().entrySet().iterator().next().getValue(), new Function<Tag, String>() {
      //               @Override
      //               public String apply(Tag input) {
      //                  return input.tagKey();
      //               }
      //            }))
      builder.tags(tags);
      builder.userMetadata(userMetadata);
      NodeMetadata nodeMetadata = builder.build();
      return nodeMetadata;
   }

   private Optional<? extends Image> findImage(String instanceType) {
      return Optional.fromNullable(images.get().get(instanceType));
   }

}
