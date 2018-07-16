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

import com.google.common.collect.ImmutableMap;
import org.jclouds.aliyun.ecs.compute.internal.BaseECSComputeServiceApiMockTest;
import org.jclouds.aliyun.ecs.domain.Regions;
import org.jclouds.aliyun.ecs.domain.VSwitch;
import org.jclouds.collect.IterableWithMarker;
import org.testng.annotations.Test;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.jclouds.aliyun.ecs.domain.options.ListVSwitchesOptions.Builder.paginationOptions;
import static org.jclouds.aliyun.ecs.domain.options.PaginationOptions.Builder.pageNumber;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test(groups = "unit", testName = "VSwitchApiMockTest", singleThreaded = true)
public class VSwitchApiMockTest extends BaseECSComputeServiceApiMockTest {

   public void testListVSwitches() throws InterruptedException {
      server.enqueue(jsonResponse("/vswitches-first.json"));
      server.enqueue(jsonResponse("/vswitches-last.json"));
      Iterable<VSwitch> vSwitches = api.vSwitchApi().list(Regions.EU_CENTRAL_1.getName()).concat();
      assertEquals(size(vSwitches), 2); // Force the PagedIterable to advance
      assertEquals(server.getRequestCount(), 2);
      assertSent(server, "GET", "DescribeVSwitches", ImmutableMap.of("RegionId", Regions.EU_CENTRAL_1.getName()));
      assertSent(server, "GET", "DescribeVSwitches", ImmutableMap.of("RegionId", Regions.EU_CENTRAL_1.getName()), 2);
   }

   public void testListVSwitchesReturns404() throws InterruptedException {
      server.enqueue(response404());
      Iterable<VSwitch> vSwitches = api.vSwitchApi().list(Regions.EU_CENTRAL_1.getName()).concat();
      assertTrue(isEmpty(vSwitches));
      assertEquals(server.getRequestCount(), 1);
      assertSent(server, "GET", "DescribeVSwitches", ImmutableMap.of("RegionId", Regions.EU_CENTRAL_1.getName()));
   }

   public void testListVSwitchesWithOptions() throws InterruptedException {
      server.enqueue(jsonResponse("/vswitches-first.json"));
      IterableWithMarker<VSwitch> vSwitches = api.vSwitchApi().list(Regions.EU_CENTRAL_1.getName(), paginationOptions(pageNumber(1).pageSize(5)));
      assertEquals(size(vSwitches), 1);
      assertEquals(server.getRequestCount(), 1);
      assertSent(server, "GET", "DescribeVSwitches", ImmutableMap.of("RegionId", Regions.EU_CENTRAL_1.getName()), 1);
   }

   public void testListVSwitchesWithOptionsReturns404() throws InterruptedException {
      server.enqueue(response404());
      IterableWithMarker<VSwitch> vSwitches = api.vSwitchApi().list(Regions.EU_CENTRAL_1.getName(), paginationOptions(pageNumber(2)));
      assertTrue(isEmpty(vSwitches));
      assertEquals(server.getRequestCount(), 1);
      assertSent(server, "GET", "DescribeVSwitches", ImmutableMap.of("RegionId", Regions.EU_CENTRAL_1.getName()), 2);
   }

}
