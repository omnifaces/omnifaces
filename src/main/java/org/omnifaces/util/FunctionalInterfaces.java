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
package org.omnifaces.util;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Collection of functional interfaces. Useful in (mini) visitor and strategy patterns.
 *
 * @author Bauke Scholtz
 * @since 4.6
 */
public final class FunctionalInterfaces {

    /**
     * Use this if you need a serializable runnable.
     *
     * @author Bauke Scholtz
     * @see Runnable
     */
    @FunctionalInterface
    public interface SerializableRunnable extends Serializable {

        /**
         * @see Runnable#run()
         */
        void run();
    }

    /**
     * Use this if you need a serializable supplier.
     *
     * @author Bauke Scholtz
     * @param <T> the type of results supplied by this supplier
     * @see Supplier
     */
    @FunctionalInterface
    public interface SerializableSupplier<T> extends Serializable {

        /**
         * @see Supplier#get()
         */
        T get();
    }

    /**
     * Use this if you need a serializable consumer.
     *
     * @author Bauke Scholtz
     * @param <T> the type of the input to the operation
     * @see Consumer
     */
    @FunctionalInterface
    public interface SerializableConsumer<T> extends Serializable {

        /**
         * @see Consumer#accept(Object)
         */
        void accept(T t);
    }

    /**
     * Use this if you need a serializable bi-consumer.
     *
     * @author Bauke Scholtz
     * @param <T> the type of the first argument to the operation
     * @param <U> the type of the second argument to the operation
     * @see BiConsumer
     */
    @FunctionalInterface
    public interface SerializableBiConsumer<T, U> extends Serializable {

        /**
         * @see BiConsumer#accept(Object, Object)
         */
        void accept(T t, U u);
    }

    /**
     * Use this if you need a serializable function.
     *
     * @author Bauke Scholtz
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @see Function
     */
    @FunctionalInterface
    public interface SerializableFunction<T, R> extends Serializable {

        /**
         * @see Function#apply(Object)
         */
        R apply(T t);
    }

    /**
     * Use this if you need a serializable bi-function.
     *
     * @author Bauke Scholtz
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     * @param <R> the type of the result of the function
     * @see BiFunction
     */
    @FunctionalInterface
    public interface SerializableBiFunction<T, U, R> extends Serializable {

        /**
         * @see BiFunction#apply(Object, Object)
         */
        R apply(T t, U u);
    }

    /**
     * Use this if you need a throwing runnable.
     *
     * @author Bauke Scholtz
     * @see Runnable
     */
    @FunctionalInterface
    public interface ThrowingRunnable {

        /**
         * @see Runnable#run()
         * @throws Exception When something irrecoverably fails.
         */
        void run() throws Exception;
    }

    /**
     * Use this if you need a throwing supplier.
     *
     * @author Bauke Scholtz
     * @param <T> the type of results supplied by this supplier
     * @see Supplier
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {

        /**
         * @see Supplier#get()
         * @throws Exception When something irrecoverably fails.
         */
        T get() throws Exception;
    }

    /**
     * Use this if you need a throwing consumer.
     *
     * @author Bauke Scholtz
     * @param <T> the type of the input to the operation
     * @see Consumer
     */
    @FunctionalInterface
    public interface ThrowingConsumer<T> {

        /**
         * @see Consumer#accept(Object)
         * @throws Exception When something irrecoverably fails.
         */
        void accept(T t) throws Exception;
    }

    /**
     * Use this if you need a throwing bi-consumer.
     *
     * @author Bauke Scholtz
     * @param <T> the type of the first argument to the operation
     * @param <U> the type of the second argument to the operation
     * @see BiConsumer
     */
    @FunctionalInterface
    public interface ThrowingBiConsumer<T, U> {

        /**
         * @see BiConsumer#accept(Object, Object)
         * @throws Exception When something irrecoverably fails.
         */
        void accept(T t, U u) throws Exception;
    }

    /**
     * Use this if you need a throwing function.
     *
     * @author Bauke Scholtz
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @see Function
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {

        /**
         * @see Function#apply(Object)
         * @throws Exception When something irrecoverably fails.
         */
        R apply(T t) throws Exception;
    }

    /**
     * Use this if you need a throwing bi-function.
     *
     * @author Bauke Scholtz
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     * @param <R> the type of the result of the function
     * @see BiFunction
     */
    @FunctionalInterface
    public interface ThrowingBiFunction<T, U, R> {

        /**
         * @see BiFunction#apply(Object, Object)
         * @throws Exception When something irrecoverably fails.
         */
        R apply(T t, U u) throws Exception;
    }
}