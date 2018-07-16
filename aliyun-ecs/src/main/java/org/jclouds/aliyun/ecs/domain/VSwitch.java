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
import com.google.common.collect.ImmutableMap;
import org.jclouds.json.SerializedNames;

import java.util.Date;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class VSwitch {

   VSwitch() {}

   @SerializedNames({"CidrBlock", "CreationTime", "Description", "ZoneId", "Status",
           "AvailableIpAddressCount", "VpcId", "VSwitchId", "VSwitchName" })
   public static VSwitch create(String cidrBlock, Date creationTime, String description, String zoneId, String status,
                                int availableIpAddressCount,
                                String vpcId, String vSwitchId, String vSwitchName) {
      return new AutoValue_VSwitch(cidrBlock, creationTime, description, zoneId, status,
              availableIpAddressCount, vpcId,  vSwitchId, vSwitchName);
   }

   public abstract String cidrBlock();

   public abstract Date creationTime();

   public abstract String description();

   public abstract String zoneId();

   public abstract String status();

   public abstract int availableIpAddressCount();

   public abstract String vpcId();

   public abstract String vSwitchId();

   public abstract String vSwitchName();

}
