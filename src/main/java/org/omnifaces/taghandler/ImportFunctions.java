/*
 * Copyright 2013 OmniFaces.
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
package org.omnifaces.taghandler;

import static org.omnifaces.taghandler.ImportConstants.toClass;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.el.FunctionMapper;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

/**
 * <p>The <code>&lt;o:importFunctions&gt;</code> allows the developer to have access to all functions of the given
 * fully qualified name of a type in the EL scope using the usual EL functions syntax without the need to register them
 * in <code>.taglib.xml</code> file. The functions are those <code>public static</code> methods with a
 * <strong>non</strong>-<code>void</code> return type. For example:
 * <pre>
 * &lt;o:importFunctions type="java.lang.Math" var="m" /&gt;
 * &lt;o:importFunctions type="org.omnifaces.util.Faces" /&gt;
 * ...
 * #{m:abs(-10)}
 * #{m:max(bean.number1, bean.number2)}
 * ...
 * &lt;base href="#{Faces:getRequestBaseURL()}" /&gt;
 * </pre>
 * <p>The functions prefix becomes by default the simple name of the type. You can override this by explicitly
 * specifying the <code>var</code> attribute. If there are multiple function methods with exactly the same name, then
 * the one with the least amount of parameters will be used. If there are multiple function methods with exactly the
 * same name and amount of parameters, then the choice is unspecified (technically, JVM-dependent) and should not be
 * relied upon. So if you absolutely need to differentiate functions in such case, give them each a different name.</p>
 * <p>Note that the colon <code>:</code> operator to invoke the method is as required by EL functions spec. It's by
 * design not easily possible to change it to the period <code>.</code> operator. Also note that in case of
 * <code>org.omnifaces.util.Faces</code> it's considered poor practice if the same functionality is already available
 * through the implicit EL variables <code>#{facesContext}</code>, <code>#{view}</code>, <code>#{request}</code>, etc
 * such as <code>#{request.contextPath}</code> which should be preferred over
 * <code>#{Faces:getRequestContextPath()}</code>.</p>
 * <p>
 * The resolved functions are by reference stored in the cache to improve retrieving performance.
 *
 * @author Bauke Scholtz
 * @since 1.4
 */
public class ImportFunctions extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static Map<String, Method> FUNCTIONS_CACHE = new HashMap<String, Method>();

	private static final String ERROR_INVALID_VAR = "The 'var' attribute may not be an EL expression.";

	// Variables ------------------------------------------------------------------------------------------------------

	private String var;
	private TagAttribute type;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public ImportFunctions(TagConfig config) {
		super(config);
		TagAttribute var = getAttribute("var");

		if (var != null) {
			if (var.isLiteral()) {
				this.var = var.getValue();
			}
			else {
				throw new IllegalArgumentException(ERROR_INVALID_VAR);
			}
		}

		type = getRequiredAttribute("type");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register a new {@link FunctionMapper} which checks if the given prefix matches our own <code>var</code> and then
	 * find the associated method based on the given method name.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		String type = this.type.getValue(context);
		final Class<?> cls = toClass(type);
		final String var = (this.var != null) ? this.var : type.substring(type.lastIndexOf('.') + 1);
		final FunctionMapper originalFunctionMapper = context.getFunctionMapper();
		context.setFunctionMapper(new FunctionMapper() {

			@Override
			public Method resolveFunction(String prefix, String name) {
				if (var.equals(prefix)) {
					String key = cls + "." + name;
					Method function = FUNCTIONS_CACHE.get(key);

					if (function == null) {
						function = findMethod(cls, name);
						FUNCTIONS_CACHE.put(key, function);
					}

					return function;
				}
				else {
					return originalFunctionMapper.resolveFunction(prefix, name);
				}
			}
		});
	}

	/**
	 * Collect all public static methods of the given name in the given class, sort them by the amount of parameters
	 * and return the first one.
	 * @param cls The class to find the method in.
	 * @param name The method name.
	 * @return The found method, or <code>null</code> if none is found.
	 */
	private static Method findMethod(Class<?> cls, String name) {
		Set<Method> methods = new TreeSet<Method>(new Comparator<Method>() {
			@Override
			public int compare(Method m1, Method m2) {
				return Integer.valueOf(m1.getParameterTypes().length).compareTo(m2.getParameterTypes().length);
			}
		});

		for (Method method : cls.getDeclaredMethods()) {
			if (method.getName().equals(name) && isPublicStaticNonVoid(method)) {
				methods.add(method);
			}
		}

		return methods.isEmpty() ? null : methods.iterator().next();
	}

	/**
	 * Returns whether the given method is an utility method, that is when it is public and static and returns a
	 * non-void type.
	 * @param method The method to be checked.
	 * @return <code>true</code> if the given method is an utility method, otherwise <code>false</code>.
	 */
	private static boolean isPublicStaticNonVoid(Method method) {
		int modifiers = method.getModifiers();
		return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && method.getReturnType() != void.class;
	}

}
