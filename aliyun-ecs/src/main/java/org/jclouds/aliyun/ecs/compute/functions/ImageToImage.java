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
package org.jclouds.aliyun.ecs.compute.functions;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import org.jclouds.aliyun.ecs.compute.functions.internal.OperatingSystems;
import org.jclouds.aliyun.ecs.domain.Image;
import org.jclouds.compute.domain.ImageBuilder;
import org.jclouds.compute.domain.OsFamily;

import java.util.Map;

import static com.google.common.collect.Iterables.tryFind;
import static java.util.Arrays.asList;
import static org.jclouds.compute.domain.OperatingSystem.builder;

public class ImageToImage implements Function<Image, org.jclouds.compute.domain.Image> {

   private static final Map<String, OsFamily> OTHER_OS_MAP = ImmutableMap.<String, OsFamily>builder()
         .put("Aliyun", OsFamily.LINUX).build();

   private static Optional<OsFamily> findInStandardFamilies(final String platform) {
      return tryFind(asList(OsFamily.values()), new Predicate<OsFamily>() {
         @Override
         public boolean apply(OsFamily input) {
            return platform.toUpperCase().startsWith(input.name());
         }
      });
   }

   private static Optional<OsFamily> findInOtherOSMap(final String label) {
      return tryFind(OTHER_OS_MAP.keySet(), new Predicate<String>() {
         @Override
         public boolean apply(String input) {
            return label.contains(input);
         }
      }).transform(new Function<String, OsFamily>() {
         @Override
         public OsFamily apply(String input) {
            return OTHER_OS_MAP.get(input);
         }
      });
   }

   @Override
   public org.jclouds.compute.domain.Image apply(Image input) {
      ImageBuilder builder = new ImageBuilder();
      builder.ids(input.imageId());
      builder.name(input.imageName());
      builder.description(input.description());
      builder.status("Available".equals(input.status()) ?
            org.jclouds.compute.domain.Image.Status.AVAILABLE :
            org.jclouds.compute.domain.Image.Status.PENDING);

      OsFamily family = findInStandardFamilies(input.platform()).or(findInOtherOSMap(input.platform()))
            .or(OsFamily.UNRECOGNIZED);

      String osVersion = OperatingSystems.version().apply(input);

      builder.operatingSystem(
            builder().name(input.osName()).family(family).description(input.osType()).version(osVersion)
                  .is64Bit("x86_64".equals(input.architecture()) ? true : false).build());

      return builder.build();
   }

}
