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
package org.jclouds.azurecompute.arm.compute;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.azurecompute.arm.AzureComputeApi;
import org.jclouds.azurecompute.arm.compute.config.AzureComputeServiceContextModule.AzureComputeConstants;
import org.jclouds.azurecompute.arm.compute.functions.DeploymentToVMDeployment;
import org.jclouds.azurecompute.arm.compute.functions.VMImageToImage;
import org.jclouds.azurecompute.arm.compute.options.AzureTemplateOptions;
import org.jclouds.azurecompute.arm.compute.predicates.IsDeploymentInRegions;
import org.jclouds.azurecompute.arm.domain.DataDisk;
import org.jclouds.azurecompute.arm.domain.HardwareProfile;
import org.jclouds.azurecompute.arm.domain.IdReference;
import org.jclouds.azurecompute.arm.domain.ImageReference;
import org.jclouds.azurecompute.arm.domain.IpConfiguration;
import org.jclouds.azurecompute.arm.domain.IpConfigurationProperties;
import org.jclouds.azurecompute.arm.domain.Location;
import org.jclouds.azurecompute.arm.domain.NetworkInterfaceCard;
import org.jclouds.azurecompute.arm.domain.NetworkInterfaceCardProperties;
import org.jclouds.azurecompute.arm.domain.NetworkProfile;
import org.jclouds.azurecompute.arm.domain.OSDisk;
import org.jclouds.azurecompute.arm.domain.OSProfile;
import org.jclouds.azurecompute.arm.domain.Offer;
import org.jclouds.azurecompute.arm.domain.PublicIPAddress;
import org.jclouds.azurecompute.arm.domain.PublicIPAddressProperties;
import org.jclouds.azurecompute.arm.domain.ResourceProviderMetaData;
import org.jclouds.azurecompute.arm.domain.SKU;
import org.jclouds.azurecompute.arm.domain.StorageProfile;
import org.jclouds.azurecompute.arm.domain.StorageService;
import org.jclouds.azurecompute.arm.domain.VHD;
import org.jclouds.azurecompute.arm.domain.VMHardware;
import org.jclouds.azurecompute.arm.domain.VMImage;
import org.jclouds.azurecompute.arm.domain.VMSize;
import org.jclouds.azurecompute.arm.domain.Version;
import org.jclouds.azurecompute.arm.domain.VirtualMachine;
import org.jclouds.azurecompute.arm.domain.VirtualMachineProperties;
import org.jclouds.azurecompute.arm.features.OSImageApi;
import org.jclouds.azurecompute.arm.features.PublicIPAddressApi;
import org.jclouds.azurecompute.arm.functions.CleanupResources;
import org.jclouds.azurecompute.arm.functions.ParseJobStatus;
import org.jclouds.azurecompute.arm.util.BlobHelper;
import org.jclouds.azurecompute.arm.util.DeploymentTemplateBuilder;
import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.location.Region;
import org.jclouds.logging.Logger;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkState;
import static org.jclouds.azurecompute.arm.compute.extensions.AzureComputeImageExtension.CUSTOM_IMAGE_PREFIX;
import static org.jclouds.util.Predicates2.retry;

/**
 * Defines the connection between the {@link AzureComputeApi} implementation and the jclouds
 * {@link org.jclouds.compute.ComputeService}.
 */
@Singleton
public class AzureComputeServiceAdapter implements ComputeServiceAdapter<VirtualMachine, VMHardware, VMImage, Location> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   private Logger logger = Logger.NULL;

   private String azureGroup;
   private final CleanupResources cleanupResources;
   private final AzureComputeApi api;
   private final AzureComputeConstants azureComputeConstants;
   private final Supplier<Set<String>> regionIds;
   private final IsDeploymentInRegions isDeploymentInRegions;
   private final DeploymentToVMDeployment deploymentToVMDeployment;

   @Inject
   AzureComputeServiceAdapter(final AzureComputeApi api, final AzureComputeConstants azureComputeConstants,
         CleanupResources cleanupResources, @Region Supplier<Set<String>> regionIds,
         IsDeploymentInRegions isDeploymentInRegions, DeploymentToVMDeployment deploymentToVMDeployment) {
      this.api = api;
      this.azureComputeConstants = azureComputeConstants;
      this.azureGroup = azureComputeConstants.azureResourceGroup();

      logger.debug("AzureComputeServiceAdapter set azuregroup to: " + azureGroup);

      this.cleanupResources = cleanupResources;
      this.regionIds = regionIds;
      this.isDeploymentInRegions = isDeploymentInRegions;
      this.deploymentToVMDeployment = deploymentToVMDeployment;
   }

   @Override
   public NodeAndInitialCredentials<VirtualMachine> createNodeWithGroupEncodedIntoName(
           final String group, final String name, final Template template) {

      AzureTemplateOptions templateOptions = template.getOptions().as(AzureTemplateOptions.class);

      // TODO ARM specific options
      
      String adminUsername = Objects.firstNonNull(templateOptions.getLoginUser(), "jclouds");
      OSProfile.LinuxConfiguration linuxConfiguration = OSProfile.LinuxConfiguration.create("true",
              OSProfile.LinuxConfiguration.SSH.create(Arrays.asList(
                      OSProfile.LinuxConfiguration.SSH.SSHPublicKey.create(
                              String.format("/home/%s/.ssh/authorized_keys", adminUsername),
                              templateOptions.getPublicKey())
              ))
      );

      String locationName = template.getLocation().getId();

      final PublicIPAddressApi ipApi = api.getPublicIPAddressApi(azureGroup);

      PublicIPAddressProperties properties =
              PublicIPAddressProperties.builder()
                      .publicIPAllocationMethod("Static")
                      .idleTimeoutInMinutes(4)
                      .build();

      String publicIpAddressName = "public-address-" + name;
      PublicIPAddress ip = ipApi.createOrUpdate(publicIpAddressName, locationName, ImmutableMap.of("testkey", "testvalue"), properties);
      retry(new Predicate<String>() {
         @Override public boolean apply(String name) {
            return api.getPublicIPAddressApi(azureGroup).get(name).properties().provisioningState().equals("Succeeded");
         }
      }, 10 * 1000).apply(publicIpAddressName);
      
      String subnetId = templateOptions.getSubnetId();
      final NetworkInterfaceCardProperties networkInterfaceCardProperties =
              NetworkInterfaceCardProperties.builder()
                      .ipConfigurations(ImmutableList.of(
                              IpConfiguration.builder()
                                      .name("ipConfig-" + name)
                                      .properties(IpConfigurationProperties.builder()
                                              .privateIPAllocationMethod("Dynamic")
                                              .publicIPAddress(IdReference.create(ip.id()))
                                              .subnet(IdReference.create(subnetId))
                                              .build())
                                      .build()))
                      .build();

      String networkInterfaceCardName = "jc-nic-" + name;
      NetworkInterfaceCard nic = api.getNetworkInterfaceCardApi(azureGroup).createOrUpdate(networkInterfaceCardName, locationName, networkInterfaceCardProperties, ImmutableMap.of("jclouds", "livetest"));
      
      // StorageAccount
      String storageAccountName = null;
      String imageName = template.getImage().getName();
      if (imageName.startsWith(CUSTOM_IMAGE_PREFIX)) {
         storageAccountName = template.getImage().getVersion();
      }

      if (Strings.isNullOrEmpty(storageAccountName)) {
         storageAccountName = DeploymentTemplateBuilder.generateStorageAccountName(name);
      }

      URI uri = api.getStorageAccountApi(azureGroup).create(storageAccountName, locationName, ImmutableMap.of("property_name",
              "property_value"), ImmutableMap.of("accountType", StorageService.AccountType.Standard_LRS.toString()));
         retry(new Predicate<URI>() {
            @Override
            public boolean apply(URI uri) {
               return ParseJobStatus.JobStatus.DONE == api.getJobApi().jobStatus(uri);
            }
         }, 60 * 1 * 1000 /* 1 minute timeout */).apply(uri);
      StorageService storageService = api.getStorageAccountApi(azureGroup).get(storageAccountName);
      String blob = storageService.storageServiceProperties().primaryEndpoints().get("blob");

      ImageReference imageReference = ImageReference.builder()
              .publisher(template.getImage().getProviderId())
              .offer(template.getImage().getName())
              .sku(template.getImage().getVersion())
              .version("latest")
              .build();
      VHD vhd = VHD.create(blob + "vhds/" + name + ".vhd");
      OSDisk osDisk = OSDisk.create(null, name, vhd, "ReadWrite", "FromImage", null);
      StorageProfile storageProfile = StorageProfile.create(imageReference, osDisk, ImmutableList.<DataDisk>of());

      VirtualMachineProperties virtualMachineProperties = VirtualMachineProperties.builder()
              .licenseType(null) // TODO
              .availabilitySet(null)
              .hardwareProfile(HardwareProfile.builder().vmSize(template.getHardware().getId()).build())
              .storageProfile(storageProfile)
              .osProfile(OSProfile.builder()
                      .adminUsername(adminUsername)
                      .linuxConfiguration(linuxConfiguration)
                      .computerName(name)
                      .build())
              .networkProfile(NetworkProfile.builder()
                      .networkInterfaces(ImmutableList.of(IdReference.create(nic.id())))
                      .build())
              .build();

      VirtualMachine virtualMachine = api.getVirtualMachineApi(azureGroup).create(name, template.getLocation().getId(), virtualMachineProperties);

      //Poll until resource is ready to be used
      retry(new Predicate<String>() {
         @Override
         public boolean apply(String name) {
            return api.getVirtualMachineApi(azureGroup).get(name).properties().provisioningState() != VirtualMachineProperties.ProvisioningState.SUCCEEDED;
         }
      }, 60 * 20 * 1000).apply(name);

      VirtualMachineProperties.ProvisioningState status = api.getVirtualMachineApi(azureGroup).get(name).properties().provisioningState();
      /*
      DeploymentTemplateBuilder deploymentTemplateBuilder = api.deploymentTemplateFactory().create(group, name, template);
      DeploymentBody deploymentTemplateBody = deploymentTemplateBuilder.getDeploymentTemplate();
      DeploymentProperties properties = DeploymentProperties.create(deploymentTemplateBody);
      final String deploymentTemplate = UrlEscapers.urlFormParameterEscaper().escape(deploymentTemplateBuilder.getDeploymentTemplateJson(properties));

      logger.debug("Deployment created with name: %s group: %s", name, group);

      Deployment deployment = api.getDeploymentApi(azureGroup).create(name, deploymentTemplate);
      if (!retry(new Predicate<String>() {
         @Override
         public boolean apply(final String name) {
            Deployment deployment = api.getDeploymentApi(azureGroup).get(name);
            if (deployment == null) return false;
            Deployment.ProvisioningState state = Deployment.ProvisioningState.fromValue(deployment.properties().provisioningState());
            if (state == Deployment.ProvisioningState.FAILED) {
               logger.error(String.format("Deployment %s failed", deployment));
               cleanupResources.apply(name);
               throw new IllegalStateException(String.format("Deployment %s failed", deployment));
            }
            return state == Deployment.ProvisioningState.SUCCEEDED;
         }
      }, azureComputeConstants.operationTimeout(), 1, SECONDS).apply(deployment.name())) {
         final String illegalStateExceptionMessage = format("Deployment %s was not created within %sms so it will be destroyed.",
                 name, azureComputeConstants.operationTimeout());
         logger.warn(illegalStateExceptionMessage);
         cleanupResources.apply(name);
         throw new IllegalStateException(illegalStateExceptionMessage);
      }

      VMDeployment vmDeployment = deploymentToVMDeployment.apply(api.getDeploymentApi(azureGroup).get(name));
*/      
      // Safe to pass null credentials here, as jclouds will default populate the node with the default credentials from the image, or the ones in the options, if provided.
      return new NodeAndInitialCredentials<VirtualMachine>(virtualMachine, name, null);
   }

   @Override
   public Iterable<VMHardware> listHardwareProfiles() {

      final List<VMHardware> hwProfiles = Lists.newArrayList();
      final List<String> locationIds = Lists.newArrayList();

      Iterable<Location> locations = listLocations();
      for (Location location : locations){
         locationIds.add(location.name());

         Iterable<VMSize> vmSizes = api.getVMSizeApi(location.name()).list();

         for (VMSize vmSize : vmSizes){
            VMHardware hwProfile = VMHardware.create(
                    vmSize.name(),
                    vmSize.numberOfCores(),
                    vmSize.osDiskSizeInMB(),
                    vmSize.resourceDiskSizeInMB(),
                    vmSize.memoryInMB(),
                    vmSize.maxDataDiskCount(),
                    location.name(),
                    false);
            hwProfiles.add(hwProfile);
         }
      }

      checkAndSetHwAvailability(hwProfiles, Sets.newHashSet(locationIds));

      return hwProfiles;
   }
   private void checkAndSetHwAvailability(List<VMHardware> hwProfiles, Collection<String> locations) {
      Multimap<String, String> hwMap = ArrayListMultimap.create();
      for (VMHardware hw : hwProfiles) {
         hwMap.put(hw.name(), hw.location());
      }

      /// TODO
      //      for (VMHardware hw : hwProfiles) {
      //         hw.globallyAvailable() = hwMap.get(hw.name()).containsAll(locations);
      //      }
   }

   private List<VMImage> getImagesFromPublisher(String publisherName, String location) {
      List<VMImage> osImagesRef = Lists.newArrayList();
      OSImageApi osImageApi = api.getOSImageApi(location);
      Iterable<Offer> offerList = osImageApi.listOffers(publisherName);

      for (Offer offer : offerList) {
         Iterable<SKU> skuList = osImageApi.listSKUs(publisherName, offer.name());

         for (SKU sku : skuList) {
            Iterable<Version> versionList = osImageApi.listVersions(publisherName, offer.name(), sku.name());
            for (Version version : versionList) {
               VMImage vmImage = VMImage.create(publisherName, offer.name(), sku.name(), version.name(), location);
               osImagesRef.add(vmImage);
            }
         }
      }
      return osImagesRef;
   }

   private List<VMImage> listImagesByLocation(String location) {
      final List<VMImage> osImages = Lists.newArrayList();
      Iterable<String> publishers = Splitter.on(',').trimResults().omitEmptyStrings().split(this.azureComputeConstants.azureImagePublishers());
      for (String publisher : publishers) {
         osImages.addAll(getImagesFromPublisher(publisher, location));
      }
      return osImages;
   }

   @Override
   public Iterable<VMImage> listImages() {

      final List<VMImage> osImages = Lists.newArrayList();

      for (Location location : listLocations()){
         osImages.addAll(listImagesByLocation(location.name()));
      }
      // list custom images
      List<StorageService> storages = api.getStorageAccountApi(azureGroup).list();
      for (StorageService storage : storages) {
         String name = storage.name();
         String key = api.getStorageAccountApi(azureGroup).getKeys(name).key1();
            List<VMImage> images = BlobHelper.getImages("jclouds", azureGroup, storage.name(), key,
                  "custom", storage.location());
            osImages.addAll(images);
      }
      return osImages;
   }

   @Override
   public VMImage getImage(final String id) {
      VMImage image = VMImageToImage.decodeFieldsFromUniqueId(id);
      if (image.custom()) {
         String key = api.getStorageAccountApi(azureGroup).getKeys(image.storage()).key1();
         if (BlobHelper.customImageExists(image.storage(), key))
            return image;
         else
            return null;

      }

      String location = image.location();
      String publisher = image.publisher();
      String offer = image.offer();
      String sku = image.sku();

      OSImageApi osImageApi = api.getOSImageApi(location);
      List<Version> versions = osImageApi.listVersions(publisher, offer, sku);
      if (!versions.isEmpty()) {
         return VMImage.create(publisher, offer, sku, versions.get(0).name(), location);
      }
      return null;
   }

   @Override
   public Iterable<Location> listLocations() {
      final Iterable<String> vmLocations = FluentIterable.from(api.getResourceProviderApi().get("Microsoft.Compute"))
              .filter(new Predicate<ResourceProviderMetaData>() {
                 @Override
                 public boolean apply(ResourceProviderMetaData input) {
                    return input.resourceType().equals("virtualMachines");
                 }
              })
              .transformAndConcat(new Function<ResourceProviderMetaData, Iterable<String>>() {
                 @Override
                 public Iterable<String> apply(ResourceProviderMetaData resourceProviderMetaData) {
                    return resourceProviderMetaData.locations();
                 }
              });

      List<Location> locations = FluentIterable.from(api.getLocationApi().list())
              .filter(new Predicate<Location>() {
                 @Override
                 public boolean apply(Location location) {
                    return Iterables.contains(vmLocations, location.displayName());
                 }
              })
              .filter(new Predicate<Location>() {
                 @Override
                 public boolean apply(Location location) {
                    return regionIds.get().isEmpty() ? true : regionIds.get().contains(location.name());
                 }
              })
              .toList();

      return locations;
   }

   @Override
   public VirtualMachine getNode(final String id) {
      return api.getVirtualMachineApi(azureGroup).get(id);
   }

   @Override
   public void destroyNode(final String id) {
      checkState(cleanupResources.apply(id), "server(%s) and its resources still there after deleting!?", id);
   }

   @Override
   public void rebootNode(final String id) {
      api.getVirtualMachineApi(azureGroup).restart(id);
   }

   @Override
   public void resumeNode(final String id) {
      api.getVirtualMachineApi(azureGroup).start(id);
   }

   @Override
   public void suspendNode(final String id) {
      api.getVirtualMachineApi(azureGroup).stop(id);
   }

   @Override
   public Iterable<VirtualMachine> listNodes() {
      return api.getVirtualMachineApi(azureGroup).list();
      /*
      return FluentIterable.from(api.getDeploymentApi(azureGroup).list())
              .filter(isDeploymentInRegions)
              .filter(new Predicate<Deployment>() {
                 @Override
                 public boolean apply(Deployment deployment) {
                    Value storageAccountNameValue = deployment.properties().parameters().get("storageAccountName");
                    String storageAccountName = storageAccountNameValue.value();
                    String key = api.getStorageAccountApi(azureGroup).getKeys(storageAccountName).key1();
                    return !BlobHelper.customImageExists(storageAccountName, key);
                 }
              })
              .transform(deploymentToVMDeployment)
              .toList();
       */              
   }

   @Override
   public Iterable<VirtualMachine> listNodesByIds(final Iterable<String> ids) {
      return null; // TODO
      /*
      return Iterables.filter(listNodes(), new Predicate<VMDeployment>() {
         @Override
         public boolean apply(final VMDeployment input) {
            return Iterables.contains(ids, input.virtualMachine().name());
         }
      });
      */
   }
}
