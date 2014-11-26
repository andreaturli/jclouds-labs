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
package org.jclouds.docker.features;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.jclouds.docker.DockerApi;
import org.jclouds.docker.domain.Config;
import org.jclouds.docker.domain.Container;
import org.jclouds.docker.domain.Resource;
import org.jclouds.docker.internal.BaseDockerMockTest;
import org.jclouds.docker.options.ListContainerOptions;
import org.jclouds.docker.parse.ContainerParseTest;
import org.jclouds.docker.parse.ContainersParseTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

/**
 * Mock tests for the {@link org.jclouds.docker.features.ContainerApi} class.
 */
@Test(groups = "unit", testName = "ContainerApiMockTest")
public class ContainerApiMockTest extends BaseDockerMockTest {

   public void testListContainers() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setBody(payloadFromResource("/containers.json")));

      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();

      try {
         assertEquals(api.listContainers(), new ContainersParseTest().expected());
         assertSent(server, "GET", "/containers/json");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testListNonexistentContainers() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(404));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         assertEquals(api.listContainers(), ImmutableList.of());
         assertSent(server, "GET", "/containers/json");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   @Test(timeOut = 10000l)
   public void testListAllContainers() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setBody(payloadFromResource("/containers.json")));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         assertEquals(api.listContainers(ListContainerOptions.Builder.all(true)), new ContainersParseTest().expected());
         assertSent(server, "GET", "/containers/json?all=true");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testGetContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setBody(payloadFromResource("/container.json")));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      String containerId = "b03d4cd15b76f8876110615cdeed15eadf77c9beb408d62f1687dcc69192cd6d";
      try {
         assertEquals(api.inspectContainer(containerId), new ContainerParseTest().expected());
         assertSent(server, "GET", "/containers/" + containerId + "/json");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testCreateContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setBody(payloadFromResource("/container-creation.json")));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      Config containerConfig = Config.builder().cmd(ImmutableList.of("date"))
              .attachStdin(false)
              .attachStderr(true)
              .attachStdout(true)
              .tty(false)
              .image("base")
              .build();
      try {
         Container container = api.createContainer("test", containerConfig);

         assertSent(server, "POST", "/containers/create?name=test");

         assertNotNull(container);
         assertEquals(container.id(), "c6c74153ae4b1d1633d68890a68d89c40aa5e284a1ea016cbc6ef0e634ee37b2");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testRemoveContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      String containerId = "6d35806c1bd2b25cd92bba2d2c2c5169dc2156f53ab45c2b62d76e2d2fee14a9";

      try {
         api.removeContainer(containerId);
         assertSent(server, "DELETE", "/containers/" + containerId);

      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testStartContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(200));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.startContainer("1");
         assertSent(server, "POST", "/containers/1/start");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testStopContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(200));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.stopContainer("1");
         assertSent(server, "POST", "/containers/1/stop");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }


   public void testCommitContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(201));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.commit();
         assertSent(server, "POST", "/commit");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testPauseContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.pause("1");
         assertSent(server, "POST", "/containers/1/pause");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testUnpauseContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.unpause("1");
         assertSent(server, "POST", "/containers/1/unpause");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testAttachContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(200));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.attach("1");
         assertSent(server, "POST", "/containers/1/attach");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testWaitContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(200));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.wait("1");
         assertSent(server, "POST", "/containers/1/wait");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testRestartContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.restart("1");
         assertSent(server, "POST", "/containers/1/restart");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testKillContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.kill("1");
         assertSent(server, "POST", "/containers/1/kill");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

   public void testCopyFileFromContainer() throws Exception {
      MockWebServer server = mockDockerWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));
      DockerApi dockerApi = api(server.getUrl("/"));
      ContainerApi api = dockerApi.getContainerApi();
      try {
         api.copy("1", Resource.create("test"));
         assertSent(server, "POST", "/containers/1/copy");
      } finally {
         dockerApi.close();
         server.shutdown();
      }
   }

}
