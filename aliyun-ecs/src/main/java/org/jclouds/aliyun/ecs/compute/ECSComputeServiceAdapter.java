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
package org.jclouds.aliyun.ecs.compute;

import org.jclouds.aliyun.ecs.ECSComputeServiceApi;
import org.jclouds.aliyun.ecs.domain.Image;
import org.jclouds.aliyun.ecs.domain.Instance;
import org.jclouds.aliyun.ecs.domain.InstanceType;
import org.jclouds.aliyun.ecs.domain.Region;
import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.domain.Template;

import javax.inject.Singleton;

/**
 * defines the connection between the {@link ECSComputeServiceApi} implementation and
 * the jclouds {@link org.jclouds.compute.ComputeService}
 */
@Singleton
public class ECSComputeServiceAdapter implements ComputeServiceAdapter<Instance, InstanceType, Image, Region> {

   @Override
   public NodeAndInitialCredentials<Instance> createNodeWithGroupEncodedIntoName(String s, String s1, Template template) {
      return null;
   }

   @Override
   public Iterable<InstanceType> listHardwareProfiles() {
      return null;
   }

   @Override
   public Iterable<Image> listImages() {
      return null;
   }

   @Override
   public Image getImage(String s) {
      return null;
   }

   @Override
   public Iterable<Region> listLocations() {
      return null;
   }

   @Override
   public Instance getNode(String s) {
      return null;
   }

   @Override
   public void destroyNode(String s) {

   }

   @Override
   public void rebootNode(String s) {

   }

   @Override
   public void resumeNode(String s) {

   }

   @Override
   public void suspendNode(String s) {

   }

   @Override
   public Iterable<Instance> listNodes() {
      return null;
   }

   @Override
   public Iterable<Instance> listNodesByIds(Iterable<String> iterable) {
      return null;
   }
}