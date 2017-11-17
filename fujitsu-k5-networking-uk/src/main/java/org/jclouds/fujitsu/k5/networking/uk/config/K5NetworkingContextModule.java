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
package org.jclouds.fujitsu.k5.networking.uk.config;

import org.jclouds.openstack.keystone.v2_0.config.AuthenticationApiModule;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneAuthenticationModule;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Configures the Neutron connection for Fujitsu K5.
 *
 */
public class K5NetworkingContextModule extends AbstractModule {

   @Override
   protected void configure() {
      Module overrides = Modules
            .override(
                    new AuthenticationApiModule(),
                    new KeystoneAuthenticationModule(),
                    new KeystoneAuthenticationModule.RegionModule()
            ).with(
                    new org.jclouds.openstack.keystone.v3.config.AuthenticationApiModule(),
                    new org.jclouds.openstack.keystone.v3.config.KeystoneAuthenticationModule(),
                    new org.jclouds.openstack.keystone.v3.config.KeystoneAuthenticationModule.RegionModule()
              );

      install(overrides);
   }

}
