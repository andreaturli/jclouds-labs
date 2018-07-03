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
package org.jclouds.aliyun.ecs.compute.strategy;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.jclouds.aliyun.ecs.ECSComputeServiceApi;
import org.jclouds.aliyun.ecs.domain.Instance;
import org.jclouds.aliyun.ecs.domain.InstanceStatus;
import org.jclouds.aliyun.ecs.domain.Request;
import org.jclouds.aliyun.ecs.domain.Tag;
import org.jclouds.aliyun.ecs.domain.internal.PaginatedCollection;
import org.jclouds.aliyun.ecs.domain.options.ListInstancesOptions;
import org.jclouds.aliyun.ecs.domain.options.ListTagsOptions;
import org.jclouds.aliyun.ecs.domain.regionscoped.RegionAndId;
import org.jclouds.aliyun.ecs.predicates.InstanceStatusPredicate;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;
import org.jclouds.rest.AuthorizationException;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_NODE_SUSPENDED;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_NODE_TERMINATED;

@Singleton
public class CleanupResources {

   public static final String RESOURCE_TYPE = "securitygroup";
   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final ECSComputeServiceApi api;
   private final Predicate<String> instanceSuspendedPredicate;
   private final Predicate<String> instanceTerminatedPredicate;

   @Inject
   public CleanupResources(ECSComputeServiceApi api,
                           @Named(TIMEOUT_NODE_SUSPENDED) Predicate<String> instanceSuspendedPredicate,
                           @Named(TIMEOUT_NODE_TERMINATED) Predicate<String> instanceTerminatedPredicate
   ) {
      this.api = api;
      this.instanceSuspendedPredicate = instanceSuspendedPredicate;
      this.instanceTerminatedPredicate = instanceTerminatedPredicate;
   }

   public boolean cleanupNode(final String id) {
      final RegionAndId regionAndId = RegionAndId.fromSlashEncoded(id);
      List<String> securityGroupIds = getSecurityGroupIdsUsedByNode(regionAndId);
      String instanceId = regionAndId.id();
      InstanceStatus instanceStatus = Iterables.tryFind(api.instanceApi().getStatus(regionAndId.regionId()),
              new InstanceStatusPredicate(instanceId)).orNull();
      if (instanceStatus == null) return true;
      if (InstanceStatus.Status.STOPPED != instanceStatus.status()) {
         logger.debug(">> powering off %s ...", id);
         api.instanceApi().powerOff(instanceId);
         instanceSuspendedPredicate.apply(id);
      }
      logger.debug(">> destroying %s ...", id);
      api.instanceApi().delete(instanceId);
      boolean instanceDeleted = instanceTerminatedPredicate.apply(id);

      for (String securityGroupId : securityGroupIds) {
         logger.debug(">> destroying security group %s ...", securityGroupId);
         if (cleanupSecurityGroupIfOrphaned(regionAndId.regionId(), securityGroupId)) {
            logger.debug(">> security group: (%s) has been deleted.", securityGroupId);
         } else {
            logger.warn(">> security group: (%s) has not been deleted.", securityGroupId);
         }
      }

      return instanceDeleted;
   }

   private List<String> getSecurityGroupIdsUsedByNode(RegionAndId regionAndId) {
      List<String> securityGroupIds = Lists.newArrayList();
      PaginatedCollection<Instance> instances = api.instanceApi().list(regionAndId.regionId(), ListInstancesOptions.Builder.instanceIds(regionAndId.id()));
      if (instances.isEmpty()) return securityGroupIds;

      Instance instance = Iterables.get(instances, 0);
      if (instance != null && !instance.securityGroupIds().isEmpty()) {
         securityGroupIds = instance.securityGroupIds().values().iterator().next();
      }
      return securityGroupIds;
   }


   public boolean cleanupSecurityGroupIfOrphaned(final String regionId, final String securityGroupId) {
      try {
         return api.tagApi().list(regionId, ListTagsOptions.Builder.resourceId(securityGroupId).resourceType(RESOURCE_TYPE)).firstMatch(new Predicate<Tag>() {
            @Override
            public boolean apply(@javax.annotation.Nullable Tag input) {
               return input.key().equalsIgnoreCase("owner") && input.value().equalsIgnoreCase("jclouds");
            }
         }).transform(new Function<Tag, Boolean>() {
            @Override
            public Boolean apply(@javax.annotation.Nullable Tag input) {
               Request request = api.securityGroupApi().delete(regionId, securityGroupId);
               return request != null;
            }
         }).or(Boolean.FALSE);
      } catch (AuthorizationException e) {
         logger.error(">> security group: (%s) can not be deleted.\nReason: %s", securityGroupId, e.getMessage());
         return Boolean.FALSE;
      }
   }

}
