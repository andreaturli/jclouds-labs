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
package org.jclouds.azurecompute.arm.compute.extensions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.jclouds.azurecompute.arm.compute.functions.VMImageToImage.decodeFieldsFromUniqueId;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_IMAGE_AVAILABLE;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_NODE_SUSPENDED;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Resource;

import org.jclouds.Constants;
import org.jclouds.azurecompute.arm.AzureComputeApi;
import org.jclouds.azurecompute.arm.compute.config.AzureComputeServiceContextModule.AzureComputeConstants;
import org.jclouds.azurecompute.arm.compute.functions.ResourceDefinitionToCustomImage;
import org.jclouds.azurecompute.arm.domain.ResourceDefinition;
import org.jclouds.azurecompute.arm.domain.StorageServiceKeys;
import org.jclouds.azurecompute.arm.domain.VMImage;
import org.jclouds.azurecompute.arm.functions.CleanupResources;
import org.jclouds.azurecompute.arm.util.BlobHelper;
import org.jclouds.compute.domain.CloneImageTemplate;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.ImageTemplate;
import org.jclouds.compute.domain.ImageTemplateBuilder;
import org.jclouds.compute.extensions.ImageExtension;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class AzureComputeImageExtension implements ImageExtension {
   public static final String CONTAINER_NAME = "jclouds";
   public static final String CUSTOM_IMAGE_OFFER = "custom";

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final AzureComputeApi api;
   private final String group;
   private final ListeningExecutorService userExecutor;
   private final Predicate<URI> imageAvailablePredicate;
   private final Predicate<String> nodeSuspendedPredicate;
   private final ResourceDefinitionToCustomImage.Factory resourceDefinitionToImage;
   private final CleanupResources cleanupResources;

   @Inject
   AzureComputeImageExtension(AzureComputeApi api,
         @Named(TIMEOUT_IMAGE_AVAILABLE) Predicate<URI> imageAvailablePredicate,
         @Named(TIMEOUT_NODE_SUSPENDED) Predicate<String> nodeSuspendedPredicate,
         AzureComputeConstants azureComputeConstants,
         @Named(Constants.PROPERTY_USER_THREADS) ListeningExecutorService userExecutor,
         ResourceDefinitionToCustomImage.Factory resourceDefinitionToImage,
         CleanupResources cleanupResources) {
      this.api = api;
      this.imageAvailablePredicate = imageAvailablePredicate;
      this.nodeSuspendedPredicate = nodeSuspendedPredicate;
      this.group = azureComputeConstants.azureResourceGroup();
      this.userExecutor = userExecutor;
      this.resourceDefinitionToImage = resourceDefinitionToImage;
      this.cleanupResources = cleanupResources;
   }

   @Override
   public ImageTemplate buildImageTemplateFromNode(String name, String id) {
      return new ImageTemplateBuilder.CloneImageTemplateBuilder().nodeId(id).name(name.toLowerCase()).build();
   }

   @Override
   public ListenableFuture<Image> createImage(ImageTemplate template) {
      final CloneImageTemplate cloneTemplate = (CloneImageTemplate) template;
      final String id = cloneTemplate.getSourceNodeId();
      final String name = cloneTemplate.getName();

      logger.debug(">> stopping node %s...", id);
      api.getVirtualMachineApi(group).stop(id);
      checkState(nodeSuspendedPredicate.apply(id), "Node %s was not suspended within the configured time limit", id);

      return userExecutor.submit(new Callable<Image>() {
         @Override
         public Image call() throws Exception {
            logger.debug(">> generalizing virtal machine %s...", id);
            api.getVirtualMachineApi(group).generalize(id);

            logger.debug(">> capturing virtual machine %s to container %s...", id, CONTAINER_NAME);
            URI uri = api.getVirtualMachineApi(group).capture(id, cloneTemplate.getName(), CONTAINER_NAME);
            checkState(uri != null && imageAvailablePredicate.apply(uri),
                  "Image %s was not created within the configured time limit", cloneTemplate.getName());

            List<ResourceDefinition> definitions = api.getJobApi().captureStatus(uri);
            checkState(definitions.size() == 1,
                  "Expected one resource definition after creating the image but %s were returned", definitions.size());

            Image image =  resourceDefinitionToImage.create(id, name).apply(definitions.get(0));
            logger.debug(">> created %s", image);
            return image;
         }
      });
   }

   @Override
   public boolean deleteImage(String id) {
      VMImage image = decodeFieldsFromUniqueId(id);
      checkArgument(image.custom(), "Only custom images can be deleted");
      
      logger.debug(">> deleting image %s", id);

      StorageServiceKeys keys = api.getStorageAccountApi(image.group()).getKeys(image.storage());
      // This removes now all the images in this storage. At least in theory,
      // there should be just one and if there is
      // more, they should be copies of each other.
      // TODO: Reuse the blobstore context in these two calls
      BlobHelper.deleteContainerIfExists(image.storage(), keys.key1(), "system");
      boolean result = !BlobHelper.customImageExists(image.storage(), keys.key1());

      if (!BlobHelper.hasContainers(image.storage(), keys.key1())) {
         logger.debug(">> storage account is empty after deleting the custom image. Deleting the storage account...");
         api.getStorageAccountApi(image.group()).delete(image.storage());
         cleanupResources.deleteResourceGroupIfEmpty(image.group());
      }

      return result;
   }
}
