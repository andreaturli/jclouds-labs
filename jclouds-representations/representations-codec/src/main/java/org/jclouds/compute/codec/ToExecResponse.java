/*
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jclouds.compute.codec;

import com.google.common.base.Function;
import org.jclouds.compute.representations.ExecResponse;
import org.jclouds.javax.annotation.Nullable;

public enum ToExecResponse implements Function<org.jclouds.compute.domain.ExecResponse, ExecResponse> {

   INSTANCE;

   @Override
   public ExecResponse apply(@Nullable org.jclouds.compute.domain.ExecResponse input) {
      if (input == null) {
         return null;
      }
      return ExecResponse.builder().output(input.getOutput()).error(input.getError()).exitStatus(input.getExitStatus())
              .build();
   }
}