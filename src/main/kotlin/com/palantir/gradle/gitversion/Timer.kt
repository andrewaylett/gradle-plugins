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

import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class Timer {
  private val totalTimesTakenMillis: ConcurrentMap<String?, Long> = ConcurrentHashMap()

  fun <T> record(
    name: String?,
    codeToTime: Supplier<T>,
  ): T {
    val stopwatch = Stopwatch.createStarted()
    return try {
      codeToTime.get()
    } finally {
      stopwatch.stop()
      val timeTakenMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS)
      totalTimesTakenMillis.compute(
        name,
      ) { _, previousValue: Long? -> timeTakenMillis + (previousValue ?: 0) }
    }
  }

  fun toJson(): String {
    val withTotal: Map<String?, Long> =
      ImmutableMap.builder<String?, Long>()
        .putAll(totalTimesTakenMillis)
        .put("total", totalMillis())
        .build()
    return JsonUtils.mapToJson(withTotal)
  }

  fun totalMillis(): Long {
    return totalTimesTakenMillis.values.stream().mapToLong { time: Long? -> time!! }.sum()
  }
}
