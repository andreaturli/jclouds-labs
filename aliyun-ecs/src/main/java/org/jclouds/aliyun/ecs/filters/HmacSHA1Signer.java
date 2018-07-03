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
package org.jclouds.aliyun.ecs.filters;

import com.google.common.base.Charsets;
import org.jclouds.domain.Credentials;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HmacSHA1Signer extends Signer {

   private static final String ALGORITHM_NAME = "HmacSHA1";

   @Override
   public String signString(String stringToSign, String accessKeySecret) {
      try {
         Mac mac = Mac.getInstance(ALGORITHM_NAME);
         mac.init(new SecretKeySpec(accessKeySecret.getBytes(Charsets.UTF_8), ALGORITHM_NAME));
         byte[] signData = mac.doFinal(stringToSign.getBytes(Charsets.UTF_8));
         return DatatypeConverter.printBase64Binary(signData);
      } catch (NoSuchAlgorithmException e) {
         throw new IllegalArgumentException(e.toString());
      } catch (InvalidKeyException e) {
         throw new IllegalArgumentException(e.toString());
      }
   }

   @Override
   public String signString(String stringToSign, Credentials credentials) {
      return signString(stringToSign, credentials);
   }

   @Override
   public String getSignerName() {
      return "HMAC-SHA1";
   }

   @Override
   public String getSignerVersion() {
      return "1.0";
   }

   @Override
   public String getSignerType() {
      return null;
   }

}
