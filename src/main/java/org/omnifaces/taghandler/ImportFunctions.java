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
package org.omnifaces.taghandler;

import static java.lang.String.format;
import static java.util.logging.Level.FINEST;
import static org.omnifaces.util.Facelets.getStringLiteral;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import jakarta.el.FunctionMapper;
import jakarta.faces.component.UIComponent;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;

/**
 * <p>
 * The <code>&lt;o:importFunctions&gt;</code> taghandler allows the developer to have access to all functions of the
 * given fully qualified name of a type in the Facelet scope using the usual EL functions syntax without the need to
 * register them in <code>.taglib.xml</code> file. The functions are those <code>public static</code> methods with a
 * <strong>non</strong>-<code>void</code> return type.
 *
 * <h3>Usage</h3>
 * <p>
 * For example:
 * <pre>
 * &lt;o:importFunctions type="java.lang.Math" var="m" /&gt;
 * ...
 * #{m:abs(-10)}
 * #{m:max(bean.number1, bean.number2)}
 * </pre>
 * <p>
 * The functions prefix becomes by default the simple name of the type. You can override this by explicitly
 * specifying the <code>var</code> attribute.
 * <p>
 * The resolved functions are by reference stored in the cache to improve retrieving performance.
 *
 * <h3>Precaution as to multiple functions with exactly the same method name</h3>
 * <p>
 * EL functions does <strong>not</strong> support method overloading. It's therefore <strong>not</strong> possible to
 * provide overloaded methods like {@link Math#abs(int)}, {@link Math#abs(long)}, {@link Math#abs(float)} and
 * {@link Math#abs(double)} in four separate EL functions.
 * <p>
 * If there are multiple function methods discovered with exactly the same name, then the one with the least amount of
 * parameters will be used. If there are multiple function methods with exactly the same name and amount of parameters,
 * then the choice is unspecified (technically, JVM-dependent, the first one in the methods array as found by reflection
 * would be picked up) and should not be relied upon. So if you absolutely need to differentiate functions in such case,
 * give them each a different name.
 *
 * <h3>Design notes</h3>
 * <p>
 * Note that the colon <code>:</code> operator to invoke the method is as required by EL functions spec. It's by
 * design not easily possible to change it to the period <code>.</code> operator. Also note that in case of
 * <code>org.omnifaces.util.Faces</code> it's considered poor practice if the same functionality is already available
 * through the implicit EL objects <code>#{faces}</code>, <code>#{facesContext}</code>, <code>#{view}</code>,
 * <code>#{request}</code>, etc such as <code>#{faces.development}</code> or <code>#{request.contextPath}</code> which
 * should be preferred over <code>#{Faces:isDevelopment()}</code> or <code>#{Faces:getRequestContextPath()}</code>.
 *
 * @author Bauke Scholtz
 * @since 1.4
 */
public class ImportFunctions extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(ImportFunctions.class.getName());

	private static final Map<String, Method> FUNCTIONS_CACHE = new ConcurrentHashMap<>();
	private static final String ERROR_MISSING_CLASS = "Cannot find type '%s' in classpath.";
	private static final String ERROR_INVALID_FUNCTION = "Type '%s' does not have the function '%s'.";

	// Variables ------------------------------------------------------------------------------------------------------

	private String varValue;
	private TagAttribute typeAttribute;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public ImportFunctions(TagConfig config) {
		super(config);
		varValue = getStringLiteral(getAttribute("var"), "var");
		typeAttribute = getRequiredAttribute("type");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register a new {@link FunctionMapper} which checks if the given prefix matches our own <code>var</code> and then
	 * find the associated method based on the given method name.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		String type = typeAttribute.getValue(context);
		String var = (varValue != null) ? varValue : type.substring(type.lastIndexOf('.') + 1);
		FunctionMapper originalFunctionMapper = context.getFunctionMapper();
		context.setFunctionMapper(new ImportFunctionsMapper(originalFunctionMapper, var, toClass(type)));
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Convert the given type, which should represent a fully qualified name, to a concrete {@link Class} instance.
	 * @param type The fully qualified name of the class.
	 * @return The concrete {@link Class} instance.
	 * @throws IllegalArgumentException When it is missing in the classpath.
	 */
	static Class<?> toClass(String type) { // Package-private so that ImportConstants can also use it.
		try {
			return Class.forName(type, true, Thread.currentThread().getContextClassLoader());
		}
		catch (ClassNotFoundException e) {
			// Perhaps it's an inner enum which is specified as com.example.SomeClass.SomeEnum.
			// Let's be lenient on that although the proper type notation should be com.example.SomeClass$SomeEnum.
			int i = type.lastIndexOf('.');

			if (i > 0) {
				try {
					return toClass(new StringBuilder(type).replace(i, i + 1, "$").toString());
				}
				catch (Exception ignore) {
					logger.log(FINEST, "Ignoring thrown exception; previous exception will be rethrown instead.", ignore);
					// Just continue to IllegalArgumentException on original ClassNotFoundException.
				}
			}

			throw new IllegalArgumentException(format(ERROR_MISSING_CLASS, type), e);
		}
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	private static class ImportFunctionsMapper extends FunctionMapper {

		private FunctionMapper originalFunctionMapper;
		private String var;
		private Class<?> type;

		public ImportFunctionsMapper(FunctionMapper originalFunctionMapper, String var, Class<?> type) {
			this.originalFunctionMapper = originalFunctionMapper;
			this.var = var;
			this.type = type;
		}

		@Override
		public Method resolveFunction(String prefix, String name) {
			if (var.equals(prefix)) {
				String key = type + "." + name;
				Method function = FUNCTIONS_CACHE.get(key);

				if (function == null) {
					function = findMethod(type, name);

					if (function == null) {
						throw new IllegalArgumentException(format(ERROR_INVALID_FUNCTION, type.getName(), name));
					}

					FUNCTIONS_CACHE.put(key, function);
				}

				return function;
			}
			else {
				return originalFunctionMapper.resolveFunction(prefix, name);
			}
		}

		/**
		 * Collect all public static methods of the given name in the given class, sort them by the amount of parameters
		 * and return the first one.
		 * @param cls The class to find the method in.
		 * @param name The method name.
		 * @return The found method, or <code>null</code> if none is found.
		 */
		private static Method findMethod(Class<?> cls, String name) {
			Set<Method> methods = new TreeSet<>((Method m1, Method m2) -> Integer.valueOf(m1.getParameterCount()).compareTo(m2.getParameterCount()));

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

}