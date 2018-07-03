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
package org.jclouds.aliyun.ecs.compute.options;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.jclouds.compute.options.TemplateOptions;

import static com.google.common.base.Objects.equal;

/**
 * Custom options for the Alibaba Elastic Compute Service API.
 */
public class ECSServiceTemplateOptions extends TemplateOptions implements Cloneable {

   private String keyPairName = "";
   private String vpcId = "";
   private String vSwitchId = "";
   private String userData = "";

   public ECSServiceTemplateOptions keyPairName(String keyPairName) {
      this.keyPairName = keyPairName;
      return this;
   }

   public ECSServiceTemplateOptions vpcId(String vpcId) {
      this.vpcId = vpcId;
      return this;
   }

   public ECSServiceTemplateOptions vSwitchId(String vSwitchId) {
      this.vSwitchId = vSwitchId;
      return this;
   }

   public ECSServiceTemplateOptions userData(String userData) {
      this.userData = userData;
      return this;
   }

   public String getKeyPairName() {
      return keyPairName;
   }

   public String getVpcId() {
      return vpcId;
   }

   public String getVSwitchId() {
      return vSwitchId;
   }

   public String getUserData() {
      return userData;
   }

   @Override
   public ECSServiceTemplateOptions clone() {
      ECSServiceTemplateOptions options = new ECSServiceTemplateOptions();
      copyTo(options);
      return options;
   }

   @Override
   public void copyTo(TemplateOptions to) {
      super.copyTo(to);
      if (to instanceof ECSServiceTemplateOptions) {
         ECSServiceTemplateOptions eTo = ECSServiceTemplateOptions.class.cast(to);
         eTo.keyPairName(keyPairName);
         eTo.vpcId(vpcId);
         eTo.vSwitchId(vSwitchId);
         eTo.userData(userData);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(super.hashCode(), keyPairName, vpcId, vSwitchId, userData);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (!super.equals(obj)) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      ECSServiceTemplateOptions other = (ECSServiceTemplateOptions) obj;
      return super.equals(other) && equal(this.keyPairName, other.keyPairName) && equal(this.vpcId, other.vpcId)
            && equal(this.vSwitchId, other.vSwitchId) && equal(this.userData, other.userData);
   }

   @Override
   public MoreObjects.ToStringHelper string() {
      MoreObjects.ToStringHelper toString = super.string().omitNullValues();
      toString.add("keyPairName", keyPairName);
      toString.add("vpcId", vpcId);
      toString.add("vSwitchId", vSwitchId);
      toString.add("userData", userData);
      return toString;
   }

   public static class Builder {

      /**
       * @see ECSServiceTemplateOptions#keyPairName
       */
      public static ECSServiceTemplateOptions keyPairName(String keyPairName) {
         ECSServiceTemplateOptions options = new ECSServiceTemplateOptions();
         return options.keyPairName(keyPairName);
      }

      /**
       * @see ECSServiceTemplateOptions#vpcId
       */
      public static ECSServiceTemplateOptions vpcId(String vpcId) {
         ECSServiceTemplateOptions options = new ECSServiceTemplateOptions();
         return options.vpcId(vpcId);
      }

      /**
       * @see ECSServiceTemplateOptions#vSwitchId
       */
      public static ECSServiceTemplateOptions vSwitchId(String vSwitchId) {
         ECSServiceTemplateOptions options = new ECSServiceTemplateOptions();
         return options.vSwitchId(vSwitchId);
      }

      /**
       * @see ECSServiceTemplateOptions#userData
       */
      public static ECSServiceTemplateOptions userData(String userData) {
         ECSServiceTemplateOptions options = new ECSServiceTemplateOptions();
         return options.userData(userData);
      }

   }
}
