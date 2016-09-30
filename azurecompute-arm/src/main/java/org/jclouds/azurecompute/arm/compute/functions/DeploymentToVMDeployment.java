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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.azurecompute.arm.AzureComputeApi;
import org.jclouds.azurecompute.arm.domain.Deployment;
import org.jclouds.azurecompute.arm.domain.Deployment.Dependency;
import org.jclouds.azurecompute.arm.domain.NetworkInterfaceCard;
import org.jclouds.azurecompute.arm.domain.PublicIPAddress;
import org.jclouds.azurecompute.arm.domain.VMDeployment;
import org.jclouds.azurecompute.arm.domain.VirtualMachine;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

/**
 * Converts an Deployment into a VMDeployment.
 */
@Singleton
public class DeploymentToVMDeployment implements Function<Deployment, VMDeployment> {

   private final AzureComputeApi api;

   @Inject
   DeploymentToVMDeployment(AzureComputeApi api) {
      this.api = api;
   }

   @Override
   public VMDeployment apply(final Deployment deployment) {
      if (deployment.properties() == null || deployment.properties().dependencies() == null) return null;
      List<Dependency> dependencies = deployment.properties().dependencies();
      String resourceGroup = getResourceGroupFromId(deployment.id());

      VirtualMachine virtualMachine = getVirtualMachine(dependencies, resourceGroup);
      
      List<NetworkInterfaceCard> networkInterfaceCards = getNetworkInterfaceCards(dependencies, resourceGroup);
      List<PublicIPAddress> ipAddressList = getPublicIPAddress(dependencies, resourceGroup);

      return VMDeployment.create(deployment.name(), virtualMachine, ipAddressList, networkInterfaceCards);
   }

   private VirtualMachine getVirtualMachine(List<Dependency> dependencies, String resourceGroup) {
      Dependency dependency = Iterables.find(dependencies, new DependencyPredicate("Microsoft.Compute/virtualMachines"));
      return api.getVirtualMachineApi(resourceGroup).get(dependency.resourceName());
   }

   private List<PublicIPAddress> getPublicIPAddress(List<Dependency> dependencies, final String resourceGroup) {
      List<PublicIPAddress> list = FluentIterable.from(dependencies)
              .filter(new DependencyPredicate("Microsoft.Network/networkInterfaces"))
              .transformAndConcat(new Function<Dependency, Iterable<Dependency>>() {
                 @Override
                 public Iterable<Dependency> apply(Dependency input) {
                    return input.dependsOn();
                 }
              }).filter(new DependencyPredicate("Microsoft.Network/publicIPAddresses"))
              .transform(new Function<Dependency, PublicIPAddress>() {
                 @Override
                 public PublicIPAddress apply(Dependency input) {
                    return api.getPublicIPAddressApi(resourceGroup).get(input.resourceName());
                 }
              }).toList();
      return list;
   }

   private String getResourceGroupFromId(String id) {
      String searchStr = "/resourceGroups/";
      int indexStart = id.lastIndexOf(searchStr) + searchStr.length();
      searchStr = "/providers/";
      int indexEnd = id.lastIndexOf(searchStr);

      String resourceGroup = id.substring(indexStart, indexEnd);
      return resourceGroup;
   }

   private List<NetworkInterfaceCard> getNetworkInterfaceCards(List<Dependency> dependencies, final String resourceGroup) {
      List<NetworkInterfaceCard> result = FluentIterable.from(dependencies)
              .filter(new DependencyPredicate("Microsoft.Network/networkInterfaces"))
      .transform(new Function<Dependency, NetworkInterfaceCard>() {
         @Override
         public NetworkInterfaceCard apply(Dependency input) {
            return api.getNetworkInterfaceCardApi(resourceGroup).get(input.resourceName());
         }
      }).toList();
      return result;
   }

   private static class DependencyPredicate implements Predicate<Dependency> {

      private final String resourceType;
      
      public DependencyPredicate(String resourceType) {
         this.resourceType = resourceType;
      }

      @Override
      public boolean apply(Dependency dependency) {
         return dependency.resourceType().equals(resourceType);
      }
   }
}
