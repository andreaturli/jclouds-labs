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
package org.jclouds.docker.compute;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.Constants;
import org.jclouds.apis.BaseApiLiveTest;
import org.jclouds.xstream.XStreamApi;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.testng.annotations.Test;

import java.util.Properties;

@Test(groups = "live")
public class BaseXStreamApiLiveTest extends BaseApiLiveTest<XStreamApi> {

   public BaseXStreamApiLiveTest() {
      provider = "xstream";
   }

   @Override
   protected Iterable<Module> setupModules() {
      return ImmutableSet.<Module>of(getLoggingModule(), new SshjSshClientModule());
   }

   @Override
   protected Properties setupProperties() {
      Properties overrides = super.setupProperties();
      overrides.setProperty(Constants.PROPERTY_MAX_RETRIES, "15");
      overrides.setProperty("jclouds.ssh.retry-auth", "true");
      return overrides;
   }

}
