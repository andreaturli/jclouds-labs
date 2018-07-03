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
import com.google.common.base.Enums;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.json.SerializedNames;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@AutoValue
public abstract class Instance {

   Instance() {}

   @SerializedNames(
         { "InnerIpAddress", "ImageId", "InstanceTypeFamily", "VlanId", "NetworkInterfaces", "InstanceId", "EipAddress",
               "InternetMaxBandwidthIn", "ZoneId", "InternetChargeType", "SpotStrategy", "StoppedMode", "SerialNumber",
               "IoOptimized", "Memory", "Cpu", "VpcAttributes", "InternetMaxBandwidthOut", "DeviceAvailable",
               "SecurityGroupIds", "SaleCycle", "SpotPriceLimit", "AutoReleaseTime", "StartTime", "InstanceName",
               "Description", "ResourceGroupId", "OSType", "OSName", "InstanceNetworkType", "PublicIpAddress",
               "HostName", "InstanceType", "CreationTime", "Status", "Tags", "ClusterId", "Recyclable", "RegionId",
               "GPUSpec", "DedicatedHostAttribute", "OperationLocks", "InstanceChargeType", "GPUAmount",
               "ExpiredTime" })
   public static Instance create(Map<String, List<String>> innerIpAddress, String imageId, String instanceTypeFamily,
                                 String vlanId, Map<String, List<NetworkInterface>> networkInterfaces, String instanceId, EipAddress eipAddress,
                                 Integer internetMaxBandwidthIn, String zoneId, String internetChargeType, String spotStrategy,
                                 String stoppedMode, String serialNumber, Boolean ioOptimized, Integer memory, Integer cpu,
                                 VpcAttributes vpcAttributes, Integer internetMaxBandwidthOut, Boolean deviceAvailable,
                                 Map<String, List<String>> securityGroupIds, String saleCycle, Double spotPriceLimit, String autoReleaseTime,
                                 Date startTime, String instanceName, String description, String resourceGroupId, String osType, String osName,
                                 String instanceNetworkType, Map<String, List<String>> publicIpAddress, String hostname, String instanceType,
                                 Date creationTime, Status status, Map<String, List<Tag>> tags, String clusterId, Boolean recyclable,
                                 String regionId, String gpuSpec, DedicatedHostAttribute dedicatedHostAttribute,
                                 Map<String, List<String>> operationLocks, String InstanceChargeType, Integer gpuAmount, Date expiredTime) {
      return new AutoValue_Instance(
            innerIpAddress == null ? ImmutableMap.<String, List<String>>of() : ImmutableMap.copyOf(innerIpAddress),
            imageId, instanceTypeFamily, vlanId, networkInterfaces == null ?
            ImmutableMap.<String, List<NetworkInterface>>of() :
            ImmutableMap.copyOf(networkInterfaces), instanceId, eipAddress, internetMaxBandwidthIn, zoneId,
            internetChargeType, spotStrategy, stoppedMode, serialNumber, ioOptimized, memory, cpu, vpcAttributes,
            internetMaxBandwidthOut, deviceAvailable,
            securityGroupIds == null ? ImmutableMap.<String, List<String>>of() : ImmutableMap.copyOf(securityGroupIds),
            saleCycle, spotPriceLimit, autoReleaseTime, startTime, instanceName, description, resourceGroupId, osType,
            osName, instanceNetworkType,
            publicIpAddress == null ? ImmutableMap.<String, List<String>>of() : ImmutableMap.copyOf(publicIpAddress),
            hostname, instanceType, creationTime, status,
            tags == null ? ImmutableMap.<String, List<Tag>>of() : ImmutableMap.copyOf(tags), clusterId, recyclable,
            regionId, gpuSpec, dedicatedHostAttribute,
            operationLocks == null ? ImmutableMap.<String, List<String>>of() : ImmutableMap.copyOf(operationLocks),
            InstanceChargeType, gpuAmount, expiredTime);
   }

   public abstract Map<String, List<String>> innerIpAddress();

   public abstract String imageId();

   public abstract String instanceTypeFamily();

   public abstract String vlanId();

   @Nullable
   public abstract Map<String, List<NetworkInterface>> networkInterfaces();

   public abstract String instanceId();

   public abstract EipAddress eipAddress();

   public abstract Integer internetMaxBandwidthIn();

   public abstract String zoneId();

   public abstract String internetChargeType();

   public abstract String spotStrategy();

   public abstract String stoppedMode();

   public abstract String serialNumber();

   public abstract Boolean ioOptimized();

   public abstract Integer memory();

   public abstract Integer cpu();

   public abstract VpcAttributes vpcAttributes();

   public abstract Integer internetMaxBandwidthOut();

   public abstract Boolean deviceAvailable();

   public abstract Map<String, List<String>> securityGroupIds();

   public abstract String saleCycle();

   public abstract Double spotPriceLimit();

   public abstract String autoReleaseTime();

   public abstract Date startTime();

   public abstract String instanceName();

   public abstract String description();

   public abstract String resourceGroupId();

   public abstract String osType();

   public abstract String osName();

   public abstract String instanceNetworkType();

   public abstract Map<String, List<String>> publicIpAddress();

   public abstract String hostname();

   public abstract String instanceType();

   public abstract Date creationTime();

   public abstract Status status();

   public abstract Map<String, List<Tag>> tags();

   public abstract String clusterId();

   public abstract Boolean recyclable();

   public abstract String regionId();

   public abstract String gpuSpec();

   public abstract DedicatedHostAttribute dedicatedHostAttribute();

   public abstract Map<String, List<String>> operationLocks();

   public abstract String InstanceChargeType();

   public abstract Integer gpuAmount();

   public abstract Date expiredTime();

   public enum Status {
      STARTING, RUNNING, STOPPING, STOPPED;

      public static Status fromValue(String value) {
         Optional<Status> status = Enums.getIfPresent(Status.class, value.toUpperCase());
         checkArgument(status.isPresent(), "Expected one of %s but was %s", Joiner.on(',').join(Status.values()),
               value);
         return status.get();
      }
   }

}
