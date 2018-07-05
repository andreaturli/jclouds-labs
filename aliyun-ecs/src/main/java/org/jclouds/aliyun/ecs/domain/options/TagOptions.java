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
package org.jclouds.aliyun.ecs.domain.options;

import org.jclouds.http.options.BaseHttpRequestOptions;

public class TagOptions extends BaseHttpRequestOptions {

   public static final String TAG_1_KEY_PARAM = "Tag.1.Key";
   public static final String TAG_2_KEY_PARAM = "Tag.2.Key";
   public static final String TAG_3_KEY_PARAM = "Tag.3.Key";
   public static final String TAG_4_KEY_PARAM = "Tag.4.Key";
   public static final String TAG_5_KEY_PARAM = "Tag.5.Key";
   public static final String TAG_1_VALUE_PARAM = "Tag.1.Value";
   public static final String TAG_2_VALUE_PARAM = "Tag.2.Value";
   public static final String TAG_3_VALUE_PARAM = "Tag.3.Value";
   public static final String TAG_4_VALUE_PARAM = "Tag.4.Value";
   public static final String TAG_5_VALUE_PARAM = "Tag.5.Value";

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag1Key(String tag1Key) {
      queryParameters.put(TAG_1_KEY_PARAM, tag1Key);
      return this;
   }

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag1Value(String tag1Value) {
      queryParameters.put(TAG_1_VALUE_PARAM, tag1Value);
      return this;
   }

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag2Key(String tag2Key) {
      queryParameters.put(TAG_2_KEY_PARAM, tag2Key);
      return this;
   }

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag2Value(String tag2Value) {
      queryParameters.put(TAG_2_VALUE_PARAM, tag2Value);
      return this;
   }

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag3Key(String tag3Key) {
      queryParameters.put(TAG_3_KEY_PARAM, tag3Key);
      return this;
   }

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag3Value(String tag3Value) {
      queryParameters.put(TAG_3_VALUE_PARAM, tag3Value);
      return this;
   }

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag4Key(String tag4Key) {
      queryParameters.put(TAG_4_KEY_PARAM, tag4Key);
      return this;
   }

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag4Value(String tag4Value) {
      queryParameters.put(TAG_4_VALUE_PARAM, tag4Value);
      return this;
   }

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag5Key(String tag5Key) {
      queryParameters.put(TAG_5_KEY_PARAM, tag5Key);
      return this;
   }

   public org.jclouds.aliyun.ecs.domain.options.TagOptions tag5Value(String tag5Value) {
      queryParameters.put(TAG_5_VALUE_PARAM, tag5Value);
      return this;
   }

   public static class Builder {

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag1Key(String tag1Key) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag1Key(tag1Key);
      }

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag1Value(String tag1Value) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag1Value(tag1Value);
      }

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag2Key(String tag2Key) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag2Key(tag2Key);
      }

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag2Value(String tag2Value) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag2Value(tag2Value);
      }

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag3Key(String tag3Key) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag3Key(tag3Key);
      }

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag3Value(String tag3Value) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag3Value(tag3Value);
      }

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag4Key(String tag4Key) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag4Key(tag4Key);
      }

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag4Value(String tag4Value) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag4Value(tag4Value);
      }

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag5Key(String tag5Key) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag5Key(tag5Key);
      }

      public static org.jclouds.aliyun.ecs.domain.options.TagOptions tag5Value(String tag5Value) {
         return new org.jclouds.aliyun.ecs.domain.options.TagOptions().tag5Value(tag5Value);
      }

   }
}
