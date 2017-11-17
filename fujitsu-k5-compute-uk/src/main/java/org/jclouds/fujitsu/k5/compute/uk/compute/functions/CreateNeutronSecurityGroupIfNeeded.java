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
package org.jclouds.fujitsu.k5.compute.uk.compute.functions;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;

import org.jclouds.Context;
import org.jclouds.collect.PagedIterable;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.net.domain.IpProtocol;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Rule;
import org.jclouds.openstack.neutron.v2.domain.RuleDirection;
import org.jclouds.openstack.neutron.v2.domain.RuleProtocol;
import org.jclouds.openstack.neutron.v2.domain.SecurityGroup;
import org.jclouds.openstack.neutron.v2.domain.SecurityGroup.CreateSecurityGroup;
import org.jclouds.openstack.neutron.v2.features.SecurityGroupApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.compute.functions.CreateSecurityGroupIfNeeded;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroupRule;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.RegionSecurityGroupNameAndPorts;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.SecurityGroupInRegion;
import org.jclouds.rest.ApiContext;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

public class CreateNeutronSecurityGroupIfNeeded extends CreateSecurityGroupIfNeeded {

   private final Supplier<Context> neutronApiContextSupplier;

   @Inject
   public CreateNeutronSecurityGroupIfNeeded(NovaApi novaApi, Supplier<Context> neutronApiContextSupplier) {
      // @Named("fujitsu-k5-networking-uk") Supplier<Context>
      // neutronApiContextSupplier) {
      super(novaApi);
      this.neutronApiContextSupplier = neutronApiContextSupplier;
   }

   @Override
   public SecurityGroupInRegion apply(RegionSecurityGroupNameAndPorts regionSecurityGroupNameAndPorts) {
      checkNotNull(regionSecurityGroupNameAndPorts, "regionSecurityGroupNameAndPorts");

      String regionId = regionSecurityGroupNameAndPorts.getRegion();
      SecurityGroupApi api = getSecurityGroupApi(regionId);
      final PagedIterable<SecurityGroup> allGroups = api.listSecurityGroups();
      logger.debug(">> creating securityGroup %s", regionSecurityGroupNameAndPorts);
      try {
         SecurityGroup securityGroup = api
               .create(CreateSecurityGroup.createBuilder().name(regionSecurityGroupNameAndPorts.getName())
                     .description("security group created by jclouds").build());

         logger.debug("<< created securityGroup(%s)", securityGroup);
         for (int port : regionSecurityGroupNameAndPorts.getPorts()) {
            authorizeGroupToItselfAndAllIPsToTCPPort(api, securityGroup, port);
         }
         return new SecurityGroupInRegion(
               toNovaSecurityGroup(regionId).apply(api.getSecurityGroup(securityGroup.getId())), regionId,
               Iterables.transform(allGroups.concat(), toNovaSecurityGroup(regionId)));
      } catch (IllegalStateException e) {
         logger.trace("<< trying to find securityGroup(%s): %s", regionSecurityGroupNameAndPorts, e.getMessage());
         // TODO
         // SecurityGroup group = find(allGroups,
         // nameEquals(regionSecurityGroupNameAndPorts.getName()));
         // logger.debug("<< reused securityGroup(%s)", group.getId());
         // return new SecurityGroupInRegion(group, regionId, allGroups);
         return null;
      }
   }

   private SecurityGroupApi getSecurityGroupApi(String region) {
      return ((ApiContext<NeutronApi>) neutronApiContextSupplier.get()).getApi().getSecurityGroupApi(region);
   }

   private void authorizeGroupToItselfAndAllIPsToTCPPort(SecurityGroupApi api, SecurityGroup securityGroup, int port) {
      logger.debug(">> authorizing securityGroup(%s) permission to 0.0.0.0/0 on port %d", securityGroup, port);
      api.create(Rule.CreateRule.createBuilder(RuleDirection.INGRESS, securityGroup.getId()).protocol(RuleProtocol.TCP)
            .portRangeMin(port).portRangeMax(port).remoteIpPrefix("0.0.0.0/0").build());
      logger.debug("<< authorized securityGroup(%s) permission to 0.0.0.0/0 on port %d", securityGroup, port);
   }

   private Function<SecurityGroup, org.jclouds.openstack.nova.v2_0.domain.SecurityGroup> toNovaSecurityGroup(
         final String regionId) {

      return new Function<SecurityGroup, org.jclouds.openstack.nova.v2_0.domain.SecurityGroup>() {

         @Nullable
         @Override
         public org.jclouds.openstack.nova.v2_0.domain.SecurityGroup apply(@Nullable final SecurityGroup group) {
            org.jclouds.openstack.nova.v2_0.domain.SecurityGroup.Builder builder = org.jclouds.openstack.nova.v2_0.domain.SecurityGroup
                  .builder();

            builder.name(group.getName());
            builder.tenantId(group.getTenantId());
            if (group.getRules() != null) {
               builder.rules(FluentIterable.from(group.getRules()).filter(new Predicate<Rule>() {
                  @Override
                  public boolean apply(@Nullable Rule input) {
                     return input.getPortRangeMin() != null;
                  }
               }).transform(new Function<Rule, SecurityGroupRule>() {
                  @Override
                  public SecurityGroupRule apply(Rule input) {
                     return securityGroupIpPermissionToRule(input, group.getId());
                  }
               }).toSet());
            }

            builder.id(regionId + "/" + group.getId());

            return builder.build();
         }
      };
   }

   private SecurityGroupRule securityGroupIpPermissionToRule(Rule rule, String groupId) {
      SecurityGroupRule.Builder builder = SecurityGroupRule.builder();
      builder.id(rule.getId());
      builder.ipProtocol(IpProtocol.TCP);
      builder.parentGroupId(groupId);
      builder.fromPort(rule.getPortRangeMin());
      builder.toPort(rule.getPortRangeMax());
      return builder.build();
   }

}
