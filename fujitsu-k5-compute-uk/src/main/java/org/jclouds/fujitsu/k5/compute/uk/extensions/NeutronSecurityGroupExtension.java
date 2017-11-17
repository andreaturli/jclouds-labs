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
package org.jclouds.fujitsu.k5.compute.uk.extensions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;

import org.jclouds.Constants;
import org.jclouds.Context;
import org.jclouds.compute.domain.SecurityGroup;
import org.jclouds.compute.domain.SecurityGroupBuilder;
import org.jclouds.compute.functions.GroupNamingConvention;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.Location;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.location.Region;
import org.jclouds.logging.Logger;
import org.jclouds.net.domain.IpPermission;
import org.jclouds.net.domain.IpProtocol;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Rule;
import org.jclouds.openstack.neutron.v2.domain.RuleDirection;
import org.jclouds.openstack.neutron.v2.domain.RuleEthertype;
import org.jclouds.openstack.neutron.v2.domain.RuleProtocol;
import org.jclouds.openstack.neutron.v2.features.SecurityGroupApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.compute.extensions.NovaSecurityGroupExtension;
import org.jclouds.openstack.nova.v2_0.domain.ServerWithSecurityGroups;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.RegionAndId;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.RegionAndName;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.SecurityGroupInRegion;
import org.jclouds.openstack.nova.v2_0.extensions.ServerWithSecurityGroupsApi;
import org.jclouds.rest.ApiContext;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * An extension to compute service to allow for the manipulation of {@link org.jclouds.compute.domain.SecurityGroup}s. Implementation
 * is optional by providers.
 */
public class NeutronSecurityGroupExtension extends NovaSecurityGroupExtension {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final Supplier<Context> neutronApiContextSupplier;
   private final Supplier<Map<String, Location>> locationIndex;

   @Inject
   public NeutronSecurityGroupExtension(NovaApi api,
                                        @Named(Constants.PROPERTY_USER_THREADS) ListeningExecutorService userExecutor,
                                        @Region Supplier<Set<String>> regionIds,
                                        Function<SecurityGroupInRegion, SecurityGroup> groupConverter,
                                        LoadingCache<RegionAndName, SecurityGroupInRegion> groupCreator,
                                        GroupNamingConvention.Factory namingConvention,
                                        Supplier<Context> neutronApiContextSupplier,
                                        //@Named("fujitsu-k5-networking-uk") Supplier<Context> neutronApiContextSupplier,
                                        Supplier<Map<String, Location>> locationIndex) {
      super(api, userExecutor, regionIds, groupConverter, groupCreator, namingConvention);
      this.neutronApiContextSupplier = neutronApiContextSupplier;
      this.locationIndex = checkNotNull(locationIndex, "locationIndex");
   }

   @Override
   public SecurityGroup createSecurityGroup(String name, Location location) {
      String region = location.getId();
      if (region == null) {
         return null;
      }
      logger.debug(">> creating security group %s in %s...", name, location);

      String markerGroup = namingConvention.create().sharedNameForGroup(name);
//      RegionSecurityGroupNameAndPorts regionAndName = new RegionSecurityGroupNameAndPorts(region, markerGroup, ImmutableSet.<Integer> of());
//      SecurityGroupInRegion rawGroup = groupCreator.getUnchecked(regionAndName);
//      return groupConverter.apply(rawGroup);
      SecurityGroupApi securityGroupApi = getSecurityGroupApi(region);
      org.jclouds.openstack.neutron.v2.domain.SecurityGroup group = securityGroupApi.create(org.jclouds.openstack.neutron.v2.domain.SecurityGroup.CreateSecurityGroup.createBuilder()
              .name(markerGroup)
              .description("security group created by jclouds")
              .build());
      return toSecurityGroup(location).apply(group);
   }

   @Override
   public Set<SecurityGroup> listSecurityGroups() {
      Set<SecurityGroup> securityGroups = Sets.newHashSet();

      for (String regionId : regionIds.get()) {
         Location location = locationIndex.get().get(regionId);
         securityGroups.addAll(listSecurityGroupsInLocation(location));
      }
      return ImmutableSet.copyOf(securityGroups);
   }


   @Override
   public Set<SecurityGroup> listSecurityGroupsInLocation(final Location location) {
      String region = location.getId();
      if (region == null) {
         return ImmutableSet.of();
      }
      return getSecurityGroupApi(region).listSecurityGroups().concat().transform(toSecurityGroup(location)).toSet();
   }

   @Override
   public Set<SecurityGroup> listSecurityGroupsForNode(String id) {
      RegionAndId regionAndId = RegionAndId.fromSlashEncoded(checkNotNull(id, "id"));
      String region = regionAndId.getRegion();
      String instanceId = regionAndId.getId();

      Optional<? extends ServerWithSecurityGroupsApi> serverApi = api.getServerWithSecurityGroupsApi(region);
      SecurityGroupApi securityGroupApi = getSecurityGroupApi(region);

      ServerWithSecurityGroups instance = serverApi.get().get(instanceId);
      if (instance == null) {
         return ImmutableSet.of();
      }
      final FluentIterable<org.jclouds.openstack.neutron.v2.domain.SecurityGroup> allGroups = securityGroupApi.listSecurityGroups().concat();
      Location location = locationIndex.get().get(region);
      return ImmutableSet.copyOf(transform(filter(allGroups, notNull()), toSecurityGroup(location)));
   }

   @Override
   public SecurityGroup getSecurityGroupById(String id) {
      RegionAndId regionAndId = RegionAndId.fromSlashEncoded(checkNotNull(id, "id"));
      String region = regionAndId.getRegion();
      String groupId = regionAndId.getId();

      SecurityGroupApi securityGroupApi = getSecurityGroupApi(region);

//      final FluentIterable<org.jclouds.openstack.nova.v2_0.domain.SecurityGroup> allGroups = securityGroupApi.listSecurityGroups();
//      SecurityGroupInRegion rawGroup = new SecurityGroupInRegion(securityGroupApi.get(groupId), region, allGroups);

//      return groupConverter.apply(rawGroup);
      Location location = locationIndex.get().get(region);
      return toSecurityGroup(location).apply(securityGroupApi.getSecurityGroup(groupId));
   }

   @Override
   public boolean removeSecurityGroup(String id) {
      checkNotNull(id, "id");
      RegionAndId regionAndId = RegionAndId.fromSlashEncoded(id);
      String region = regionAndId.getRegion();
      String groupId = regionAndId.getId();

      SecurityGroupApi securityGroupApi = getSecurityGroupApi(region);

      // Would be nice to delete the group and invalidate the cache atomically - i.e. use a mutex.
      // Will make sure that a create operation in parallel won't see inconsistent state.

      boolean deleted = securityGroupApi.deleteSecurityGroup(groupId);

//      for (SecurityGroupInRegion cachedSg : groupCreator.asMap().values()) {
//         if (groupId.equals(cachedSg.getSecurityGroup().getId())) {
//            String groupName = cachedSg.getName();
//            groupCreator.invalidate(new RegionSecurityGroupNameAndPorts(region, groupName, ImmutableSet.<Integer>of()));
//            break;
//         }
//      }

      return deleted;
   }

   @Override
   public SecurityGroup addIpPermission(IpPermission ipPermission, SecurityGroup group) {
      String region = group.getLocation().getId();
      RegionAndId groupRegionAndId = RegionAndId.fromSlashEncoded(group.getId());
      String id = groupRegionAndId.getId();
      SecurityGroupApi securityGroupApi = getSecurityGroupApi(region);


      if (!ipPermission.getCidrBlocks().isEmpty()) {
         for (String cidr : ipPermission.getCidrBlocks()) {
            securityGroupApi.create(Rule.CreateRule.createBuilder(RuleDirection.INGRESS, group.getProviderId())
                    .protocol(RuleProtocol.fromValue(ipPermission.getIpProtocol().name()))
                    .ethertype(RuleEthertype.IPV4)
                    .portRangeMin(ipPermission.getFromPort())
                    .portRangeMax(ipPermission.getToPort())
                    .remoteIpPrefix(cidr)
                    .build());
         }
      }

      if (!ipPermission.getGroupIds().isEmpty()) {
         for (String regionAndGroupRaw : ipPermission.getGroupIds()) {
            RegionAndId regionAndId = RegionAndId.fromSlashEncoded(regionAndGroupRaw);
            String groupId = regionAndId.getId();
            securityGroupApi.create(Rule.CreateRule.createBuilder(RuleDirection.INGRESS, groupId)
                    .protocol(RuleProtocol.fromValue(ipPermission.getIpProtocol().name()))
                    .ethertype(RuleEthertype.IPV4)
                    .portRangeMin(ipPermission.getFromPort())
                    .portRangeMax(ipPermission.getToPort())
                    .remoteGroupId(groupId)
                    .build());
         }
      }

      return getSecurityGroupById(RegionAndId.fromRegionAndId(region, id).slashEncode());
   }

   @Override
   public SecurityGroup addIpPermission(IpProtocol protocol, int startPort, int endPort,
                                        Multimap<String, String> tenantIdGroupNamePairs,
                                        Iterable<String> ipRanges,
                                        Iterable<String> groupIds, SecurityGroup group) {
      IpPermission.Builder permBuilder = IpPermission.builder();
      permBuilder.ipProtocol(protocol);
      permBuilder.fromPort(startPort);
      permBuilder.toPort(endPort);
      permBuilder.tenantIdGroupNamePairs(tenantIdGroupNamePairs);
      permBuilder.cidrBlocks(ipRanges);
      permBuilder.groupIds(groupIds);

      return addIpPermission(permBuilder.build(), group);
   }

   @Override
   public SecurityGroup removeIpPermission(final IpPermission ipPermission, SecurityGroup group) {
      String region = group.getLocation().getId();
      RegionAndId groupRegionAndId = RegionAndId.fromSlashEncoded(group.getId());
      String id = groupRegionAndId.getId();

      SecurityGroupApi securityGroupApi = getSecurityGroupApi(region);

      org.jclouds.openstack.neutron.v2.domain.SecurityGroup securityGroup = securityGroupApi.getSecurityGroup(id);

      if (!ipPermission.getCidrBlocks().isEmpty()) {
         for (final String cidr : ipPermission.getCidrBlocks()) {
            for (Rule rule : filter(securityGroup.getRules(),
                    new Predicate<Rule>() {
                       @Override
                       public boolean apply(@Nullable Rule input) {
                          return input.getRemoteIpPrefix() != null && input.getRemoteIpPrefix().equals(cidr) &&
                                 input.getProtocol() != null && input.getProtocol().name().equals(ipPermission.getIpProtocol().name()) &&
                                 input.getPortRangeMin() != null && input.getPortRangeMin() == ipPermission.getFromPort() &&
                                 input.getPortRangeMax() != null && input.getPortRangeMax() == ipPermission.getToPort();
                       }
                    })) {
               securityGroupApi.deleteRule(rule.getId());
            }
         }
      }

      if (!ipPermission.getGroupIds().isEmpty()) {
         for (final String groupId : ipPermission.getGroupIds()) {
            for (Rule rule : filter(securityGroup.getRules(),
                    new Predicate<Rule>() {
                       @Override
                       public boolean apply(@Nullable Rule input) {
                          return input.getRemoteGroupId() != null && input.getRemoteGroupId().equals(groupId) &&
                                 input.getProtocol() != null && input.getProtocol().name().equals(ipPermission.getIpProtocol().name()) &&
                                 input.getPortRangeMin() != null && input.getPortRangeMin() == ipPermission.getFromPort() &&
                                 input.getPortRangeMax() != null && input.getPortRangeMax() == ipPermission.getToPort();
                       }
                    })) {
               securityGroupApi.deleteRule(rule.getId());
            }
         }
      }

      return getSecurityGroupById(RegionAndId.fromRegionAndId(region, id).slashEncode());
   }

   @Override
   public SecurityGroup removeIpPermission(IpProtocol protocol, int startPort, int endPort,
                                           Multimap<String, String> tenantIdGroupNamePairs,
                                           Iterable<String> ipRanges,
                                           Iterable<String> groupIds, SecurityGroup group) {
      IpPermission.Builder permBuilder = IpPermission.builder();
      permBuilder.ipProtocol(protocol);
      permBuilder.fromPort(startPort);
      permBuilder.toPort(endPort);
      permBuilder.tenantIdGroupNamePairs(tenantIdGroupNamePairs);
      permBuilder.cidrBlocks(ipRanges);
      permBuilder.groupIds(groupIds);

      return removeIpPermission(permBuilder.build(), group);
   }

   @Override
   public boolean supportsTenantIdGroupNamePairs() {
      return false;
   }

   @Override
   public boolean supportsTenantIdGroupIdPairs() {
      return false;
   }

   @Override
   public boolean supportsGroupIds() {
      return true;
   }

   @Override
   public boolean supportsPortRangesForGroups() {
      return false;
   }

   @Override
   public boolean supportsExclusionCidrBlocks() {
      return false;
   }

   protected Iterable<? extends SecurityGroupInRegion> pollSecurityGroups() {
      Iterable<? extends Set<? extends SecurityGroupInRegion>> groups
              = transform(regionIds.get(), allSecurityGroupsInRegion());

      return concat(groups);
   }


   protected Iterable<? extends SecurityGroupInRegion> pollSecurityGroupsByRegion(String region) {
      return allSecurityGroupsInRegion().apply(region);
   }

   private Function<org.jclouds.openstack.neutron.v2.domain.SecurityGroup, SecurityGroup> toSecurityGroup(final Location location) {

      return new Function<org.jclouds.openstack.neutron.v2.domain.SecurityGroup, SecurityGroup>() {

         @Override
         public SecurityGroup apply(@Nullable org.jclouds.openstack.neutron.v2.domain.SecurityGroup group) {
            SecurityGroupBuilder builder = new SecurityGroupBuilder();
            builder.providerId(group.getId());
            builder.ownerId(group.getTenantId());
            builder.name(group.getName());
            final String regionId = location.getId();
            builder.location(location);

            builder.id(regionId + "/" + group.getId());
            if (group.getRules() != null) {
               builder.ipPermissions(filter(transform(group.getRules(), new Function<Rule, IpPermission>() {
                  @Override
                  public IpPermission apply(Rule from) {
                     if (from.getDirection() == RuleDirection.EGRESS) return null;
                     IpPermission.Builder builder = IpPermission.builder();
                     if (from.getProtocol() != null) {
                        builder.ipProtocol(IpProtocol.fromValue(from.getProtocol().name()));
                     } else {
                        builder.ipProtocol(IpProtocol.TCP);
                     }
                     if (from.getPortRangeMin() != null) builder.fromPort(from.getPortRangeMin());
                     if (from.getPortRangeMax() != null) builder.toPort(from.getPortRangeMax());
                     if (from.getRemoteGroupId() != null) {
                        builder.groupId(regionId + "/" + from.getRemoteGroupId());
                     } else if (from.getRemoteIpPrefix() != null){
                        builder.cidrBlock(from.getRemoteIpPrefix());
                     }

                     return builder.build();
                  }
               }), Predicates.notNull()));
            }

            return builder.build();
         }
      };
   }

   private SecurityGroupApi getSecurityGroupApi(String region) {
      return ((ApiContext<NeutronApi>) neutronApiContextSupplier.get()).getApi().getSecurityGroupApi(region);
   }

}
