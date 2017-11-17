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
package org.jclouds.fujitsu.k5.compute.uk.strategy;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.jclouds.Constants;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.config.CustomizationResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.extensions.SecurityGroupExtension;
import org.jclouds.compute.functions.GroupNamingConvention;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.compute.strategy.CreateNodeWithGroupEncodedIntoName;
import org.jclouds.compute.strategy.CustomizeNodeAndAddToGoodMapOrPutExceptionIntoBadMap;
import org.jclouds.compute.strategy.ListNodesStrategy;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.logging.Logger;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.compute.functions.AllocateAndAddFloatingIpToNode;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.compute.strategy.ApplyNovaTemplateOptionsCreateNodesWithGroupEncodedIntoNameThenAddToSet;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.RegionAndName;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.RegionSecurityGroupNameAndPorts;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.SecurityGroupInRegion;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newLinkedHashMap;

@Singleton
public class K5CreateNodesWithGroupEncodedIntoNameThenAddToSet extends
        ApplyNovaTemplateOptionsCreateNodesWithGroupEncodedIntoNameThenAddToSet {

    @javax.annotation.Resource
    @Named(ComputeServiceConstants.COMPUTE_LOGGER)
    protected Logger logger = Logger.NULL;

    private SecurityGroupExtension securityGroupExtension;
    private ComputeServiceContext computeServiceContext;

    @Inject
    protected K5CreateNodesWithGroupEncodedIntoNameThenAddToSet(CreateNodeWithGroupEncodedIntoName addNodeWithTagStrategy,
                                                                ListNodesStrategy listNodesStrategy, GroupNamingConvention.Factory namingConvention,
                                                                CustomizeNodeAndAddToGoodMapOrPutExceptionIntoBadMap.Factory customizeNodeAndAddToGoodMapOrPutExceptionIntoBadMapFactory,
                                                                @Named(Constants.PROPERTY_USER_THREADS) ListeningExecutorService userExecutor,
                                                                AllocateAndAddFloatingIpToNode createAndAddFloatingIpToNode,
                                                                LoadingCache<RegionAndName, SecurityGroupInRegion> securityGroupCache, NovaApi novaApi,
                                                                ComputeServiceContext computeServiceContext) {
        super(addNodeWithTagStrategy, listNodesStrategy, namingConvention, customizeNodeAndAddToGoodMapOrPutExceptionIntoBadMapFactory, userExecutor,
              createAndAddFloatingIpToNode, securityGroupCache, novaApi);
        this.computeServiceContext = computeServiceContext;
    }

    @Override
    public Map<?, ListenableFuture<Void>> execute(String group, int count, Template template, Set<NodeMetadata> goodNodes,
                                                  Map<NodeMetadata, Exception> badNodes, Multimap<NodeMetadata, CustomizationResponse> customizationResponses) {

        NovaTemplateOptions templateOptions = NovaTemplateOptions.class.cast(template.getOptions());
        final String region = template.getLocation().getId();

        if (templateOptions.shouldAutoAssignFloatingIp()) {
            checkArgument(novaApi.getFloatingIPApi(region).isPresent(),
                    "Floating IPs are required by options, but the extension is not available! options: %s",
                    templateOptions);
        }
        if (templateOptions.shouldGenerateKeyPair() || templateOptions.getKeyPairName() != null) {
            checkArgument(novaApi.getKeyPairApi(region).isPresent(),
                    "Key Pairs are required by options, but the extension is not available! options: %s", templateOptions);
        }

        final List<Integer> inboundPorts = Ints.asList(templateOptions.getInboundPorts());
        if (!templateOptions.getGroups().isEmpty() || !inboundPorts.isEmpty()) {
            // TODO check Neutron or Nova
//         checkArgument(novaApi.getSecurityGroupApi(region).isPresent(),
//                 "Security groups are required by options, but the extension is not available! options: %s",
//                 templateOptions);
        }

        KeyPair keyPair = null;
        if (templateOptions.shouldGenerateKeyPair()) {
            keyPair = generateKeyPair(region, namingConvention.create().sharedNameForGroup(group));
            // If a private key has not been explicitly set, configure the auto-generated one
            if (Strings.isNullOrEmpty(templateOptions.getLoginPrivateKey())) {
                templateOptions.overrideLoginPrivateKey(keyPair.getPrivateKey());
            }
        } else if (templateOptions.getKeyPairName() != null) {
            keyPair = checkNotNull(novaApi.getKeyPairApi(region).get().get(templateOptions.getKeyPairName()),
                    "keypair %s doesn't exist", templateOptions.getKeyPairName());
        }
        if (keyPair != null) {
            templateOptions.keyPairName(keyPair.getName());
        }

        ImmutableList.Builder<String> tagsBuilder = ImmutableList.builder();

        if (computeServiceContext.getComputeService().getSecurityGroupExtension().isPresent()) {
            securityGroupExtension = computeServiceContext.getComputeService().getSecurityGroupExtension().get();
        }

        if (!templateOptions.getGroups().isEmpty()) {
            Iterable<String> securityGroupNames = Iterables.transform(securityGroupExtension.listSecurityGroups(), new Function<org.jclouds.compute.domain.SecurityGroup, String>() {
                @Override
                public String apply(@Nullable org.jclouds.compute.domain.SecurityGroup input) {
                    return input.getName();
                }
            });
            for (String securityGroupName : templateOptions.getGroups()) {
                checkState(Iterables.contains(securityGroupNames, securityGroupName), "Cannot find security group with name " + securityGroupName + ". \nSecurity groups available are: \n" + Iterables.toString(securityGroupNames)); // {
            }

        } else if (!inboundPorts.isEmpty()) {

            SecurityGroupInRegion securityGroupInRegion = null;
            String securityGroupName = namingConvention.create().sharedNameForGroup(group);
            try {
                securityGroupInRegion = securityGroupCache.get(new RegionSecurityGroupNameAndPorts(region, securityGroupName, inboundPorts));
            } catch (ExecutionException e) {
                throw Throwables.propagate(e.getCause());
            }
            templateOptions.securityGroups(securityGroupName);
            tagsBuilder.add(String.format("%s-%s", JCLOUDS_SG_PREFIX, securityGroupInRegion.getSecurityGroup().getId()));
        }
        templateOptions.tags(tagsBuilder.addAll(templateOptions.getTags()).build());

        Map<?, ListenableFuture<Void>> responses = doExecute(group, count, template, goodNodes, badNodes,
                customizationResponses);

        // Key pairs in Openstack are only required to create the Server. They aren't used anymore so it is better
        // to delete the auto-generated key pairs at this point where we know exactly which ones have been
        // auto-generated by jclouds.
        if (templateOptions.shouldGenerateKeyPair() && keyPair != null) {
            registerAutoGeneratedKeyPairCleanupCallbacks(responses, region, keyPair.getName());
        }
        return responses;
    }

    private Map<?, ListenableFuture<Void>> doExecute(String group, int count, Template template, Set<NodeMetadata> goodNodes,
                                                     Map<NodeMetadata, Exception> badNodes, Multimap<NodeMetadata, CustomizationResponse> customizationResponses) {
        Map<String, ListenableFuture<Void>> responses = newLinkedHashMap();
        for (String name : getNextNames(group, template, count)) {
            responses.put(name, Futures.transform(createNodeInGroupWithNameAndTemplate(group, name, template),
                    customizeNodeAndAddToGoodMapOrPutExceptionIntoBadMapFactory.create(template.getOptions(), goodNodes,
                            badNodes, customizationResponses), userExecutor));
        }
        return responses;
    }

    private KeyPair generateKeyPair(String region, String prefix) {
        logger.debug(">> creating default keypair for node...");
        KeyPair keyPair = novaApi.getKeyPairApi(region).get().create(namingConvention.createWithoutPrefix().uniqueNameForGroup(prefix));
        logger.debug(">> keypair created! %s", keyPair.getName());
        return keyPair;
    }

    private void registerAutoGeneratedKeyPairCleanupCallbacks(final Map<?, ListenableFuture<Void>> responses,
                                                              final String region, final String generatedKeyPairName) {
        // The Futures.allAsList fails immediately if some of the futures fail. The Futures.successfulAsList, however,
        // returns a list containing the results or 'null' for those futures that failed. We want to wait for all them
        // (even if they fail), so better use the latter form.
        ListenableFuture<List<Void>> aggregatedResponses = Futures.successfulAsList(responses.values());

        // Key pairs must be cleaned up after all futures completed (even if some failed).
        Futures.addCallback(aggregatedResponses, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(List<Void> result) {
                cleanupAutoGeneratedKeyPair(generatedKeyPairName);
            }

            @Override
            public void onFailure(Throwable t) {
                cleanupAutoGeneratedKeyPair(generatedKeyPairName);
            }

            private void cleanupAutoGeneratedKeyPair(String keyPairName) {
                logger.debug(">> cleaning up auto-generated key pairs...");
                try {
                    novaApi.getKeyPairApi(region).get().delete(keyPairName);
                } catch (Exception ex) {
                    logger.warn(">> could not delete key pair %s: %s", keyPairName, ex.getMessage());
                }
            }

        }, userExecutor);
    }

}
