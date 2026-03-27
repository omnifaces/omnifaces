/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.test;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.IntStream.range;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Concurrency {

    public static final int DEFAULT_ITERATIONS = 1000;

    private Concurrency() {
        throw new AssertionError();
    }

    public static void testThreadSafety(Consumer<Integer> task) {
        testThreadSafety(task, DEFAULT_ITERATIONS);
    }

    public static void testThreadSafety(Consumer<Integer> task, int iterations) {
        testThreadSafety((i, tasks) -> tasks.add(runAsync(() -> task.accept(i))), iterations);
    }

    public static void testThreadSafety(BiConsumer<Integer, Set<CompletableFuture<Void>>> task, int iterations) {
        Set<CompletableFuture<Void>> tasks = ConcurrentHashMap.newKeySet();
        range(0, iterations).forEach(i -> task.accept(i, tasks));
        awaitCompletion(tasks);
    }

    private static void awaitCompletion(Set<CompletableFuture<Void>> tasks) {
        tasks.forEach(t -> {
            try {
                t.get();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch (ExecutionException e) {
                throw new IllegalStateException(e);
            }
        });
    }

}
