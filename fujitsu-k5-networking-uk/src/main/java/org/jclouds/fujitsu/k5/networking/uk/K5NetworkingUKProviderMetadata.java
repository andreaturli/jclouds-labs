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
package org.jclouds.fujitsu.k5.networking.uk;

import static org.jclouds.location.reference.LocationConstants.ISO3166_CODES;
import static org.jclouds.location.reference.LocationConstants.PROPERTY_REGION;
import static org.jclouds.location.reference.LocationConstants.PROPERTY_REGIONS;
import static org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties.CREDENTIAL_TYPE;

import java.net.URI;
import java.util.Properties;

import org.jclouds.fujitsu.k5.networking.uk.config.K5NetworkingContextModule;
import org.jclouds.fujitsu.k5.networking.uk.config.K5NetworkingParserModule;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.neutron.v2.NeutronApiMetadata;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.internal.BaseProviderMetadata;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

/**
 * Implementation of {@link ProviderMetadata} for Fujitsu K5 Networking Provider.
 */
@AutoService(ProviderMetadata.class)
public class K5NetworkingUKProviderMetadata extends BaseProviderMetadata {

   public static Builder builder() {
      return new Builder();
   }

   @Override
   public Builder toBuilder() {
      return builder().fromProviderMetadata(this);
   }

   public K5NetworkingUKProviderMetadata() {
      super(builder());
   }

   public K5NetworkingUKProviderMetadata(Builder builder) {
      super(builder);
   }

   public static Properties defaultProperties() {
      Properties properties = new Properties();
      properties.setProperty(CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);
      properties.setProperty(PROPERTY_REGIONS, "uk-1");
      properties.setProperty(PROPERTY_REGION + ".uk-1." + ISO3166_CODES, "GB-SLG");
      return properties;
   }

   public static class Builder extends BaseProviderMetadata.Builder {

      protected Builder() {
         id("fujitsu-k5-networking-uk")
         .name("Fujitsu K5 Networkling provider UK")
         .apiMetadata(new NeutronApiMetadata().toBuilder()
                  .identityName("${userName}")
                  .credentialName("${apiKey}")
                  .version("3")
                  .defaultEndpoint("https://identity.uk-1.cloud.global.fujitsu.com/v3")
                  .endpointName("identity service url ending in /v3")
                  .documentation(URI.create("https://k5-doc.jp-east-1.paas.cloud.global.fujitsu.com/doc/en/service_doc.html"))
                  .defaultModules(ImmutableSet.<Class<? extends Module>>builder()
                          .add(K5NetworkingParserModule.class)
                          .add(K5NetworkingContextModule.class)
                                              .build())
                  .build())
         .homepage(URI.create("https://s-portal.cloud.global.fujitsu.com"))
         .console(URI.create("https://dashboard.cloud.global.fujitsu.com/"))
         .iso3166Codes("GB-SLG")
         .endpoint("https://identity.uk-1.cloud.global.fujitsu.com/v3")
         .defaultProperties(K5NetworkingUKProviderMetadata.defaultProperties());
      }

      @Override
      public K5NetworkingUKProviderMetadata build() {
         return new K5NetworkingUKProviderMetadata(this);
      }

      @Override
      public Builder fromProviderMetadata(ProviderMetadata in) {
         super.fromProviderMetadata(in);
         return this;
      }
   }
}
