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
package org.jclouds.aliyun.ecs.compute;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.jclouds.aliyun.ecs.ECSComputeServiceApi;
import org.jclouds.aliyun.ecs.compute.strategy.CleanupResources;
import org.jclouds.aliyun.ecs.domain.AvailableResource;
import org.jclouds.aliyun.ecs.domain.AvailableZone;
import org.jclouds.aliyun.ecs.domain.Image;
import org.jclouds.aliyun.ecs.domain.Instance;
import org.jclouds.aliyun.ecs.domain.InstanceRequest;
import org.jclouds.aliyun.ecs.domain.InstanceType;
import org.jclouds.aliyun.ecs.domain.Region;
import org.jclouds.aliyun.ecs.domain.SupportedResource;
import org.jclouds.aliyun.ecs.domain.options.CreateInstanceOptions;
import org.jclouds.aliyun.ecs.domain.options.ListInstancesOptions;
import org.jclouds.aliyun.ecs.domain.options.TagOptions;
import org.jclouds.aliyun.ecs.domain.regionscoped.RegionAndId;
import org.jclouds.aliyun.ecs.compute.options.ECSServiceTemplateOptions;
import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_NODE_RUNNING;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_NODE_SUSPENDED;

/**
 * defines the connection between the {@link ECSComputeServiceApi} implementation and
 * the jclouds {@link org.jclouds.compute.ComputeService}
 */
@Singleton
public class ECSComputeServiceAdapter implements ComputeServiceAdapter<Instance, InstanceType, Image, Region> {

   private final ECSComputeServiceApi api;
   private final Predicate<String> instanceRunningPredicate;
   private final Predicate<String> instanceSuspendedPredicate;

   private final Supplier<Set<String>> regionIds;
   private final CleanupResources cleanupResources;

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   @Inject
   ECSComputeServiceAdapter(ECSComputeServiceApi api,
                            @Named(TIMEOUT_NODE_RUNNING) Predicate<String> instanceRunningPredicate,
                            @Named(TIMEOUT_NODE_SUSPENDED) Predicate<String> instanceSuspendedPredicate,
                            @org.jclouds.location.Region Supplier<Set<String>> regionIds,
                            CleanupResources cleanupResources) {
      this.api = api;
      this.instanceRunningPredicate = instanceRunningPredicate;
      this.instanceSuspendedPredicate = instanceSuspendedPredicate;
      this.regionIds = regionIds;
      this.cleanupResources = cleanupResources;
   }

   @Override
   public NodeAndInitialCredentials<Instance> createNodeWithGroupEncodedIntoName(String group, String name, Template template) {
      String instanceType = template.getHardware().getId();
      String regionId = template.getLocation().getId();
      String imageId = template.getImage().getId();

      ECSServiceTemplateOptions templateOptions = template.getOptions().as(ECSServiceTemplateOptions.class);

      String keyPairName = templateOptions.getKeyPairName();
      String securityGroupId = Iterables.getOnlyElement(templateOptions.getGroups());
      String vSwitchId = templateOptions.getVSwitchId();
      Map<String, String> tags = tagsAsValuesOfEmptyString(templateOptions);
      TagOptions tagOptions = TagOptions.Builder.keys(tags.keySet());

      InstanceRequest instanceRequest = api.instanceApi().create(regionId, imageId, securityGroupId, name, instanceType,
              CreateInstanceOptions.Builder
                      .vSwitchId(vSwitchId)
                      .internetChargeType("PayByTraffic")
                      .internetMaxBandwidthOut(5)
                      .instanceChargeType("PostPaid")
                      .instanceName(name)
                      .keyPairName(keyPairName)
                      .tagOptions(tagOptions)
      );
      String regionAndInstanceId = RegionAndId.slashEncodeRegionAndId(regionId, instanceRequest.getInstanceId());
      instanceSuspendedPredicate.apply(regionAndInstanceId);
      api.instanceApi().allocatePublicIpAddress(regionId, instanceRequest.getInstanceId());
      api.instanceApi().powerOn(instanceRequest.getInstanceId());
      instanceRunningPredicate.apply(regionAndInstanceId);
      Instance instance = Iterables.get(api.instanceApi().list(regionId, ListInstancesOptions.Builder.instanceIds(instanceRequest.getInstanceId())), 0);

      // Safe to pass null credentials here, as jclouds will default populate
      // the node with the default credentials from the image, or the ones in
      // the options, if provided.
      return new NodeAndInitialCredentials<Instance>(instance,
              RegionAndId.slashEncodeRegionAndId(regionId, instanceRequest.getInstanceId()), null);
   }

   @Override
   public Iterable<InstanceType> listHardwareProfiles() {
      final ImmutableSet.Builder<String> instanceTypeIds = ImmutableSet.builder();
      final List<String> availableLocationNames = newArrayList(
              transform(listLocations(), new Function<Region, String>() {
                 @Override
                 public String apply(Region location) {
                    return location.regionId();
                 }
              }));

      // TODO probably need to be per zone as each availability zone can have a different list of supported resources
      for (String regionId : availableLocationNames) {
         instanceTypeIds.addAll(getInstanceTypeId(regionId));
      }

      List<InstanceType> instanceTypes = FluentIterable.from(api.instanceApi().listTypes())
              .filter(new Predicate<InstanceType>() {
                 @Override
                 public boolean apply(@Nullable InstanceType input) {
                    return contains(instanceTypeIds.build(), input.instanceTypeId());
                 }
              }).toList();

      return instanceTypes;
   }

   private List<String> getInstanceTypeId(String regionId) {
      List<String> instanceTypeIds = Lists.newArrayList();
      for (AvailableZone availableZone : api.instanceApi().listInstanceTypesByAvailableZone(regionId)) {
         for (AvailableResource availableResource : availableZone.availableResources().get("AvailableResource")) {
            for (SupportedResource supportedResource : availableResource.supportedResources()
                    .get("SupportedResource")) {
               if ("Available".equals(supportedResource.status())) {
                  instanceTypeIds.add(supportedResource.value());
               }
            }
         }
      }
      return instanceTypeIds;
   }

   @Override
   public Iterable<Image> listImages() {
      final ImmutableList.Builder<Image> images = ImmutableList.builder();
      final List<String> availableLocationNames = newArrayList(
              transform(listLocations(), new Function<Region, String>() {
                 @Override
                 public String apply(Region location) {
                    return location.regionId();
                 }
              }));

      for (String regionId : availableLocationNames) {
         images.addAll(api.imageApi().list(regionId).concat());
      }
      return images.build();
   }

   @Override
   public Image getImage(final String id) {
      Optional<Image> firstInterestingImage = Iterables.tryFind(listImages(), new Predicate<Image>() {
         public boolean apply(Image input) {
            return input.imageId().equals(id);
         }
      });
      if (!firstInterestingImage.isPresent()) {
         throw new IllegalStateException("Cannot find image with the required slug " + id);
      }
      return firstInterestingImage.get();
   }

   @Override
   public Iterable<Region> listLocations() {
      return FluentIterable.from(api.regionAndZoneApi().describeRegions()).filter(new Predicate<Region>() {
         @Override
         public boolean apply(Region region) {
            return regionIds.get().isEmpty() ? true : regionIds.get().contains(region.regionId());
         }
      }).toList();
   }

   @Override
   public Instance getNode(final String id) {
      RegionAndId regionAndId = RegionAndId.fromSlashEncoded(id);
      return Iterables.getFirst(api.instanceApi().list(regionAndId.regionId(),
              ListInstancesOptions.Builder.instanceIds(regionAndId.id())),
              null);
   }

   @Override
   public void destroyNode(String id) {
      checkState(cleanupResources.cleanupNode(id), "server(%s) and its resources still there after deleting!?", id);
   }

   @Override
   public void rebootNode(String id) {
      api.instanceApi().reboot(id);
   }

   @Override
   public void resumeNode(String id) {
      api.instanceApi().powerOn(id);
   }

   @Override
   public void suspendNode(String id) {
      api.instanceApi().powerOff(id);
   }

   @Override
   public Iterable<Instance> listNodes() {
      final ImmutableList.Builder<Instance> instances = ImmutableList.builder();
      final List<String> availableLocationNames = newArrayList(
              transform(listLocations(), new Function<Region, String>() {
                 @Override
                 public String apply(Region location) {
                    return location.regionId();
                 }
              }));

      for (String regionId : availableLocationNames) {
         instances.addAll(api.instanceApi().list(regionId).concat());
      }
      return instances.build();
   }

   @Override
   public Iterable<Instance> listNodesByIds(final Iterable<String> ids) {
      return filter(listNodes(), new Predicate<Instance>() {
         @Override
         public boolean apply(Instance instance) {
            return contains(ids, String.valueOf(instance.instanceId()));
         }
      });
   }

   public static Map<String, String> tagsAsValuesOfEmptyString(TemplateOptions options) {
      ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String> builder();
      for (String tag : options.getTags())
         builder.put(tag, "");
      return builder.build();
   }

}
