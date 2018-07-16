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
package org.jclouds.aliyun.ecs.compute.strategy;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.jclouds.Constants;
import org.jclouds.aliyun.ecs.ECSComputeServiceApi;
import org.jclouds.aliyun.ecs.domain.IpProtocol;
import org.jclouds.aliyun.ecs.domain.KeyPair;
import org.jclouds.aliyun.ecs.domain.KeyPairRequest;
import org.jclouds.aliyun.ecs.domain.Regions;
import org.jclouds.aliyun.ecs.domain.SecurityGroup;
import org.jclouds.aliyun.ecs.domain.SecurityGroupRequest;
import org.jclouds.aliyun.ecs.domain.VPC;
import org.jclouds.aliyun.ecs.domain.VPCRequest;
import org.jclouds.aliyun.ecs.domain.VSwitchRequest;
import org.jclouds.aliyun.ecs.domain.options.CreateSecurityGroupOptions;
import org.jclouds.aliyun.ecs.domain.options.CreateVPCOptions;
import org.jclouds.aliyun.ecs.domain.options.DeleteKeyPairOptions;
import org.jclouds.aliyun.ecs.domain.options.ListKeyPairsOptions;
import org.jclouds.aliyun.ecs.compute.options.ECSServiceTemplateOptions;
import org.jclouds.aliyun.ecs.domain.options.TagOptions;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.compute.config.CustomizationResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.functions.GroupNamingConvention;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.compute.strategy.CreateNodeWithGroupEncodedIntoName;
import org.jclouds.compute.strategy.CustomizeNodeAndAddToGoodMapOrPutExceptionIntoBadMap;
import org.jclouds.compute.strategy.ListNodesStrategy;
import org.jclouds.compute.strategy.impl.CreateNodesWithGroupEncodedIntoNameThenAddToSet;
import org.jclouds.domain.Location;
import org.jclouds.logging.Logger;
import org.jclouds.ssh.SshKeyPairGenerator;
import org.jclouds.ssh.SshKeys;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;
import static org.jclouds.compute.util.ComputeServiceUtils.getPortRangesFromList;

@Singleton
public class CreateResourcesThenCreateNodes extends CreateNodesWithGroupEncodedIntoNameThenAddToSet {

   public static final String INTERNET = "0.0.0.0/0";
   public static final String JCLOUDS_KEYPAIR_IMPORTED = "jclouds-imported";

   private final ECSComputeServiceApi api;
   private final SshKeyPairGenerator keyGenerator;

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   @Inject
   protected CreateResourcesThenCreateNodes(CreateNodeWithGroupEncodedIntoName addNodeWithGroupStrategy,
                                          ListNodesStrategy listNodesStrategy, GroupNamingConvention.Factory namingConvention,
                                          @Named(Constants.PROPERTY_USER_THREADS) ListeningExecutorService userExecutor,
                                          CustomizeNodeAndAddToGoodMapOrPutExceptionIntoBadMap.Factory customizeNodeAndAddToGoodMapOrPutExceptionIntoBadMapFactory,
                                          ECSComputeServiceApi api, SshKeyPairGenerator keyGenerator) {
      super(addNodeWithGroupStrategy, listNodesStrategy, namingConvention, userExecutor,
            customizeNodeAndAddToGoodMapOrPutExceptionIntoBadMapFactory);
      this.api = api;
      this.keyGenerator = keyGenerator;
   }

   @Override
   public Map<?, ListenableFuture<Void>> execute(String group, int count, Template template,
                                                 Set<NodeMetadata> goodNodes, Map<NodeMetadata, Exception> badNodes,
                                                 Multimap<NodeMetadata, CustomizationResponse> customizationResponses) {

      final String regionId = template.getLocation().getId();
      ECSServiceTemplateOptions options = template.getOptions().as(ECSServiceTemplateOptions.class);

      // If keys haven't been configured, generate a key pair
      if (Strings.isNullOrEmpty(options.getPublicKey()) &&
          Strings.isNullOrEmpty(options.getLoginPrivateKey())) {
         KeyPairRequest keyPairRequest = generateKeyPair(regionId, group);
         options.keyPairName(keyPairRequest.getKeyPairName());
         options.overrideLoginPrivateKey(keyPairRequest.getPrivateKeyBody());
      }

      // If there is a script to run in the node, make sure a private key has
      // been configured so jclouds will be able to access the node
      if (options.getRunScript() != null && Strings.isNullOrEmpty(options.getLoginPrivateKey())) {
         logger.warn(">> A runScript has been configured but no SSH key has been provided. Authentication will delegate to the ssh-agent");
      }

      // If there is a public key configured, then make sure there is a key pair for it
      if (!Strings.isNullOrEmpty(options.getPublicKey())) {
         KeyPair keyPair = getOrImportKeyPairForPublicKey(options, regionId);
         options.keyPairName(keyPair.keyPairName());
      }

      String securityGroupId = getOrCreateSecurityGroupForOptions(group, template.getLocation(), options);
      if (securityGroupId != null) options.securityGroups(securityGroupId);

      Map<?, ListenableFuture<Void>> responses = super.execute(group, count, template, goodNodes, badNodes, customizationResponses);

      // Key pairs are only required to create the devices.
      // Better to delete the auto-generated key pairs when they are mo more required
      registerAutoGeneratedKeyPairCleanupCallbacks(responses, regionId, options.getKeyPairName());

      return responses;
   }

   private KeyPair getOrImportKeyPairForPublicKey(ECSServiceTemplateOptions options, String regionId) {
      logger.debug(">> checking if the key pair already exists...");
      PublicKey userKey = readPublicKey(options.getPublicKey());
      final String fingerprint = computeFingerprint(userKey);
      KeyPair keyPair;

      synchronized (CreateResourcesThenCreateNodes.class) {
         Optional<KeyPair> keyPairOptional = Iterables
               .tryFind(api.sshKeyPairApi().list(regionId).concat(), new Predicate<KeyPair>() {
                  @Override
                  public boolean apply(KeyPair input) {
                     return input.keyPairFingerPrint().equals(fingerprint.replace(":", ""));
                  }
               });
         if (!keyPairOptional.isPresent()) {
            logger.debug(">> key pair not found. Importing a new key pair %s ...", fingerprint);
            keyPair = api.sshKeyPairApi().importKeyPair(
                    regionId,
                    options.getPublicKey(),
                    namingConvention.create().uniqueNameForGroup(JCLOUDS_KEYPAIR_IMPORTED));
            logger.debug(">> key pair imported! %s", keyPair);
         } else {
            logger.debug(">> key pair found for key %s", fingerprint);
            keyPair = keyPairOptional.get();
         }
         return keyPair;
      }
   }

   private KeyPairRequest generateKeyPair(String regionId, String prefix) {
      logger.debug(">> creating default keypair for node...");
      KeyPairRequest keyPairRequest = api.sshKeyPairApi().create(regionId, namingConvention.create().uniqueNameForGroup(prefix));
      logger.debug(">> keypair created! %s", keyPairRequest);
      return keyPairRequest;
   }

   private String getOrCreateSecurityGroupForOptions(String group, Location location,
                                                 ECSServiceTemplateOptions options) {

      checkArgument(options.getGroups().size() <= 1,
              "Only one security group can be configured for each network interface");

      String securityGroupId = null;
      if (!options.getGroups().isEmpty()) {
         Iterable<String> securityGroupNames = api.securityGroupApi().list(location.getId()).concat().transform(new Function<SecurityGroup, String>() {
            @Override
            public String apply(SecurityGroup input) {
               return input.securityGroupName();
            }
         });
         for (String securityGroupName : options.getGroups()) {
            checkState(Iterables.contains(securityGroupNames, securityGroupName), "Cannot find security group with name " + securityGroupName + ". \nSecurity groups available are: \n" + Iterables.toString(securityGroupNames)); // {
         }
      } else if (options.getInboundPorts().length > 0) {
         String name = namingConvention.create().sharedNameForGroup(group);
         SecurityGroupRequest securityGroupRequest = api.securityGroupApi().create(location.getId(),
                 CreateSecurityGroupOptions.Builder.securityGroupName(name).vpcId(options.getVpcId()));
         // add rules
         Map<Integer, Integer> portRanges = getPortRangesFromList(options.getInboundPorts());
         for (Map.Entry<Integer, Integer> portRange : portRanges.entrySet()) {
            String range = portRange.getKey() + "/" + portRange.getValue();
            // TODO makes protocol and source CIDR configurable?
            api.securityGroupApi().addInboundRule(
                    location.getId(),
                    securityGroupRequest.getSecurityGroupId(),
                    IpProtocol.TCP,
                    range,
                    INTERNET);
         }
         api.tagApi().add(location.getId(), securityGroupRequest.getSecurityGroupId(), "securitygroup", TagOptions.Builder.tag1Key("owner").tag1Value("jclouds"));
         securityGroupId = securityGroupRequest.getSecurityGroupId();
      }
      return securityGroupId;
   }

   private void registerAutoGeneratedKeyPairCleanupCallbacks(Map<?, ListenableFuture<Void>> responses,
         final String regionId, final String keyPairName) {
      // The Futures.allAsList fails immediately if some of the futures fail.
      // The Futures.successfulAsList, however,
      // returns a list containing the results or 'null' for those futures that
      // failed. We want to wait for all them
      // (even if they fail), so better use the latter form.
      ListenableFuture<List<Void>> aggregatedResponses = Futures.successfulAsList(responses.values());

      // Key pairs must be cleaned up after all futures completed (even if some
      // failed).
      Futures.addCallback(aggregatedResponses, new FutureCallback<List<Void>>() {
         @Override
         public void onSuccess(List<Void> result) {
            cleanupAutoGeneratedKeyPairs(keyPairName);
         }

         @Override
         public void onFailure(Throwable t) {
            cleanupAutoGeneratedKeyPairs(keyPairName);
         }

         private void cleanupAutoGeneratedKeyPairs(String keyPairName) {
            logger.debug(">> cleaning up auto-generated key pairs...");
            try {
               api.sshKeyPairApi().delete(regionId, DeleteKeyPairOptions.Builder.keyPairNames(keyPairName));
            } catch (Exception ex) {
               logger.warn(">> could not delete key pair %s: %s", keyPairName, ex.getMessage());
            }
         }
      }, userExecutor);
   }


   private static PublicKey readPublicKey(String publicKey) {
      Iterable<String> parts = Splitter.on(' ').split(publicKey);
      checkArgument(size(parts) >= 2, "bad format, should be: ssh-rsa AAAAB3...");
      String type = get(parts, 0);

      try {
         if ("ssh-rsa".equals(type)) {
            RSAPublicKeySpec spec = SshKeys.publicKeySpecFromOpenSSH(publicKey);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
         } else {
            throw new IllegalArgumentException("bad format, ssh-rsa is only supported");
         }
      } catch (InvalidKeySpecException ex) {
         throw new RuntimeException(ex);
      } catch (NoSuchAlgorithmException ex) {
         throw new RuntimeException(ex);
      }
   }

   private static String computeFingerprint(PublicKey key) {
      if (key instanceof RSAPublicKey) {
         RSAPublicKey rsaKey = (RSAPublicKey) key;
         return SshKeys.fingerprint(rsaKey.getPublicExponent(), rsaKey.getModulus());
      } else {
         throw new IllegalArgumentException("Only RSA keys are supported");
      }
   }
}
