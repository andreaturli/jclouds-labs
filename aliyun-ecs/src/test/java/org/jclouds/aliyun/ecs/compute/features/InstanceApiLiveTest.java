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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.jclouds.aliyun.ecs.compute.internal.BaseECSComputeServiceApiLiveTest;
import org.jclouds.aliyun.ecs.domain.Instance;
import org.jclouds.aliyun.ecs.domain.InstanceRequest;
import org.jclouds.aliyun.ecs.domain.InstanceType;
import org.jclouds.aliyun.ecs.domain.Regions;
import org.jclouds.aliyun.ecs.domain.options.CreateInstanceOptions;
import org.jclouds.aliyun.ecs.domain.options.ListInstancesOptions;
import org.jclouds.aliyun.ecs.features.InstanceApi;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.util.Strings.isNullOrEmpty;

@Test(groups = "live", testName = "InstanceApiLiveTest")
public class InstanceApiLiveTest extends BaseECSComputeServiceApiLiveTest {

   private String instanceId;
   private String imageId = "ubuntu_16_0402_32_20G_alibase_20180409.vhd";
   private String securityGroupId = "sg-gw8izkhfxvoemvocgdsj";
   private String instanceType = "ecs.t5-lc2m1.nano";
   private String hostname = "jclouds";
   private String vSwitchId = "vsw-gw8c79bsp4ezbe34ij3w8";

   @BeforeClass
   public void setUp() {
      // TODO create security group
      if (instanceId != null) {
         api().delete(instanceId);
      }
   }

   @AfterClass
   public void tearDown() {
      if (securityGroupId != null) {
      // TODO delete security group
      }
      if (instanceId != null) {
         api().delete(instanceId);
      }
   }

   public void testListInstanceType() {
      final AtomicInteger found = new AtomicInteger(0);
      assertTrue(Iterables.all(api().listTypes(), new Predicate<InstanceType>() {
         @Override
         public boolean apply(InstanceType input) {
            found.incrementAndGet();
            return !isNullOrEmpty(input.instanceTypeId());
         }
      }), "All instance types must have the 'id' field populated");
      assertTrue(found.get() > 0, "Expected some instance type to be returned");
   }

   @Test(groups = "live", dependsOnMethods = "testListInstanceType")
   public void testListInstance() {
      final AtomicInteger found = new AtomicInteger(0);
      assertTrue(Iterables.all(api().list(Regions.EU_CENTRAL_1.getName()).concat(), new Predicate<Instance>() {
         @Override
         public boolean apply(Instance input) {
            found.incrementAndGet();
            return !isNullOrEmpty(input.instanceId());
         }
      }), "All instances must have the 'id' field populated");
   }

   @Test(groups = "live", dependsOnMethods = "testListInstance")
   public void testCreate() {
      InstanceRequest instanceRequest = api().create(Regions.EU_CENTRAL_1.getName(), imageId, securityGroupId, hostname, instanceType,
            CreateInstanceOptions.Builder.vSwitchId(vSwitchId));
      instanceId = instanceRequest.getInstanceId();
      assertNotNull(instanceId, "Instance must not be null");
   }

   @Test(groups = "live", dependsOnMethods = "testCreate")
   //         dependsOnMethods = "testListInstance")
   public void testGet() {
      Instance instance = Iterables.getOnlyElement(api().list(Regions.EU_CENTRAL_1.getName(), ListInstancesOptions.Builder.instanceIds(instanceId)));
      assertNotNull(instance.instanceId(), "Instance must not be null");
   }

   @Test(groups = "live", dependsOnMethods = "testGet")
   public void testStartInstance() {
      api().powerOn(instanceId);
   }

   @Test(groups = "live", dependsOnMethods = "testStartInstance")
   public void testStopInstance() {
      api().powerOff(instanceId);
   }

   private InstanceApi api() {
      return api.instanceApi();
   }
}
