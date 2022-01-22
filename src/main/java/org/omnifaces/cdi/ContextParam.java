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

import org.omnifaces.cdi.contextparam.ContextParamProducer;

/**
 * <p>
 * The CDI annotation <code>&#64;</code>{@link ContextParam} allows you to inject a <code>web.xml</code> context
 * parameter from the current application in a CDI managed bean. It's basically like
 * <code>&#64;ManagedProperty("#{initParam['some.key']}") private String someKey;</code>
 * in a "plain old" JSF managed bean.
 * <p>
 * By default the name of the context parameter is taken from the name of the variable into which injection takes place.
 * The example below injects the context parameter with name <code>foo</code>.
 * <pre>
 * &#64;Inject &#64;ContextParam
 * private String foo;
 * </pre>
 * <p>
 * The name can be optionally specified via the <code>name</code> attribute, which shall more often be used as context
 * parameters may have a.o. periods and/or hyphens in the name, which are illegal in variable names.
 * The example below injects the context parameter with name <code>foo.bar</code> into a variable named <code>bar</code>.
 * <pre>
 * &#64;Inject &#64;ContextParm(name="foo.bar")
 * private String bar;
 * </pre>
 *
 * @since 2.2
 * @author Bauke Scholtz
 * @see ContextParamProducer
 */
@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER })
public @interface ContextParam {

	/**
	 * (Optional) The name of the context parameter. If not specified the name of the injection target field will be used.
	 *
	 * @return The name of the context parameter.
	 */
	@Nonbinding	String name() default "";

}