/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.gradle.gitversion

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal object TimingVersionDetails {
  fun wrap(
    timer: Timer,
    versionDetails: VersionDetails?,
  ): VersionDetails {
    return Proxy.newProxyInstance(
      VersionDetails::class.java.getClassLoader(),
      arrayOf<Class<*>>(
        VersionDetails::class.java,
      ),
    ) { _, method: Method, args: Array<Any?>? ->
      timer.record(method.name) {
        try {
          val args2 = args ?: emptyArray()
          return@record method.invoke(versionDetails, *args2)
        } catch (e: IllegalAccessException) {
          throw RuntimeException("Failed in invoke method", e)
        } catch (e: InvocationTargetException) {
          throw RuntimeException("Failed in invoke method", e)
        }
      }
    } as VersionDetails
  }
}
