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
import org.jclouds.aliyun.ecs.domain.Image;
import org.jclouds.aliyun.ecs.domain.KeyPair;
import org.jclouds.aliyun.ecs.domain.Regions;
import org.jclouds.aliyun.ecs.domain.options.ListImagesOptions;
import org.jclouds.aliyun.ecs.domain.options.ListKeyPairsOptions;
import org.jclouds.aliyun.ecs.domain.options.PaginationOptions;
import org.jclouds.collect.IterableWithMarker;
import org.testng.annotations.Test;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test(groups = "unit", testName = "SshKeyPairApiMockTest", singleThreaded = true)
public class SshKeyPairApiMockTest extends BaseECSComputeServiceApiMockTest {

   public void testListImages() throws InterruptedException {
      server.enqueue(jsonResponse("/keypairs-first.json"));
      server.enqueue(jsonResponse("/keypairs-last.json"));

      Iterable<KeyPair> keypairs = api.sshKeyPairApi().list(Regions.EU_CENTRAL_1.getName()).concat();
      assertEquals(size(keypairs), 12);
      assertEquals(server.getRequestCount(), 2);
      assertSent(server, "GET", "DescribeKeyPairs");
      assertSent(server, "GET", "DescribeKeyPairs", 2);
   }

   public void testListKeyPairsReturns404() {
      server.enqueue(response404());
      Iterable<KeyPair> keypairs = api.sshKeyPairApi().list(Regions.EU_CENTRAL_1.getName()).concat();
      assertTrue(isEmpty(keypairs));
      assertEquals(server.getRequestCount(), 1);
   }

   public void testListKeyPairsWithOptions() throws InterruptedException {
      server.enqueue(jsonResponse("/keypairs-first.json"));

      IterableWithMarker<KeyPair> keypairs = api.sshKeyPairApi().list(Regions.EU_CENTRAL_1.getName(), ListKeyPairsOptions.Builder
              .paginationOptions(PaginationOptions.Builder.pageNumber(1)));

      assertEquals(size(keypairs), 10);
      assertEquals(server.getRequestCount(), 1);

      assertSent(server, "GET", "DescribeKeyPairs", 1);
   }

   public void testListKeyPairsWithOptionsReturns404() throws InterruptedException {
      server.enqueue(response404());

      IterableWithMarker<KeyPair> keypairs = api.sshKeyPairApi().list(Regions.EU_CENTRAL_1.getName(), ListKeyPairsOptions.Builder
              .paginationOptions(PaginationOptions.Builder.pageNumber(2)));

      assertTrue(isEmpty(keypairs));

      assertEquals(server.getRequestCount(), 1);
      assertSent(server, "GET", "DescribeKeyPairs", 2);
   }

}
