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

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import org.jclouds.domain.Credentials;
import org.jclouds.http.HttpException;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpRequestFilter;
import org.jclouds.location.Provider;
import org.jclouds.util.Strings2;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.http.Uris.uriBuilder;
import static org.jclouds.http.utils.Queries.queryParser;

@Singleton
public class FormSign implements HttpRequestFilter {

   private static final String SEPARATOR = "&";

   private final Supplier<Credentials> creds;

   @Inject
   FormSign(@Provider Supplier<Credentials> creds) {
      this.creds = creds;
   }

   public HttpRequest filter(HttpRequest request) throws HttpException {
      Credentials currentCreds = checkNotNull(creds.get(), "credential supplier returned null");

      Signer signer = Signer.getSigner();

      String signature;

      Multimap<String, String> decodedParams = queryParser().apply(request.getEndpoint().getRawQuery());

      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      df.setTimeZone(new SimpleTimeZone(0, "GMT"));

      String timestamp = df.format(new Date());
      String signatureNonce = UUID.randomUUID().toString();

      decodedParams.put("AccessKeyId", currentCreds.identity);
      decodedParams.put("Timestamp", timestamp);
      decodedParams.put("SignatureNonce", signatureNonce);

      String prefix;
      try {
         prefix = request.getMethod() + SEPARATOR + AcsURLEncoder.percentEncode("/") + SEPARATOR;
      } catch (UnsupportedEncodingException e) {
         throw Throwables.propagate(e);
      }

      // encode each parameter value first,
      String stringToSign = prefix;
      ImmutableSortedSet.Builder<String> builder = ImmutableSortedSet.naturalOrder();
      for (Map.Entry<String, String> entry : decodedParams.entries())
         builder.add(Strings2.urlEncode(entry.getKey()) + "=" + Strings2.urlEncode(entry.getValue()));
      stringToSign += Strings2.urlEncode(Joiner.on("&").join(builder.build()));

      signature = signer.signString(stringToSign, currentCreds.credential + "&");
      decodedParams.put("Signature", signature);

      request = request.toBuilder().endpoint(uriBuilder(request.getEndpoint()).query(decodedParams).build()).build();
      return request;
   }

}
