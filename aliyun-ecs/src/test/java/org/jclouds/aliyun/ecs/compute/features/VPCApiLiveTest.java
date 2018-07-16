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
import org.jclouds.aliyun.ecs.domain.Regions;
import org.jclouds.aliyun.ecs.domain.Request;
import org.jclouds.aliyun.ecs.domain.SecurityGroupRequest;
import org.jclouds.aliyun.ecs.domain.Tag;
import org.jclouds.aliyun.ecs.domain.VPC;
import org.jclouds.aliyun.ecs.domain.VPCRequest;
import org.jclouds.aliyun.ecs.domain.options.CreateSecurityGroupOptions;
import org.jclouds.aliyun.ecs.domain.options.CreateVPCOptions;
import org.jclouds.aliyun.ecs.domain.options.TagOptions;
import org.jclouds.aliyun.ecs.features.TagApi;
import org.jclouds.aliyun.ecs.features.VPCApi;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.util.Strings.isNullOrEmpty;

@Test(groups = "live", testName = "VPCApiLiveTest")
public class VPCApiLiveTest extends BaseECSComputeServiceApiLiveTest {

   public static final String VPC_NAME = "jclouds-vpc";

   private String vpcId;

   @BeforeClass
   public void setUp() {
      VPCRequest vpcRequest = api().create(Regions.EU_CENTRAL_1.getName(), CreateVPCOptions.Builder.vpcName(VPC_NAME));
      assertNotNull(vpcRequest.getRequestId());
      assertNotNull(vpcRequest.getVpcId());
      vpcId = vpcRequest.getVpcId();
   }

   @AfterClass
   public void tearDown() {
      if (vpcId != null) {
         assertNotNull(api().delete(Regions.EU_CENTRAL_1.getName(), vpcId));
      }
   }

   public void testList() {
      final AtomicInteger found = new AtomicInteger(0);
      assertTrue(Iterables.all(api().list(Regions.EU_CENTRAL_1.getName()).concat(), new Predicate<VPC>() {
         @Override
         public boolean apply(VPC input) {
            found.incrementAndGet();
            return !isNullOrEmpty(input.vpcId());
         }
      }), "All vpcs must have at least the 'id' field populated");
      assertTrue(found.get() > 0, "Expected some vpc to be returned");
   }

   private VPCApi api() {
      return api.vpcApi();
   }
}
