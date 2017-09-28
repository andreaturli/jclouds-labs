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
package org.jclouds.azurecompute.arm.features;

import java.net.URI;

import org.jclouds.azurecompute.arm.domain.SKU;
import org.jclouds.azurecompute.arm.domain.Vault;
import org.jclouds.azurecompute.arm.domain.VaultProperties;
import org.jclouds.azurecompute.arm.internal.BaseAzureComputeApiLiveTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Test(groups = "live", testName = "VaultApiLiveTest")
public class VaultApiLiveTest extends BaseAzureComputeApiLiveTest {

   private String subscriptionid;
   private String vaultName;
   private URI vaultUri = null;
   
   @BeforeClass
   @Override
   public void setup() {
      super.setup();
      createTestResourceGroup();
      vaultName = String.format("kv-%s", this.getClass().getSimpleName().toLowerCase());
   }

   @Test
   public void testCreate() {
      Vault vault = api().createOrUpdateVault(vaultName, LOCATION, VaultProperties.builder()
              .tenantId("ba85e8cd-8c83-486e-a7e3-0d7666169d34")
              .sku(SKU.create(LOCATION, "standard", null, "A"))
              .accessPolicies(ImmutableList.of(VaultProperties.AccessPolicyEntry.create(null, "b025a8c0-d7fa-42fd-8e62-d988a3f12791", "ba85e8cd-8c83-486e-a7e3-0d7666169d34",
                      VaultProperties.Permissions.create(
                              ImmutableList.of( // certificates
                                      "Get",
                                      "List",
                                      "Update",
                                      "Create",
                                      "Import",
                                      "Delete",
                                      "ManageContacts",
                                      "ManageIssuers",
                                      "GetIssuers",
                                      "ListIssuers",
                                      "SetIssuers",
                                      "DeleteIssuers"
                              ),
                              ImmutableList.of( // keys
                                      "Get",
                                      "List",
                                      "Update",
                                      "Create",
                                      "Import",
                                      "Delete",
                                      "Recover",
                                      "Backup",
                                      "Restore"
                              ),
                              ImmutableList.of( // secrets
                                      "Get",
                                      "List",
                                      "Set",
                                      "Delete",
                                      "Recover",
                                      "Backup",
                                      "Restore"
                              ),
                              ImmutableList.<String>of()
                      ))))
              .build());
      vaultUri = vault.properties().vaultUri();
      assertTrue(!vault.name().isEmpty());
   }

   @Test(dependsOnMethods = "testCreate")
   public void testGet() {
      Vault vaultFound = api().getVault(vaultName);
      assertTrue(!vaultFound.name().isEmpty());
   }

   @Test(dependsOnMethods = "testGet")
   public void testListKeys() {
      api().listKeys(vaultUri);
   }

   @Test(dependsOnMethods = "testListKeys")
   public void testCreateKey() {
      api().createKey(vaultUri, "myKey");
   }

   @Test(dependsOnMethods = "testCreateKey")
   public void testList() {
      for (Vault vault : api().listVaults()) {
         assertNotNull(vault.name());
      }
   }

   @Test(dependsOnMethods = "testList")
   public void testDelete() {
      api().deleteVault(vaultName);
   }
   
   private VaultApi api() {
      return api.getVaultApi(resourceGroupName);
   }
}
