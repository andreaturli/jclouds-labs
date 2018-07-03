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

import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.BaseEncoding.base64;

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

   public TagOptions tag1Key(String tag1Key) {
      queryParameters.put(TAG_1_KEY_PARAM, encodeTag(tag1Key));
      return this;
   }

   public TagOptions tag1Value(String tag1Value) {
      queryParameters.put(TAG_1_VALUE_PARAM, encodeTag(tag1Value));
      return this;
   }

   public TagOptions tag2Key(String tag2Key) {
      queryParameters.put(TAG_2_KEY_PARAM, encodeTag(tag2Key));
      return this;
   }

   public TagOptions tag2Value(String tag2Value) {
      queryParameters.put(TAG_2_VALUE_PARAM, encodeTag(tag2Value));
      return this;
   }

   public TagOptions tag3Key(String tag3Key) {
      queryParameters.put(TAG_3_KEY_PARAM, encodeTag(tag3Key));
      return this;
   }

   public TagOptions tag3Value(String tag3Value) {
      queryParameters.put(TAG_3_VALUE_PARAM, encodeTag(tag3Value));
      return this;
   }

   public TagOptions tag4Key(String tag4Key) {
      queryParameters.put(TAG_4_KEY_PARAM, encodeTag(tag4Key));
      return this;
   }

   public TagOptions tag4Value(String tag4Value) {
      queryParameters.put(TAG_4_VALUE_PARAM, encodeTag(tag4Value));
      return this;
   }

   public TagOptions tag5Key(String tag5Key) {
      queryParameters.put(TAG_5_KEY_PARAM, encodeTag(tag5Key));
      return this;
   }

   public TagOptions tag5Value(String tag5Value) {
      queryParameters.put(TAG_5_VALUE_PARAM, encodeTag(tag5Value));
      return this;
   }

   public TagOptions keys(Set<String> keys) {
      int i = 1;
      String keyTemplate = "Tag.%d.Key";
      String valueTemplate = "Tag.%d.Value";

      for (String key : keys) {
         queryParameters.put(String.format(keyTemplate, i), encodeTag(key));
         queryParameters.put(String.format(valueTemplate, i), "");
         i++;
      }
      return this;
   }

   public static class Builder {

      public static TagOptions tag1Key(String tag1Key) {
         return new TagOptions().tag1Key(tag1Key);
      }

      public static TagOptions tag1Value(String tag1Value) {
         return new TagOptions().tag1Value(tag1Value);
      }

      public static TagOptions tag2Key(String tag2Key) {
         return new TagOptions().tag2Key(tag2Key);
      }

      public static TagOptions tag2Value(String tag2Value) {
         return new TagOptions().tag2Value(tag2Value);
      }

      public static TagOptions tag3Key(String tag3Key) {
         return new TagOptions().tag3Key(tag3Key);
      }

      public static TagOptions tag3Value(String tag3Value) {
         return new TagOptions().tag3Value(tag3Value);
      }

      public static TagOptions tag4Key(String tag4Key) {
         return new TagOptions().tag4Key(tag4Key);
      }

      public static TagOptions tag4Value(String tag4Value) {
         return new TagOptions().tag4Value(tag4Value);
      }

      public static TagOptions tag5Key(String tag5Key) {
         return new TagOptions().tag5Key(tag5Key);
      }

      public static TagOptions tag5Value(String tag5Value) {
         return new TagOptions().tag5Value(tag5Value);
      }

      public static TagOptions keys(Set<String> keys) {
         return new TagOptions().keys(keys);
      }
   }

   /**
    * This is strictly not needed but apparently tags with `-` can create a problem when using API, so I've decided to use
    * base64 encoding
    * @param value
    * @return
    */
   public String encodeTag(String value) {
      return base64().encode(value.getBytes(UTF_8));
   }

}
