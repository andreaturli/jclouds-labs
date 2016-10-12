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
package org.jclouds.azurecompute.arm.util;

import static org.jclouds.util.Closeables2.closeQuietly;

import java.util.ArrayList;
import java.util.List;

import org.jclouds.ContextBuilder;
import org.jclouds.azureblob.AzureBlobClient;
import org.jclouds.azureblob.domain.BlobProperties;
import org.jclouds.azureblob.domain.ContainerProperties;
import org.jclouds.azureblob.domain.ListBlobsResponse;
import org.jclouds.azurecompute.arm.domain.VMImage;

public class BlobHelper {

   public static void deleteContainerIfExists(String storage, String key, String containerName) {
      final AzureBlobClient azureBlob = ContextBuilder.newBuilder("azureblob").credentials(storage, key)
            .buildApi(AzureBlobClient.class);

      try {
         azureBlob.deleteContainer(containerName);
      } finally {
         closeQuietly(azureBlob);
      }
   }
   
   public static boolean hasContainers(String storage, String key) {
      final AzureBlobClient azureBlob = ContextBuilder.newBuilder("azureblob").credentials(storage, key)
            .buildApi(AzureBlobClient.class);

      try {
         return !azureBlob.listContainers().isEmpty();
      } finally {
         closeQuietly(azureBlob);
      }
   }

   public static boolean customImageExists(String storage, String key) {
      final AzureBlobClient azureBlob = ContextBuilder.newBuilder("azureblob").credentials(storage, key)
            .buildApi(AzureBlobClient.class);

      try {
         return azureBlob.containerExists("system");
      } finally {
         closeQuietly(azureBlob);
      }
   }

   public static List<VMImage> getImages(String containerName, String group, String storageAccountName, String key,
         String offer, String location) {
      final AzureBlobClient azureBlob = ContextBuilder.newBuilder("azureblob").credentials(storageAccountName, key)
            .buildApi(AzureBlobClient.class);

      List<VMImage> list = new ArrayList<VMImage>();

      try {
         ContainerProperties systemContainer = azureBlob.getContainerProperties("system");
         if (systemContainer != null) {
            ListBlobsResponse blobList = azureBlob.listBlobs(systemContainer.getName());
            for (BlobProperties blob : blobList) {
               String name = blob.getName();

               if (name.contains("-osDisk")) {
                  String imageName = name.substring(name.lastIndexOf('/') + 1, name.indexOf("-osDisk"));
                  String imageUrl = blob.getUrl().toString();

                  list.add(VMImage.customImage().group(group).storage(storageAccountName).vhd1(imageUrl).name(imageName)
                        .offer(offer).location(location).build());
               }
            }
         }
      } finally {
         closeQuietly(azureBlob);
      }

      return list;
   }
}
