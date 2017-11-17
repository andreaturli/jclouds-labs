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
package org.jclouds.fujitsu.k5.compute.uk.compute;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Properties;

import org.jclouds.Context;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.internal.BaseComputeServiceLiveTest;
import org.jclouds.config.ContextLinking;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.BlockDeviceMapping;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.RegionAndId;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Module;

@Test(groups = "live", singleThreaded = true, testName = "K5UKComputeServiceLiveTest")
public class K5UKComputeServiceLiveTest extends BaseComputeServiceLiveTest {

   private Context neutronApiContext;

   public K5UKComputeServiceLiveTest() {
      provider = "fujitsu-k5-compute-uk";
      Properties overrides = setupProperties();

      // neutronApiContext = ContextBuilder.newBuilder("fujitsu-k5-networking-uk") //
      // //.endpoint(setIfTestSystemPropertyPresent(overrides,
      // "openstack-neutron.endpoint"))
      // .credentials(setIfTestSystemPropertyPresent(overrides,
      // "openstack-neutron.identity"),
      // setIfTestSystemPropertyPresent(overrides, "openstack-neutron.credential"))
      // .modules(ImmutableSet.<Module>of(
      // new SshjSshClientModule(),
      // new SLF4JLoggingModule(),
      // new BouncyCastleCryptoModule())
      // )
      // .build(new TypeToken<ApiContext<NeutronApi>>() {});
   }

   @Override
   @Test(expectedExceptions = AuthorizationException.class)
   public void testCorrectAuthException() throws Exception {
      ComputeServiceContext context = null;
      try {
         Properties overrides = setupProperties();
         overrides.setProperty(provider + ".identity", "MOM:MA");
         overrides.setProperty(provider + ".credential", "MIA");
         context = newBuilder()
               .modules(ImmutableSet.of(getLoggingModule(), credentialStoreModule,
                     ContextLinking.linkContext(neutronApiContext)))
               .overrides(overrides).build(ComputeServiceContext.class);
         context.getComputeService().listNodes();
      } catch (AuthorizationException e) {
         throw e;
      } catch (RuntimeException e) {
         e.printStackTrace();
         throw e;
      } finally {
         if (context != null)
            context.close();
      }
   }

   protected Template buildTemplate(TemplateBuilder templateBuilder) {

      template = templateBuilder.build();

      NovaTemplateOptions templateOptions = template.getOptions().as(NovaTemplateOptions.class);

      String network = getSystemProperty("network");
      if (network != null)
         templateOptions.networks(network); // devtest "4f0759ed-ed74-4bbf-9311-37b714a8c801"

      String floatingIpPool = getSystemProperty("floatingIpPool");
      if (floatingIpPool != null)
         templateOptions.floatingIpPoolNames(floatingIpPool); // "inf_az1_fip-pool02-1"

      templateOptions.autoAssignFloatingIp(true);

      BlockDeviceMapping blockDeviceMapping = BlockDeviceMapping.builder().bootIndex(0).deviceName("/dev/vda")
            .sourceType("image").destinationType("volume").volumeSize(30)
            .uuid(RegionAndId.fromSlashEncoded(template.getImage().getId()).getId()) // CentOS 7.3 64bit (English) 01
            .deleteOnTermination(true).build();
      templateOptions.blockDeviceMappings(blockDeviceMapping);

      return template;
   }

   private String getSystemProperty(String propertyName) {
      String property = String.format("%s.%s.%s", "test", provider, propertyName);
      if (System.getProperties().containsKey(property)) {
         return System.getProperty(property);
      }
      return null;
   }

   @Override
   protected void checkTagsInNodeEquals(NodeMetadata node, ImmutableSet<String> tags) {
      checkState(Sets.intersection(node.getTags(), tags).size() == tags.size());
   }

   @Override
   protected Module getSshModule() {
      return new SshjSshClientModule();
   }

   @Override
   protected Iterable<Module> setupModules() {
      List<Module> modules = Lists.newArrayList(super.setupModules());
//      modules.add(ContextLinking.linkContext(neutronApiContext));
      return modules;
   }

}
