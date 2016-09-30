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
import org.jclouds.azurecompute.arm.domain.Deployment;
import org.jclouds.azurecompute.arm.domain.ImageReference;
import org.jclouds.azurecompute.arm.domain.IpConfiguration;
import org.jclouds.azurecompute.arm.domain.NetworkInterfaceCard;
import org.jclouds.azurecompute.arm.domain.PublicIPAddress;
import org.jclouds.azurecompute.arm.domain.VMDeployment;
import org.jclouds.azurecompute.arm.domain.VirtualMachine;
import org.jclouds.azurecompute.arm.util.GetEnumValue;
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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.find;

public class DeploymentToNodeMetadata implements Function<VMDeployment, NodeMetadata> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;
   
   // When using the Deployment API to deploy an ARM template, the deployment goes through
   // stages.  Accepted -> Running -> Succeeded.  Only when the deployment has SUCCEEDED is
   // the resource deployed using the template actually ready.
   //
   // To get details about the resource(s) deployed via template, one needs to query the
   // various resources after the deployment has "SUCCEEDED".
   private static final Map<Deployment.ProvisioningState, NodeMetadata.Status> STATUS_TO_NODESTATUS =
           ImmutableMap.<Deployment.ProvisioningState, NodeMetadata.Status>builder().
                   put(Deployment.ProvisioningState.ACCEPTED, NodeMetadata.Status.PENDING).
                   put(Deployment.ProvisioningState.READY, NodeMetadata.Status.PENDING).
                   put(Deployment.ProvisioningState.RUNNING, NodeMetadata.Status.PENDING).
                   put(Deployment.ProvisioningState.CANCELED, NodeMetadata.Status.TERMINATED).
                   put(Deployment.ProvisioningState.FAILED, NodeMetadata.Status.ERROR).
                   put(Deployment.ProvisioningState.DELETED, NodeMetadata.Status.TERMINATED).
                   put(Deployment.ProvisioningState.SUCCEEDED, NodeMetadata.Status.RUNNING).
                   put(Deployment.ProvisioningState.UNRECOGNIZED, NodeMetadata.Status.UNRECOGNIZED).
                   build();

   public static Deployment.ProvisioningState provisioningStateFromString(final String text) {
      return (Deployment.ProvisioningState) GetEnumValue.fromValueOrDefault(text, Deployment.ProvisioningState.UNRECOGNIZED);
   }

   private final AzureComputeApi api;
   private final GroupNamingConvention nodeNamingConvention;
   private final Supplier<Map<String, ? extends Image>> images;
   private final Supplier<Set<? extends Location>> locations;
   private final Supplier<Map<String, ? extends Hardware>> hardwares;
   private final Map<String, Credentials> credentialStore;

   @Inject
   DeploymentToNodeMetadata(
           AzureComputeApi api,
           GroupNamingConvention.Factory namingConvention, 
           Supplier<Map<String, ? extends Image>> images,
           Supplier<Map<String, ? extends Hardware>> hardwares, 
           @Memoized Supplier<Set<? extends Location>> locations, Map<String, Credentials> credentialStore) {
      this.api = api;
      this.nodeNamingConvention = namingConvention.createWithoutPrefix();
      this.images = checkNotNull(images, "images cannot be null");
      this.locations = checkNotNull(locations, "locations cannot be null");
      this.hardwares = checkNotNull(hardwares, "hardwares cannot be null");
      this.credentialStore = credentialStore;
   }

   @Override
   public NodeMetadata apply(final VMDeployment from) {
      final NodeMetadataBuilder builder = new NodeMetadataBuilder();
      VirtualMachine virtualMachine = from.virtualMachine();
      builder.id(from.deploymentId()); 
      builder.providerId(virtualMachine.id());
      builder.name(virtualMachine.name());
      //builder.hostname(deployment.name() + "pc");
      String group = this.nodeNamingConvention.extractGroup(virtualMachine.name());
      builder.group(group);
      builder.status(getStatus(virtualMachine.properties().provisioningState()));

      Credentials credentials = credentialStore.get("node#" + virtualMachine.name());
      builder.credentials(LoginCredentials.fromCredentials(credentials));

      builder.publicAddresses(getPublicIpAddresses(from.ipAddressList()));
      builder.privateAddresses(getPrivateIpAddresses(from.networkInterfaceCards()));

      if (virtualMachine != null) {
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
      }

      return builder.build();
   }

   private Iterable<String> getPrivateIpAddresses(List<NetworkInterfaceCard> networkInterfaceCards) {
      return FluentIterable.from(networkInterfaceCards)
              .filter(new Predicate<NetworkInterfaceCard>() {
                 @Override
                 public boolean apply(NetworkInterfaceCard nic) {
                    return nic != null && nic.properties() != null && nic.properties().ipConfigurations() != null;
                 }
              }).transformAndConcat(new Function<NetworkInterfaceCard, Iterable<IpConfiguration>>() {
                 @Override
                 public Iterable<IpConfiguration> apply(NetworkInterfaceCard nic) {
                    return nic.properties().ipConfigurations();
                 }
              }).filter(new Predicate<IpConfiguration>() {
                 @Override
                 public boolean apply(IpConfiguration ip) {
                    return ip != null && ip.properties() != null && ip.properties().privateIPAddress() != null;
                 }
              }).transform(new Function<IpConfiguration, String>() {
                 @Override
                 public String apply(IpConfiguration ipConfiguration) {
                    return ipConfiguration.properties().privateIPAddress();
                 }
              }).toSet();
   }

   private Iterable<String> getPublicIpAddresses(List<PublicIPAddress> publicIPAddresses) {
      return FluentIterable.from(publicIPAddresses)
              .filter(new Predicate<PublicIPAddress>() {
                 @Override
                 public boolean apply(PublicIPAddress publicIPAddress) {
                    return publicIPAddress != null && publicIPAddress.properties() != null && publicIPAddress.properties().ipAddress() != null;
                 }
              }).transform(new Function<PublicIPAddress, String>() {
                 @Override
                 public String apply(PublicIPAddress publicIPAddress) {
                    return publicIPAddress.properties().ipAddress();
                 }
              }).toSet();
   }

   private NodeMetadata.Status getStatus(String provisioningState) {
      return STATUS_TO_NODESTATUS.get(provisioningStateFromString(provisioningState));
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
