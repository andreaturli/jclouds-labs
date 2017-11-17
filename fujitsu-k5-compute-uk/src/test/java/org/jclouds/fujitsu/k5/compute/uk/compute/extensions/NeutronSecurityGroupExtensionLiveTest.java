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
package org.jclouds.fujitsu.k5.compute.uk.compute.extensions;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.inject.Module;
import org.jclouds.Context;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.SecurityGroup;
import org.jclouds.compute.extensions.SecurityGroupExtension;
import org.jclouds.compute.extensions.internal.BaseSecurityGroupExtensionLiveTest;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.config.ContextLinking;
import org.jclouds.encryption.bouncycastle.config.BouncyCastleCryptoModule;
import org.jclouds.logging.Logger;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.v3.config.KeystoneV3Module;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.rest.ApiContext;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.testng.annotations.Test;

import javax.annotation.Resource;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Test(groups = "live", singleThreaded = true, testName = "NeutronSecurityGroupExtensionLiveTest")
public class NeutronSecurityGroupExtensionLiveTest extends BaseSecurityGroupExtensionLiveTest {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.CONSOLE;

   private Context neutronApiContext;

   public NeutronSecurityGroupExtensionLiveTest() {
      provider = "fujitsu-k5-uk";

      Properties overrides = setupProperties();

      neutronApiContext = ContextBuilder.newBuilder("openstack-neutron") //
              .endpoint(setIfTestSystemPropertyPresent(overrides,  "openstack-neutron.endpoint"))
              .credentials(setIfTestSystemPropertyPresent(overrides,  "openstack-neutron.identity"), setIfTestSystemPropertyPresent(overrides,  "openstack-neutron.credential"))
              .modules(ImmutableSet.<Module>of(
                      new SshjSshClientModule(),
                      new SLF4JLoggingModule(),
                      new BouncyCastleCryptoModule(),
                      new KeystoneV3Module())
              )
              .build(new TypeToken<ApiContext<NeutronApi>>() {});
   }


   @Override
   protected Iterable<Module> setupModules() {
      List<Module> modules = Lists.newArrayList(super.setupModules());
      modules.add(ContextLinking.linkContext(neutronApiContext));
      return modules;
   }

   @Test(groups = {"integration", "live"}, singleThreaded = true)
   public void testListSecurityGroups() throws Exception {
      skipIfSecurityGroupsNotSupported();

      final long begin = new Date().getTime();
      ComputeService computeService = view.getComputeService();
      Optional<SecurityGroupExtension> securityGroupExtension = computeService.getSecurityGroupExtension();
      assertTrue(securityGroupExtension.isPresent(), "security extension was not present");

      logger.info("Loading security groups");
      final SecurityGroupExtension security = securityGroupExtension.get();
      Set<SecurityGroup> beforeAdd = security.listSecurityGroups();
      int countBeforeAdd = beforeAdd.size();
      logger.info("Found %d security groups", countBeforeAdd);

      String someUnlikelyName = String.valueOf(new Random().nextInt(1000000) + 1000000);
      logger.info("Adding security group %s", someUnlikelyName);
      final SecurityGroup testGroup = security.createSecurityGroup(someUnlikelyName, getNodeTemplate().getLocation());

      try {
         verifyAndDeleteSecurityGroup(security, countBeforeAdd, testGroup);
      } catch (Exception e) {
         logger.error(e, "Exception caught, live test leaking security group %s", testGroup.getName());
         throw e;
      }

      final long end = new Date().getTime();

      assertTrue(end - begin < TimeUnit.MINUTES.toMillis(5)); // see https://issues.apache.org/jira/browse/JCLOUDS-1235

   }

   private void verifyAndDeleteSecurityGroup(SecurityGroupExtension security, int countBeforeAdd,
                                             final SecurityGroup testGroup) {
      logger.info("Loading security groups");
      Set<SecurityGroup> afterAdd = security.listSecurityGroups();
      final int countAfterAdd = afterAdd.size();
      logger.info("Found %d security groups", countAfterAdd);

      assertEquals(countAfterAdd, countBeforeAdd + 1);
      final Predicate<SecurityGroup> findTestGroup = new Predicate<SecurityGroup>() {
         @Override
         public boolean apply(SecurityGroup input) {
            return input.getName().equals(testGroup.getName());
         }
      };
      final SecurityGroup created = Iterables.find(afterAdd, findTestGroup);
      assertNotNull(created, "Did not find security group created as expected");

      logger.info("Removing %s", testGroup.getName());
      security.removeSecurityGroup(testGroup.getId());

      logger.info("Loading security groups");
      Set<SecurityGroup> afterRemove = security.listSecurityGroups();
      final int sizeAfterRemove = afterRemove.size();
      logger.info("Found %d security groups", sizeAfterRemove);
      assertEquals(sizeAfterRemove, countBeforeAdd);
      final Optional<SecurityGroup> removed = Iterables.tryFind(afterRemove, findTestGroup);
      assertTrue(!removed.isPresent(), "Did not remove test security group as expected");
   }
}
