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

/**
 * Collection of callback interfaces. Useful in (mini) visitor and strategy patterns.
 *
 * @author Bauke Scholtz
 */
public final class Callback {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Callback() {
		// Hide constructor.
	}

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
	 * Use this if you need a callback which takes two arguments.
	 *
	 * @author Arjan Tijms
	 * @param <A> The first argument type.
	 * @param <B> The second argument type.
	 */
	public interface With2Arguments<A, B> {

		/**
		 * This method should be invoked by the method where you're passing this callback instance to.
		 * @param a The first callback argument to work with.
		 * @param a The second callback argument to work with.
		 */
		void invoke(A a, B b);

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

}