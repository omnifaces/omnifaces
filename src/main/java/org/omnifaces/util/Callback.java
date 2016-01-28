/*
 * Copyright 2012 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Collection of callback interfaces. Useful in (mini) visitor and strategy patterns.
 *
 * @author Bauke Scholtz
 */
public final class Callback {

	// Interfaces -----------------------------------------------------------------------------------------------------

	/**
	 * Use this if you need a void callback.
	 *
	 * @author Bauke Scholtz
	 */
	public interface Void {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 */
		void invoke();

	}

	/**
	 * Use this if you need a serializable void callback.
	 *
	 * @author Bauke Scholtz
	 * @since 2.1
	 */
	public interface SerializableVoid extends Serializable {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 */
		void invoke();

	}

	/**
	 * Use this if you need a callback which returns a value.
	 *
	 * @author Bauke Scholtz
	 * @param <R> The return type.
	 */
	public interface Returning<R> {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 * @return The callback result.
		 */
		R invoke();

	}

	/**
	 * Use this if you need a serializable callback which returns a value.
	 *
	 * @author Bauke Scholtz
	 * @param <R> The return type.
	 * @since 2.1
	 */
	public interface SerializableReturning<R> extends Serializable {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 * @return The callback result.
		 */
		R invoke();

	}

	/**
	 * Use this if you need a callback which takes an argument.
	 *
	 * @author Bauke Scholtz
	 * @param <A> The argument type.
	 */
	public interface WithArgument<A> {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 * @param a The callback argument to work with.
		 */
		void invoke(A a);

	}

	/**
	 * Use this if you need a serializable callback which takes an argument.
	 *
	 * @author Bauke Scholtz
	 * @param <A> The argument type.
	 * @since 2.1
	 */
	public interface SerializableWithArgument<A> extends Serializable {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 * @param a The callback argument to work with.
		 */
		void invoke(A a);

	}

	/**
	 * Use this if you need a callback which takes an argument and returns a value.
	 *
	 * @author Bauke Scholtz
	 * @param <R> The return type.
	 * @param <A> The argument type.
	 */
	public interface ReturningWithArgument<R, A> {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 * @param a The callback argument to work with.
		 * @return The callback result.
		 */
		R invoke(A a);

	}

	/**
	 * Use this if you need a serializable callback which takes an argument and returns a value.
	 *
	 * @author Bauke Scholtz
	 * @param <R> The return type.
	 * @param <A> The argument type.
	 * @since 2.1
	 */
	public interface SerializableReturningWithArgument<R, A> extends Serializable {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 * @param a The callback argument to work with.
		 * @return The callback result.
		 */
		R invoke(A a);

	}


	/**
	 * Use this if you need an output stream callback.
	 *
	 * @author Bauke Scholtz
	 * @since 2.3
	 */
	public interface Output {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 * @param output The callback output stream to write to.
		 * @throws IOException Whenever something fails at I/O level.
		 */
		void writeTo(OutputStream output) throws IOException;

	}

}