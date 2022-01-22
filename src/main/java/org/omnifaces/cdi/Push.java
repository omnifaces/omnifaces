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
package org.omnifaces.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import org.omnifaces.cdi.push.Socket;

/**
 * <p>
 * The CDI annotation <code>&#64;</code>{@link Push} allows you to inject a {@link PushContext} associated with a given
 * channel in any container managed artifact in WAR (not in EAR/EJB!).
 * <pre>
 * &#64;Inject &#64;Push
 * private PushContext channelName;
 * </pre>
 * <p>
 * For detailed usage instructions, see {@link Socket} javadoc.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER })
public @interface Push {

	/**
	 * (Optional) The name of the push channel. If not specified the name of the injection target field will be used.
	 *
	 * @return The name of the push channel.
	 */
	@Nonbinding	String channel() default "";

}