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

import org.jclouds.javax.annotation.Nullable;
import org.jclouds.json.SerializedNames;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class VaultProperties implements Provisionable {

   @Nullable
   public abstract String tenantId();
   
   @Nullable
   public abstract String vaultUri();
   
   @Nullable
   public abstract Boolean enabledForDeployment();

   @Nullable
   public abstract Boolean enabledForDiskEncryption();

   @Nullable
   public abstract Boolean enabledForTemplateDeployment();

   @Nullable
   public abstract Boolean enableSoftDelete();
   
   @Nullable
   public abstract String createMode();
   
   @Nullable
   public abstract SKU sku();

   @Nullable
   public abstract AccessPolicyEntry accessPolicies();

   @SerializedNames({"tenantId", "vaultUri", "diskState", "diskSizeGB", "lun", "vhd", "creationData"})
   public static VaultProperties create(final String tenantId, final String vaultUri, final String diskState, final Integer diskSizeGB, final Integer lun, final VHD vhd, final CreationData creationData) {
      return builder()
              .tenantId(tenantId)
              .vaultUri(vaultUri)
              .diskState(diskState)
              .diskSizeGB(diskSizeGB)
              .lun(lun)
              .vhd(vhd)
              .creationData(creationData)
              .build();
   }

   public abstract Builder toBuilder();

   public static Builder builder() {
      return new AutoValue_VaultProperties.Builder();
   }

   @AutoValue.Builder
   public abstract static class Builder {
      public abstract Builder tenantId(String tenantId);
      public abstract Builder vaultUri(String vaultUri);
      public abstract Builder diskState(String diskState);
      public abstract Builder diskSizeGB(Integer diskSizeGB);
      public abstract Builder lun(Integer lun);
      public abstract Builder vhd(VHD vhd);
      public abstract Builder creationData(CreationData creationData);
      public abstract VaultProperties build();

   }
}
