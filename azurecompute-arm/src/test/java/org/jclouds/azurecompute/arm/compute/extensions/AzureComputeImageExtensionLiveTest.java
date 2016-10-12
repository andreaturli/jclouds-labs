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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.azurecompute.arm.config.AzureComputeProperties.IMAGE_PUBLISHERS;
import static org.jclouds.azurecompute.arm.config.AzureComputeProperties.RESOURCE_GROUP_NAME;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_NODE_RUNNING;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_NODE_SUSPENDED;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_NODE_TERMINATED;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_PORT_OPEN;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;
import static org.jclouds.location.reference.LocationConstants.PROPERTY_REGIONS;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.jclouds.azurecompute.arm.AzureComputeProviderMetadata;
import org.jclouds.azurecompute.arm.internal.AzureLiveTestUtils;
import org.jclouds.compute.extensions.internal.BaseImageExtensionLiveTest;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.testng.annotations.Test;

import com.google.inject.Module;

/**
 * Live tests for the {@link org.jclouds.compute.extensions.ImageExtension} integration.
 */
@Test(groups = "live", singleThreaded = true, testName = "AzureComputeImageExtensionLiveTest")
public class AzureComputeImageExtensionLiveTest extends BaseImageExtensionLiveTest {

   public AzureComputeImageExtensionLiveTest() {
      provider = "azurecompute-arm";
   }

   public static String NAME_PREFIX = "%s";

   @Override
   protected Module getSshModule() {
      return new SshjSshClientModule();
   }

   @Override
   protected Properties setupProperties() {
      Properties properties = super.setupProperties();
      String defaultTimeout = String.valueOf(TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES));
      properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, defaultTimeout);
      properties.setProperty(TIMEOUT_NODE_RUNNING, defaultTimeout);
      properties.setProperty(TIMEOUT_PORT_OPEN, defaultTimeout);
      properties.setProperty(TIMEOUT_NODE_TERMINATED, defaultTimeout);
      properties.setProperty(TIMEOUT_NODE_SUSPENDED, defaultTimeout);
      properties.put(RESOURCE_GROUP_NAME, "jc");
      properties.put(PROPERTY_REGIONS, "eastus");
      properties.put(IMAGE_PUBLISHERS, "Canonical");

      AzureLiveTestUtils.defaultProperties(properties);
      checkNotNull(setIfTestSystemPropertyPresent(properties, "oauth.endpoint"), "test.oauth.endpoint");

      return properties;
   }

   @Override
   protected ProviderMetadata createProviderMetadata() {
      return AzureComputeProviderMetadata.builder().build();
   }

}
