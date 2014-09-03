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
package org.jclouds.docker.compute.options;

import static org.testng.Assert.assertEquals;

import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.xstream.compute.options.XStreamTemplateOptions;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * Unit tests for the {@link org.jclouds.xstream.compute.options.XStreamTemplateOptions} class.
 */
@Test(groups = "unit", testName = "DockerTemplateOptionsTest")
public class DockerTemplateOptionsTest {

   @Test
   public void testHostname() {
      TemplateOptions options = new XStreamTemplateOptions().hostname("hostname");
      assertEquals(options.as(XStreamTemplateOptions.class).getHostname(), Optional.of("hostname"));
   }

   @Test
   public void testMemory() {
      TemplateOptions options = new XStreamTemplateOptions().memory(1024);
      assertEquals(options.as(XStreamTemplateOptions.class).getMemory(), Optional.of(1024));
   }

   @Test
   public void testCpuShares() {
      TemplateOptions options = new XStreamTemplateOptions().cpuShares(2);
      assertEquals(options.as(XStreamTemplateOptions.class).getCpuShares(), Optional.of(2));
   }

   @Test
   public void testVolumes() {
      TemplateOptions options = new XStreamTemplateOptions().volumes(ImmutableMap.of("/tmp", "/tmp"));
      assertEquals(options.as(XStreamTemplateOptions.class).getVolumes(), Optional.of(ImmutableMap.of("/tmp", "/tmp")));
   }

   @Test
   public void testDns() {
      TemplateOptions options = new XStreamTemplateOptions().dns("8.8.8.8");
      assertEquals(options.as(XStreamTemplateOptions.class).getDns(), Optional.of("8.8.8.8"));
   }
}
