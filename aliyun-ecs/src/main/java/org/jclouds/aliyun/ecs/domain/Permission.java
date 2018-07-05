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

import com.google.auto.value.AutoValue;
import org.jclouds.json.SerializedNames;

import java.util.Date;

@AutoValue
public abstract class Permission {

   Permission() {
   }

   @SerializedNames(
         { "SourceCidrIp", "DestCidrIp", "Description", "NicType",
                 "DestGroupName", "PortRange", "DestGroupId", "Direction", "Priority",
                 "IpProtocol", "SourceGroupOwnerAccount", "Policy", "CreateTime",
               "SourceGroupId", "DestGroupOwnerAccount", "SourceGroupName" })
   public static Permission create(String sourceCidrIp, String destCidrIp, String description, String nicType,
                                   String destGroupName, String portRange, String destGroupId, String direction, String priority,
                                   String ipProtocol, String sourceGroupOwnerAccount, String policy, Date createTime, String sourceGroupId,
                                   String destGroupOwnerAccount, String sourceGroupName) {
      return new AutoValue_Permission(sourceCidrIp, destCidrIp, description, nicType, destGroupName, portRange,
            destGroupId, direction, priority, ipProtocol, sourceGroupOwnerAccount, policy, createTime, sourceGroupId,
            destGroupOwnerAccount, sourceGroupName);
   }

   public abstract String sourceCidrIp();

   public abstract String destCidrIp();

   public abstract String description();

   public abstract String nicType();

   public abstract String destGroupName();

   public abstract String portRange();

   public abstract String destGroupId();

   public abstract String direction();

   public abstract String priority();

   public abstract String ipProtocol();

   public abstract String sourceGroupOwnerAccount();

   public abstract String policy();

   public abstract Date createTime();

   public abstract String sourceGroupId();

   public abstract String destGroupOwnerAccount();

   public abstract String sourceGroupName();

}
