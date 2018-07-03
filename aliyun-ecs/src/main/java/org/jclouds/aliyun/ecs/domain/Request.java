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
package org.jclouds.aliyun.ecs.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.beans.ConstructorProperties;

import static com.google.common.base.Preconditions.checkNotNull;

public class Request {

   private final String requestId;

   @ConstructorProperties({ "RequestId" })
   public Request(String requestId) {
      this.requestId = checkNotNull(requestId, "requestId");
   }

   public String getRequestId() {
      return requestId;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      Request request = (Request) o;
      return Objects.equal(requestId, request.requestId);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(requestId);
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).add("requestId", requestId).toString();
   }
}
