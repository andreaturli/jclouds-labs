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
package org.jclouds.azurecompute.arm.compute.functions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;

import org.jclouds.azurecompute.arm.AzureComputeApi;
import org.jclouds.azurecompute.arm.compute.config.AzureComputeServiceContextModule.AzureComputeConstants;
import org.jclouds.azurecompute.arm.domain.IdReference;
import org.jclouds.azurecompute.arm.domain.ImageReference;
import org.jclouds.azurecompute.arm.domain.IpConfiguration;
import org.jclouds.azurecompute.arm.domain.NetworkInterfaceCard;
import org.jclouds.azurecompute.arm.domain.VirtualMachine;
import org.jclouds.azurecompute.arm.domain.VirtualMachineInstance;
import org.jclouds.azurecompute.arm.domain.VirtualMachineProperties;
import org.jclouds.azurecompute.arm.domain.VirtualMachineInstance.VirtualMachineStatus;
import org.jclouds.azurecompute.arm.domain.VirtualMachineInstance.VirtualMachineStatus.PowerState;
import org.jclouds.azurecompute.arm.domain.VirtualMachineProperties.ProvisioningState;
import org.jclouds.collect.Memoized;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.functions.GroupNamingConvention;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.Location;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.logging.Logger;

import autovalue.shaded.com.google.common.common.base.Joiner;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.transform;

public class VirtualMachineToNodeMetadata  implements Function<VirtualMachine, NodeMetadata> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;
   
   // When using the Deployment API to deploy an ARM template, the deployment goes through
   // stages.  Accepted -> Running -> Succeeded.  Only when the deployment has SUCCEEDED is
   // the resource deployed using the template actually ready.
   //
   // To get details about the resource(s) deployed via template, one needs to query the
   // various resources after the deployment has "SUCCEEDED".
   private static final Function<VirtualMachineProperties.ProvisioningState, NodeMetadata.Status> PROVISIONINGSTATE_TO_NODESTATUS = Functions
         .forMap(
               ImmutableMap.<VirtualMachineProperties.ProvisioningState, NodeMetadata.Status> builder()
                     .put(VirtualMachineProperties.ProvisioningState.ACCEPTED, NodeMetadata.Status.PENDING)
                     .put(VirtualMachineProperties.ProvisioningState.READY, NodeMetadata.Status.PENDING)
                     .put(VirtualMachineProperties.ProvisioningState.CREATING, NodeMetadata.Status.PENDING)
                     .put(VirtualMachineProperties.ProvisioningState.RUNNING, NodeMetadata.Status.PENDING)
                     .put(VirtualMachineProperties.ProvisioningState.UPDATING, NodeMetadata.Status.PENDING)
                     .put(VirtualMachineProperties.ProvisioningState.SUCCEEDED, NodeMetadata.Status.RUNNING)
                     .put(VirtualMachineProperties.ProvisioningState.DELETED, NodeMetadata.Status.TERMINATED)
                     .put(VirtualMachineProperties.ProvisioningState.CANCELED, NodeMetadata.Status.TERMINATED)
                     .put(VirtualMachineProperties.ProvisioningState.FAILED, NodeMetadata.Status.ERROR)
                     .put(VirtualMachineProperties.ProvisioningState.UNRECOGNIZED, NodeMetadata.Status.UNRECOGNIZED)
                     .build(), NodeMetadata.Status.UNRECOGNIZED);
   
   private static final Function<VirtualMachineStatus.PowerState, NodeMetadata.Status> POWERSTATE_TO_NODESTATUS = Functions
         .forMap(
               ImmutableMap.<PowerState, NodeMetadata.Status> builder()
                     .put(PowerState.RUNNING, NodeMetadata.Status.RUNNING)
                     .put(PowerState.STOPPED, NodeMetadata.Status.SUSPENDED)
                     .put(PowerState.UNRECOGNIZED, NodeMetadata.Status.UNRECOGNIZED).build(),
               NodeMetadata.Status.UNRECOGNIZED);
   
   private final String azureGroup;
   private final AzureComputeApi api;
   private final GroupNamingConvention nodeNamingConvention;
   private final Supplier<Map<String, ? extends Image>> images;
   private final Supplier<Set<? extends Location>> locations;
   private final Supplier<Map<String, ? extends Hardware>> hardwares;
   private final Map<String, Credentials> credentialStore;

   @Inject
   VirtualMachineToNodeMetadata(AzureComputeApi api, GroupNamingConvention.Factory namingConvention,
         Supplier<Map<String, ? extends Image>> images, Supplier<Map<String, ? extends Hardware>> hardwares,
         @Memoized Supplier<Set<? extends Location>> locations, Map<String, Credentials> credentialStore,
         final AzureComputeConstants azureComputeConstants) {
      this.api = api;
      this.nodeNamingConvention = namingConvention.createWithoutPrefix();
      this.images = checkNotNull(images, "images cannot be null");
      this.locations = checkNotNull(locations, "locations cannot be null");
      this.hardwares = checkNotNull(hardwares, "hardwares cannot be null");
      this.credentialStore = credentialStore;
      this.azureGroup = azureComputeConstants.azureResourceGroup();
   }
   @Override
   public NodeMetadata apply(VirtualMachine virtualMachine) {
      NodeMetadataBuilder builder = new NodeMetadataBuilder();
      builder.id(virtualMachine.name());
      builder.providerId(virtualMachine.id());
      builder.name(virtualMachine.name());
      builder.hostname(virtualMachine.name());
      String group = this.nodeNamingConvention.extractGroup(virtualMachine.name());
      builder.group(group);
      
      ProvisioningState provisioningState = virtualMachine.properties().provisioningState();
      if (ProvisioningState.SUCCEEDED.equals(provisioningState)) {
         // If the provisioning succeeded, we need to query the *real* status of the VM
         VirtualMachineInstance instanceDetails = api.getVirtualMachineApi(azureGroup).getInstanceDetails(virtualMachine.name());
         builder.status(POWERSTATE_TO_NODESTATUS.apply(instanceDetails.powerState()));
         builder.backendStatus(Joiner.on(',').join(transform(instanceDetails.statuses(), new Function<VirtualMachineStatus, String>() {
            @Override public String apply(VirtualMachineStatus input) {
               return input.code();
            }
         })));
      } else {
         builder.status(PROVISIONINGSTATE_TO_NODESTATUS.apply(provisioningState));
         builder.backendStatus(provisioningState.name());   
      }

      Credentials credentials = credentialStore.get("node#" + virtualMachine.name());
      builder.credentials(LoginCredentials.fromCredentials(credentials));

      builder.publicAddresses(getPublicIpAddresses(virtualMachine.properties().networkProfile().networkInterfaces()));
      builder.privateAddresses(getPrivateIpAddresses(virtualMachine.properties().networkProfile().networkInterfaces()));

         if (virtualMachine.tags() != null) {
            Map<String, String> userMetaData = virtualMachine.tags();
            builder.userMetadata(userMetaData);
            builder.tags(Splitter.on(",").split(userMetaData.get("tags")));
         }
         String locationName = virtualMachine.location();
         builder.location(getLocation(locationName));

         ImageReference imageReference = virtualMachine.properties().storageProfile().imageReference();
         Optional<? extends Image> image = findImage(imageReference, locationName);
         if (image.isPresent()) {
            builder.imageId(image.get().getId());
            builder.operatingSystem(image.get().getOperatingSystem());
         } else {
            logger.info(">> image with id %s for virtualmachine %s was not found. "
                            + "This might be because the image that was used to create the virtualmachine has a new id.",
                    virtualMachine.id(), virtualMachine.id());
         }

         builder.hardware(getHardware(virtualMachine.properties().hardwareProfile().vmSize()));

      return builder.build();
   }

   private Iterable<String> getPrivateIpAddresses(List<IdReference> idReferences) {
      List<String> privateIpAddresses = Lists.newArrayList();
      for (IdReference networkInterfaceCardIdReference : idReferences) {
         NetworkInterfaceCard networkInterfaceCard = getNetworkInterfaceCard(networkInterfaceCardIdReference);
         for (IpConfiguration ipConfiguration : networkInterfaceCard.properties().ipConfigurations()) {
            privateIpAddresses.add(ipConfiguration.properties().privateIPAddress());
         }
      }
      return privateIpAddresses;
   }

   private NetworkInterfaceCard getNetworkInterfaceCard(IdReference networkInterfaceCardIdReference) {
      Iterables.get(Splitter.on("/").split(networkInterfaceCardIdReference.id()), 2);
      String resourceGroup = Iterables.get(Splitter.on("/").split(networkInterfaceCardIdReference.id()), 4);
      String nicName = Iterables.getLast(Splitter.on("/").split(networkInterfaceCardIdReference.id()));
      return api.getNetworkInterfaceCardApi(resourceGroup).get(nicName);
   }

   private Iterable<String> getPublicIpAddresses(List<IdReference> idReferences) {
      List<String> publicIpAddresses = Lists.newArrayList();
      for (IdReference networkInterfaceCardIdReference : idReferences) {
         Iterables.get(Splitter.on("/").split(networkInterfaceCardIdReference.id()), 2);
         String resourceGroup = Iterables.get(Splitter.on("/").split(networkInterfaceCardIdReference.id()), 4);
         String nicName = Iterables.getLast(Splitter.on("/").split(networkInterfaceCardIdReference.id()));
         NetworkInterfaceCard networkInterfaceCard = api.getNetworkInterfaceCardApi(resourceGroup).get(nicName);
         for (IpConfiguration ipConfiguration : networkInterfaceCard.properties().ipConfigurations()) {
            if (ipConfiguration.properties().publicIPAddress() != null) {
               String publicIpId = ipConfiguration.properties().publicIPAddress().id();
               publicIpAddresses.add(api.getPublicIPAddressApi(resourceGroup).get(Iterables.getLast(Splitter.on("/").split(publicIpId))).properties().ipAddress());
            }
         }
      }
      return publicIpAddresses;
   }

   protected Location getLocation(final String locationName) {
      return find(locations.get(), new Predicate<Location>() {
         @Override
         public boolean apply(Location location) {
            return locationName != null && locationName.equals(location.getId());
         }
      }, null);
   }

   protected Optional<? extends Image> findImage(ImageReference imageReference, String locatioName) {
      return Optional.fromNullable(images.get().get(VMImageToImage.encodeFieldsToUniqueId(false, locatioName, imageReference)));
   }

   protected Hardware getHardware(final String vmSize) {
      return Iterables.find(hardwares.get().values(), new Predicate<Hardware>() {
         @Override
         public boolean apply(Hardware input) {
            return input.getId().equals(vmSize);
         }
      });
   }

}
