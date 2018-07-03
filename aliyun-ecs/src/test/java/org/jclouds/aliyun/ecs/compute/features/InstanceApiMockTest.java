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
package org.jclouds.aliyun.ecs.compute.features;

import org.jclouds.aliyun.ecs.compute.internal.BaseECSComputeServiceApiMockTest;
import org.jclouds.aliyun.ecs.domain.Instance;
import org.jclouds.aliyun.ecs.domain.InstanceType;
import org.jclouds.aliyun.ecs.domain.Regions;
import org.testng.annotations.Test;

import java.util.List;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test(groups = "unit", testName = "InstanceApiMockTest", singleThreaded = true)
public class InstanceApiMockTest extends BaseECSComputeServiceApiMockTest {

   public void testListInstances() throws InterruptedException {
      server.enqueue(jsonResponse("/instances-first.json"));
      server.enqueue(jsonResponse("/instances-last.json"));

      Iterable<Instance> instances = api.instanceApi().list(Regions.EU_CENTRAL_1.getName()).concat();
      assertEquals(size(instances), 20); // Force the PagedIterable to advance
      assertEquals(server.getRequestCount(), 2);
      assertSent(server, "GET", "DescribeInstances");
      assertSent(server, "GET", "DescribeInstances", 2);
   }

   public void testListInstancesReturns404() {
      server.enqueue(response404());
      Iterable<Instance> instances = api.instanceApi().list(Regions.EU_CENTRAL_1.getName()).concat();
      assertTrue(isEmpty(instances));
      assertEquals(server.getRequestCount(), 1);
   }

   public void testListInstanceTypes() throws InterruptedException {
      server.enqueue(jsonResponse("/instanceTypes.json"));

      List<InstanceType> instanceTypes = api.instanceApi().listTypes();
      assertEquals(size(instanceTypes), 308);
      assertEquals(server.getRequestCount(), 1);
      assertSent(server, "GET", "DescribeInstanceTypes");
   }

   public void testListInstanceTypesReturns404() {
      server.enqueue(response404());
      List<InstanceType> instanceTypes = api.instanceApi().listTypes();
      assertTrue(isEmpty(instanceTypes));
      assertEquals(server.getRequestCount(), 1);
   }

}
