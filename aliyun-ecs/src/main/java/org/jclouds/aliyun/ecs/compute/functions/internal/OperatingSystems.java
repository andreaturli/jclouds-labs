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
package org.jclouds.aliyun.ecs.compute.functions.internal;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.jclouds.aliyun.ecs.domain.Image;
import org.jclouds.compute.domain.OsFamily;

public class OperatingSystems {

   public static Function<Image, String> version() {
      return new Function<Image, String>() {
         @Override
         public String apply(final Image image) {
            return parseVersion(image);
         }
      };
   }

   private static String parseVersion(Image image) {
      String sequence = image.osName().trim().replaceAll("\\s+", " ");
      int offset = 2;
      if (isWindows(image)) {
         sequence = image.platform();
         offset = 1;
      }
      Iterable<String> splitted = Splitter.on(" ").split(sequence);
      return Iterables.get(splitted, Iterables.size(splitted) - offset);
   }

   public static boolean isWindows(Image image) {
      return image.platform().toUpperCase().startsWith(OsFamily.WINDOWS.name());
   }

}
