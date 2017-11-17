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
package org.jclouds.fujitsu.k5.compute.uk.config;

import java.net.URI;
import java.util.Map;

import org.jclouds.Context;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.encryption.bouncycastle.config.BouncyCastleCryptoModule;
import org.jclouds.fujitsu.k5.compute.uk.compute.functions.CleanupK5Resources;
import org.jclouds.fujitsu.k5.compute.uk.compute.functions.CreateNeutronSecurityGroupIfNeeded;
import org.jclouds.fujitsu.k5.compute.uk.extensions.NeutronSecurityGroupExtension;
import org.jclouds.fujitsu.k5.compute.uk.strategy.K5CreateNodesWithGroupEncodedIntoNameThenAddToSet;
import org.jclouds.location.Provider;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.v2_0.config.AuthenticationApiModule;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneAuthenticationModule;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.nova.v2_0.compute.config.NovaComputeServiceContextModule;
import org.jclouds.openstack.nova.v2_0.compute.extensions.NovaSecurityGroupExtension;
import org.jclouds.openstack.nova.v2_0.compute.functions.CleanupResources;
import org.jclouds.openstack.nova.v2_0.compute.functions.CreateSecurityGroupIfNeeded;
import org.jclouds.openstack.nova.v2_0.compute.strategy.ApplyNovaTemplateOptionsCreateNodesWithGroupEncodedIntoNameThenAddToSet;
import org.jclouds.rest.ApiContext;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class K5UKComputeServiceContextModule extends NovaComputeServiceContextModule {

   @Override
   protected void configure() {
      super.configure();
      Module overrides = Modules
              .override(new AuthenticationApiModule(), new KeystoneAuthenticationModule(), new KeystoneAuthenticationModule.RegionModule())
              .with(new org.jclouds.openstack.keystone.v3.config.AuthenticationApiModule(), new org.jclouds.openstack.keystone.v3.config.KeystoneAuthenticationModule(), new org.jclouds.openstack.keystone.v3.config.KeystoneAuthenticationModule.RegionModule());

      install(overrides);

      bind(ApplyNovaTemplateOptionsCreateNodesWithGroupEncodedIntoNameThenAddToSet.class)
            .to(K5CreateNodesWithGroupEncodedIntoNameThenAddToSet.class);
      bind(NovaSecurityGroupExtension.class).to(NeutronSecurityGroupExtension.class);

      bind(CreateSecurityGroupIfNeeded.class).to(CreateNeutronSecurityGroupIfNeeded.class);

      bind(CleanupResources.class).to(CleanupK5Resources.class);
   }

   /**
    * CloudServers images are accessible via the root user, not ubuntu
    */
   @Override
   protected Map<OsFamily, LoginCredentials> osFamilyToCredentials(Injector injector) {
      return ImmutableMap.of(
              OsFamily.WINDOWS, LoginCredentials.builder().user("Administrator").build(),
              OsFamily.UBUNTU, LoginCredentials.builder().user("k5user").build(),
              OsFamily.CENTOS, LoginCredentials.builder().user("k5user").build()
      );
   }

   @Provides
   @Singleton
   protected Supplier<Context> provideNeutronApiSupplier(
           @Provider final Supplier<URI> endpoint, @Provider final Supplier<Credentials> creds) {
      return new Supplier<Context>() {
         @Override
         public Context get() {

            return ContextBuilder.newBuilder("fujitsu-k5-networking-uk") //
                    .endpoint(endpoint.get().toASCIIString())
                    .credentials(creds.get().identity, creds.get().credential)
                    .modules(ImmutableSet.<Module>of(
                            new SshjSshClientModule(),
                            new SLF4JLoggingModule(),
                            new BouncyCastleCryptoModule())
                    )
                    .build(new TypeToken<ApiContext<NeutronApi>>() {});

         }
      };
   }
}
