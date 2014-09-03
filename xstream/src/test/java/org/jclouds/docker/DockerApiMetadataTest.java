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
package org.jclouds.docker;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.internal.BaseComputeServiceApiMetadataTest;
import org.jclouds.xstream.XStreamApiMetadata;
import org.testng.annotations.Test;

/**
 * Unit tests for the {@link org.jclouds.xstream.XStreamApiMetadata} class.
 */
@Test(groups = "unit", testName = "AbiquoApiMetadataTest")
public class DockerApiMetadataTest extends BaseComputeServiceApiMetadataTest {

   public DockerApiMetadataTest() {
      super(new XStreamApiMetadata());
   }

   public void testDockerApiRegistered() {
      ApiMetadata api = Apis.withId("docker");

      assertNotNull(api);
      assertTrue(api instanceof XStreamApiMetadata);
      assertEquals(api.getId(), "xstream");
   }

}

