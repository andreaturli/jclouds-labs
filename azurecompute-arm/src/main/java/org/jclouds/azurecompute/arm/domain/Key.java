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
package org.jclouds.azurecompute.arm.domain;

import java.util.Date;

import org.jclouds.javax.annotation.Nullable;
import org.jclouds.json.SerializedNames;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Key {

   @AutoValue
   public abstract static class KeyAttributes {
      
      public abstract Boolean enabled();
      public abstract Date created();
      public abstract Date updated();
      
      @SerializedNames({"enabled", "created", "updated"})
      public static KeyAttributes create(final Boolean enabled, final Date created, final Date updated) {
         return new AutoValue_Key_KeyAttributes(enabled, created, updated);
      }

      KeyAttributes() {

      }
   }

   @Nullable
   public abstract String kid();
   public abstract KeyAttributes attributes();

   @SerializedNames({ "kid", "attributes"})
   public static Key create(final String kid, final KeyAttributes attributes) {
      return new AutoValue_Key(kid, attributes);
   }

   Key() {

   }


}
