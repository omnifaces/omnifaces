/*
 * Copyright 2013 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

/**
 * <p>
 * This CDI extension should tell the CDI implementation to register from the org.omnifaces package only the classes
 * from org.omnifaces.cdi and org.omnifaces.showcase subpackages as CDI managed beans.
 *
 * <h3>Weld 2.x warning</h3>
 * <p>
 * Weld 2.x will during startup log a warning like below on this class:
 * <blockquote>
 * WARN: WELD-000411: Observer method [BackedAnnotatedMethod] public
 * org.omnifaces.VetoAnnotatedTypeExtension.processAnnotatedType(&#64;Observes ProcessAnnotatedType&lt;Object&gt;)
 * receives events for all annotated types. Consider restricting events using &#64;WithAnnotations or a generic
 * type with bounds.
 * </blockquote>
 * <p>
 * First of all, this warning is harmless. We are aware of this and we won't take further action in order to ensure
 * compatibility of OmniFaces on as many containers as possible.
 * <p>
 * By default, CDI capable containers attempt to register every single class in every single JAR in
 * <code>/WEB-INF/lib</code> as a CDI managed bean. In older CDI versions as used by older Java EE 6 containers, there
 * were bugs whereby the CDI implementation even attempts to register enums, abstract classes and/or classes without a
 * default constructor, resulting in deployment exceptions (WebLogic), runtime exceptions (WebSphere) and/or loads of
 * confusing warnings (GlassFish/TomEE).
 * <p>
 * In order to solve that, OmniFaces added this {@link VetoAnnotatedTypeExtension} which should "veto" any class from
 * <code>org.omnifaces</code> package which is <strong>not</strong> in either <code>org.omnifaces.cdi</code> or
 * <code>org.omnifaces.showcase</code> package from being registered as a CDI managed bean. This happens via
 * {@link ProcessAnnotatedType#veto()}
 * <p>
 * In Weld 2.x as used in among others WildFly, there's apparently a new type of warning which occurs when you use
 * {@link ProcessAnnotatedType} on <strong>all</strong> classes. However, in this specific case, it's just the whole
 * purpose to scan every single class, because classes which needs to be excluded from being registered as CDI managed
 * beans are obviously not explicitly registered as a CDI managed bean (for which you could otherwise use a more
 * specific <code>T</code> or <code>&#64;WithAnnotations</code> as suggested in the warning message).
 * <p>
 * Theoretically, the solution in this specific case would be to use <code>&lt;weld:include&gt;</code> or
 * <code>&lt;weld:exclude&gt;</code> in <code>beans.xml</code> instead of an <code>Extension</code>. However, this is
 * not a standard CDI solution as this is Weld-specific and thus wouldn't work with other CDI implementations such as
 * OpenWebBeans or CanDI. We couldn't figure a similar mechanism for other CDI implementations, so we went for the
 * <code>Extension</code>.
 * <p>
 * After all, again, this is "just" a warning, not an error. Everything should continue to work as intented.
 *
 * @author Bauke Scholtz
 * @since 1.6.1
 */
public class VetoAnnotatedTypeExtension implements Extension {

	public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> type) {
		Package typePackage = type.getAnnotatedType().getJavaClass().getPackage();

		if (typePackage == null) {
			return;
		}

		String packageName = typePackage.getName();

		if (packageName.startsWith("org.omnifaces.")
			&& !packageName.startsWith("org.omnifaces.cdi")
			&& !packageName.startsWith("org.omnifaces.showcase"))
		{
			type.veto();
		}
	}

}