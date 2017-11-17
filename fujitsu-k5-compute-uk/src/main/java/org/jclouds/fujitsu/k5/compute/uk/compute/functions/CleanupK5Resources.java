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

import static org.jclouds.openstack.nova.v2_0.compute.strategy.ApplyNovaTemplateOptionsCreateNodesWithGroupEncodedIntoNameThenAddToSet.JCLOUDS_SG_PREFIX;

import java.util.Set;

import org.jclouds.Context;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.features.SecurityGroupApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.compute.functions.CleanupResources;
import org.jclouds.openstack.nova.v2_0.compute.functions.RemoveFloatingIpFromNodeAndDeallocate;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.RegionAndId;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.RegionAndName;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.SecurityGroupInRegion;
import org.jclouds.rest.ApiContext;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;

public class CleanupK5Resources extends CleanupResources {

    private final Supplier<Context> neutronApiContextSupplier;

    @Inject
    public CleanupK5Resources(NovaApi novaApi, RemoveFloatingIpFromNodeAndDeallocate removeFloatingIpFromNodeAndDeallocate,
                              LoadingCache<RegionAndName, SecurityGroupInRegion> securityGroupMap,
                              Supplier<Context> neutronApiContextSupplier) {
//                              @Named("fujitsu-k5-networking-uk") Supplier<Context> neutronApiContextSupplier) {
        super(novaApi, removeFloatingIpFromNodeAndDeallocate, securityGroupMap);
        this.neutronApiContextSupplier = neutronApiContextSupplier;

    }

    @Override
    public Boolean apply(NodeMetadata node) {
        final RegionAndId regionAndId = RegionAndId.fromSlashEncoded(node.getId());
//        removeFloatingIpFromNodeifAny(regionAndId);
        return removeNeutronSecurityGroupCreatedByJcloudsAndInvalidateCache(regionAndId, node.getTags());
    }

    public boolean removeNeutronSecurityGroupCreatedByJcloudsAndInvalidateCache(RegionAndId regionAndId, Set<String> tags) {
        String region = regionAndId.getRegion();
        String securityGroupIdCreatedByJclouds = getSecurityGroupIdCreatedByJclouds(tags);

        SecurityGroupApi securityGroupApi = getSecurityGroupApi(region);

        if (securityGroupIdCreatedByJclouds != null) {
            String groupId = RegionAndId.fromSlashEncoded(securityGroupIdCreatedByJclouds).getId();
            org.jclouds.openstack.neutron.v2.domain.SecurityGroup securityGroup = securityGroupApi.getSecurityGroup(groupId);
            RegionAndName regionAndName = RegionAndName.fromRegionAndName(region, securityGroup.getName());
            logger.debug(">> deleting securityGroup(%s)", regionAndName);
            securityGroupApi.deleteSecurityGroup(groupId);
            securityGroupMap.invalidate(regionAndName);
            logger.debug("<< deleted securityGroup(%s)", regionAndName);
            return true;
        }
        return false;
    }

    private String getSecurityGroupIdCreatedByJclouds(Set<String> tags) {
        return FluentIterable.from(tags).filter(new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.startsWith(JCLOUDS_SG_PREFIX);
            }
        }).transform(new Function<String, String>() {
            @Override
            public String apply(String input) {
                return input.substring(JCLOUDS_SG_PREFIX.length() + 1);
            }
        }).first().orNull();
    }

    private void removeFloatingIpFromNodeifAny(RegionAndId regionAndId) {
        try {
            removeFloatingIpFromNodeAndDeallocate.apply(regionAndId);
        } catch (RuntimeException e) {
            logger.warn(e, "<< error removing and deallocating ip from node(%s): %s", regionAndId, e.getMessage());
        }
    }

    private SecurityGroupApi getSecurityGroupApi(String region) {
        return ((ApiContext<NeutronApi>) neutronApiContextSupplier.get()).getApi().getSecurityGroupApi(region);
    }

}
