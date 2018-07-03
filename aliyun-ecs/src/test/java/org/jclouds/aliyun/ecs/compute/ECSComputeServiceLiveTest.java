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
package org.jclouds.aliyun.ecs.compute;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.internal.BaseComputeServiceLiveTest;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.Statements;
import org.jclouds.scriptbuilder.statements.java.InstallJDK;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.jclouds.aliyun.ecs.compute.options.ECSServiceTemplateOptions.Builder.vpcId;

/**
 * Live tests for the {@link org.jclouds.compute.ComputeService} integration.
 */
@Test(groups = "live", singleThreaded = true, testName = "ECSComputeServiceLiveTest")
public class ECSComputeServiceLiveTest extends BaseComputeServiceLiveTest {

   private String vpcId;
   private String vSwitchId;

   public ECSComputeServiceLiveTest() {
      provider = "aliyun-ecs";
   }

   @Override
   protected Properties setupProperties() {
      Properties properties = super.setupProperties();
      vpcId = setIfTestSystemPropertyPresent(properties,  provider + ".vpcId");
      vSwitchId = setIfTestSystemPropertyPresent(properties,  provider + ".vSwitchId");
      return properties;
   }

   @Override
   protected TemplateBuilder templateBuilder() {
      return super.templateBuilder()
                      .options(vpcId(vpcId)
                      .vSwitchId(vSwitchId));
   }

   @Override
   protected Template addRunScriptToTemplate(Template template) {
      template.getOptions().runScript(Statements.newStatementList(
            new Statement[] { AdminAccess.standard(), Statements.exec("sleep 50"), InstallJDK.fromOpenJDK() }));
      return template;
   }

   @Override
   protected Module getSshModule() {
      return new SshjSshClientModule();
   }

   @Override
   @Test(expectedExceptions = AuthorizationException.class)
   public void testCorrectAuthException() throws Exception {
      ComputeServiceContext context = null;
      try {
         Properties overrides = setupProperties();
         overrides.setProperty(provider + ".identity", "MOM:MA");
         overrides.setProperty(provider + ".credential", "MIA");
         context = newBuilder().modules(ImmutableSet.of(getLoggingModule(), credentialStoreModule)).overrides(overrides)
               .build(ComputeServiceContext.class);
         // replace listNodes with listImages as it doesn't require `projectId`
         context.getComputeService().listImages();
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

   @Override
   public void testOptionToNotBlock() throws Exception {
      // Aliyun ECS ComputeService implementation has to block until the node
      // is provisioned, to be able to return it.
   }

   @Override
   protected void checkUserMetadataContains(NodeMetadata node, ImmutableMap<String, String> userMetadata) {
      // The ECS API does not return the user data
   }

}
