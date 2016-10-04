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
package  org.jclouds.azurecompute.arm.functions;

import autovalue.shaded.com.google.common.common.collect.Lists;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.azurecompute.arm.AzureComputeApi;
import org.jclouds.azurecompute.arm.domain.IdReference;
import org.jclouds.azurecompute.arm.domain.ResourceGroup;
import org.jclouds.azurecompute.arm.domain.Subnet;
import org.jclouds.azurecompute.arm.domain.VirtualMachine;
import org.jclouds.azurecompute.arm.domain.VirtualNetwork;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.azurecompute.arm.config.AzureComputeProperties.TIMEOUT_RESOURCE_DELETED;

@Singleton
public class CleanupResources implements Function<String, Boolean> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   protected final AzureComputeApi api;
   private Predicate<URI> resourceDeleted;

   @Inject
   public CleanupResources(AzureComputeApi azureComputeApi, @Named(TIMEOUT_RESOURCE_DELETED) Predicate<URI> resourceDeleted) {
      this.api = azureComputeApi;
      this.resourceDeleted = resourceDeleted;
   }

   @Override
   public Boolean apply(final String id) {
      logger.debug("Destroying %s ...", id);

      Map<String, VirtualMachine> resourceGroupNamesAndVirtualMachines = getResourceGroupNamesAndVirtualMachines(id);
      if (resourceGroupNamesAndVirtualMachines.isEmpty()) return true;
      String group = checkNotNull(resourceGroupNamesAndVirtualMachines.entrySet().iterator().next().getKey(), "resourceGroup name must not be null");
      VirtualMachine virtualMachine = checkNotNull(resourceGroupNamesAndVirtualMachines.get(group), "virtualMachine must not be null");
      boolean vmDeleted = deleteVirtualMachine(group, virtualMachine);
      // delete networkCardInterfaces
      List<String> nics = getNetworkCardInterfaceNames(virtualMachine);
      for (String nicName : nics) {
         URI nicDeletionURI = api.getNetworkInterfaceCardApi(group).delete(nicName);
         // todo is a collection!
         boolean nicDeleted = resourceDeleted.apply(nicDeletionURI);
      }
      // delete virtual networks
      for (VirtualNetwork virtualNetwork : api.getVirtualNetworkApi(group).list()) {
         for (Subnet subnet : virtualNetwork.properties().subnets()) {
            // delete subnets
            api.getSubnetApi(group, virtualNetwork.name()).delete(subnet.name());
         }
         // todo is a collection!
         boolean virtualNetworkDeleted = api.getVirtualNetworkApi(group).delete(virtualNetwork.name());
      }
      // delete storage account
      String storageAccountNameURI = virtualMachine.properties().storageProfile().osDisk().vhd().uri();
      boolean storageAccountDeleted = api.getStorageAccountApi(group).delete(Iterables.get(Splitter.on(".").split(URI.create(storageAccountNameURI).getHost()), 0));

      // delete resource group if empty
      if (api.getVirtualMachineApi(group).list().isEmpty() &&
              api.getVirtualNetworkApi(group).list().isEmpty() &&
              api.getStorageAccountApi(group).list().isEmpty() &&
              api.getNetworkInterfaceCardApi(group).list().isEmpty()) {
         boolean resourceGroupDeleted = resourceDeleted.apply(api.getResourceGroupApi().delete(group));
      }
      return vmDeleted;
   }

   private List<String> getNetworkCardInterfaceNames(VirtualMachine virtualMachine) {
      List<String> nics = Lists.newArrayList();
      for (IdReference idReference : virtualMachine.properties().networkProfile().networkInterfaces()) {
         nics.add(Iterables.getLast(Splitter.on("/").split(idReference.id())));
      }
      return nics;
   }

   private boolean deleteVirtualMachine(String group, VirtualMachine virtualMachine) {
      return resourceDeleted.apply(api.getVirtualMachineApi(group).delete(virtualMachine.name()));
   }

   private Map<String, VirtualMachine> getResourceGroupNamesAndVirtualMachines(String id) {
      for (ResourceGroup resourceGroup : api.getResourceGroupApi().list()) {
         String group = resourceGroup.name();
         VirtualMachine virtualMachine = api.getVirtualMachineApi(group).get(id);
         if (virtualMachine != null) {
            return ImmutableMap.of(group, virtualMachine);
         }
      }
      return Maps.newHashMap();
   }
}
      
/*      
      for (ResourceGroup resourceGroup : api.getResourceGroupApi().list()) {
         String group = resourceGroup.name();
         VirtualMachine virtualMachine = api.getVirtualMachineApi(group).get(id);
         if (virtualMachine != null) {
            vmDeleted = resourceDeleted.apply(api.getVirtualMachineApi(group).delete(id));
            for (IdReference idReference : virtualMachine.properties().networkProfile().networkInterfaces()) {
               String nicName = Iterables.getLast(Splitter.on("/").split(idReference.id()));
               NetworkInterfaceCard networkInterfaceCard = api.getNetworkInterfaceCardApi(group).get(nicName);
               URI nicDeletionURI = api.getNetworkInterfaceCardApi(group).delete(nicName);
               nicDeleted = resourceDeleted.apply(nicDeletionURI);
               for (IpConfiguration ipConfiguration : networkInterfaceCard.properties().ipConfigurations()) {
                  if (ipConfiguration.properties().publicIPAddress() != null) {
                     String publicIpId = ipConfiguration.properties().publicIPAddress().id();
                     String publicIpAddressName = Iterables.getLast(Splitter.on("/").split(publicIpId));
                     publicIpAddressDeleted = api.getPublicIPAddressApi(group).delete(publicIpAddressName);
                  }
               }
            }
            for (VirtualNetwork virtualNetwork : api.getVirtualNetworkApi(group).list()) {
               for (Subnet subnet : virtualNetwork.properties().subnets()) {
                  api.getSubnetApi(group, virtualNetwork.name()).delete(subnet.name());
               }
               virtualNetworkDeleted = api.getVirtualNetworkApi(group).delete(virtualNetwork.name());
            }
            String storageAccountNameURI = virtualMachine.properties().storageProfile().osDisk().vhd().uri();
            storageAccountDeleted = api.getStorageAccountApi(group).delete(Iterables.get(Splitter.on(".").split(URI.create(storageAccountNameURI).getHost()), 0));
         }
         if (api.getVirtualMachineApi(group).list().isEmpty() &&
                 api.getVirtualNetworkApi(group).list().isEmpty() &&
                 api.getStorageAccountApi(group).list().isEmpty() &&
                 api.getNetworkInterfaceCardApi(group).list().isEmpty()) {
            resourceGroupDeleted = resourceDeleted.apply(api.getResourceGroupApi().delete(group));
         }
      }
      return vmDeleted && nicDeleted && publicIpAddressDeleted && virtualNetworkDeleted && storageAccountDeleted && resourceGroupDeleted;
   }

   private List<String> getNetworkCardInterfaceNames(VirtualMachine virtualMachine) {
      List<String> nics = Lists.newArrayList();
      for (IdReference idReference : virtualMachine.properties().networkProfile().networkInterfaces()) {
         nics.add(Iterables.getLast(Splitter.on("/").split(idReference.id())));
      }
      return nics;
   }

   private boolean deleteVirtualMachine(String group, VirtualMachine virtualMachine) {
      return resourceDeleted.apply(api.getVirtualMachineApi(group).delete(virtualMachine.name()));
   }

   private Map<String, VirtualMachine> getResourceGroupNamesAndVirtualMachines(String id) {
      for (ResourceGroup resourceGroup : api.getResourceGroupApi().list()) {
         String group = resourceGroup.name();
         VirtualMachine virtualMachine = api.getVirtualMachineApi(group).get(id);
         if (virtualMachine != null) {
            return ImmutableMap.of(group, virtualMachine);
         }
      }
      return Maps.newHashMap();
   }
}
*/
