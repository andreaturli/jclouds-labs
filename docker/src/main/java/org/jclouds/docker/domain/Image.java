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
package org.jclouds.docker.domain;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Image {

   @SerializedName("Id")
   private String id;
   @SerializedName("RepoTags")
   private List<String> repoTags;
   @SerializedName("Repository")
   private String repository;
   @SerializedName("Tag")
   private String tag;
   @SerializedName("Created")
   private long created;
   @SerializedName("Size")
   private final long size;
   @SerializedName("VirtualSize")
   private long virtualSize;

   public Image(String id, List<String> repoTags, String repository, String tag, long created, long size,
                long virtualSize) {
      this.id = id;
      this.repoTags = repoTags;
      this.repository = repository;
      this.tag = tag;
      this.created = created;
      this.size = size;
      this.virtualSize = virtualSize;
   }

   public String getId() {
      return id;
   }

   public List<String> getRepoTags() {
      return repoTags;
   }

   public String getRepository() {
      return repository;
   }

   public String getTag() {
      return tag;
   }

   public long getCreated() {
      return created;
   }

   public long getSize() {
      return size;
   }

   public long getVirtualSize() {
      return virtualSize;
   }

   @Override
   public String toString() {
      return "Image{" +
              "id='" + id + '\'' +
              ", repoTags=" + repoTags +
              ", repository='" + repository + '\'' +
              ", tag='" + tag + '\'' +
              ", created=" + created +
              ", size=" + size +
              ", virtualSize=" + virtualSize +
              '}';
   }

   public static Builder builder() {
      return new Builder();
   }

   public Builder toBuilder() {
      return builder().fromImage(this);
   }

   public static final class Builder {

      private String id;
      private List<String> repoTags;
      private String repository;
      private String tag;
      private long created;
      private long size;
      private long virtualSize;

      public Builder id(String id) {
         this.id = id;
         return this;
      }

      public Builder repoTags(List<String> repoTags) {
         this.repoTags = repoTags;
         return this;
      }

      public Builder repository(String repository) {
         this.repository = repository;
         return this;
      }

      public Builder tag(String tag) {
         this.tag = tag;
         return this;
      }

      public Builder created(long created) {
         this.created = created;
         return this;
      }

      public Builder size(long size) {
         this.size = size;
         return this;
      }

      public Builder virtualSize(long virtualSize) {
         this.virtualSize = virtualSize;
         return this;
      }

      public Image build() {
         return new Image(id, repoTags, repository, tag, created, size, virtualSize);
      }

      public Builder fromImage(Image in) {
         return this
                 .id(in.getId())
                 .repoTags(in.getRepoTags())
                 .created(in.getCreated())
                 .repository(in.getRepository())
                 .tag(in.getTag())
                 .created(in.getCreated())
                 .size(in.getSize())
                 .virtualSize(in.getVirtualSize());
      }
   }
}
