/*
 * Copyright 2020 OmniFaces
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

import static org.omnifaces.util.Exceptions.is;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import jakarta.annotation.Resource;
import jakarta.faces.application.ProjectStage;

/**
 * <p>
 * Utility class for simplifying some web related tasks that use JNDI under the hood, such as getting the
 * <code>&lt;env-entry&gt;</code> from <code>web.xml</code>.
 * <p>
 * Note that the JSF spec defines one parameter that can optionally be defined via an env entry instead of the typical
 * context/init parameter; {@link ProjectStage#PROJECT_STAGE_JNDI_NAME}. Mojarra defines an additional proprietary one:
 * "java:comp/env/ClientStateSavingPassword".
 *
 * @author Arjan Tijms
 * @since 1.6
 */
public final class JNDI {

	private JNDI() {
		// Hide constructor.
	}

	/**
	 * Returns the named environment entry for the deployment component from where this is called. From within the web
	 * module this returns the <code>&lt;env-entry-value&gt;</code> of a <code>&lt;env-entry&gt;</code> in
	 * <code>web.xml</code> associated with the given <code>&lt;env-entry-name&gt;</code>.
	 * <p>
	 * Note that the name used here is relative to "java:comp/env", which is exactly as it appears in <code>web.xml</code>.
	 * <p>
	 * Example:
	 * <p>
	 * <b>web.xml</b>
	 * <pre>
	 * &lt;env-entry&gt;
	 *	&lt;cont&gt;org.omnifaces.TEST_INTEGER&lt;/env-entry-name&gt;
	 *	&lt;env-entry-type&gt;java.lang.Integer&lt;/env-entry-type&gt;
	 *	&lt;env-entry-value&gt;10&lt;/env-entry-value&gt;
	 * &lt;/env-entry&gt;
	 * </pre>
	 * <p>
	 * <b>Lookup in Java using relative name</b>
	 * <pre>
	 * Integer test = JNDI.getEnvEntry("org.omnifaces.TEST_INTEGER");
	 * </pre>
	 * <p>
	 * <b>Lookup in Java using full JNDI name</b>
	 * <pre>
	 * Integer test = JNDI.lookup("java:comp/env/org.omnifaces.TEST_INTEGER");
	 * </pre>
	 *
	 * <p>
	 * Note that even the "full JNDI name" is relative to the "deployment component" from which the lookup is done.
	 * To use a true global JNDI name an additional <code>&lt;lookup-name&gt;</code> should be specified in <code>web.xml</code>.
	 *
	 * <p>
	 * Environment entries can also be injected using {@link Resource}.
	 *
	 * @param <T> The expected return type.
	 * @param name the environment entry name relative to "java:comp/env".
	 * @return The environment entry value associated with the given name, or <code>null</code> if
	 * there is none.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see InitialContext#lookup(String)
	 * @since 1.6
	 */
	public static <T> T getEnvEntry(String name) {
		return lookup("java:comp/env/" + name);
	}

	/**
	 * Returns the named object from the default JNDI instance.
	 * @param <T> The expected return type.
	 * @param name the name of the object to be retrieved
	 * @return the named object, or <code>null</code> if there is none.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see InitialContext#lookup(String)
	 * @since 1.6
	 */
	@SuppressWarnings("unchecked")
	public static <T> T lookup(String name) {
		InitialContext context = null;
		try {
			context = new InitialContext();
			return (T) context.lookup(name);
		} catch (NamingException e) {
			if (is(e, NameNotFoundException.class)) {
				return null;
			} else {
				throw new IllegalStateException(e);
			}
		} finally {
			close(context);
		}
	}

	private static void close(InitialContext context) {
		try {
			if (context != null) {
				context.close();
			}
		} catch (NamingException e) {
			throw new IllegalStateException(e);
		}
	}

}