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
package org.omnifaces.taghandler;

import static java.lang.Math.max;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.omnifaces.util.MapWrapper;

/**
 * <p>
 * The <code>&lt;o:importConstants&gt;</code> taghandler allows the developer to have a mapping of all constant field
 * values of the given fully qualified name of a type in the request scope. The constant field values are those public
 * static final fields. This works for classes, interfaces and enums.
 *
 * <h3>Usage</h3>
 * <p>
 * For example:
 * <pre>
 * public class Foo {
 *     public static final String FOO1 = "foo1";
 *     public static final String FOO2 = "foo2";
 * }
 *
 * public interface Bar {
 *     public String BAR1 = "bar1";
 *     public String BAR2 = "bar2";
 * }
 *
 * public enum Baz {
 *     BAZ1, BAZ2;
 * }
 * </pre>
 * <p>The constant field values of the above types can be mapped into the request scope as follows:
 * <pre>
 * &lt;o:importConstants type="com.example.Foo" /&gt;
 * &lt;o:importConstants type="com.example.Bar" /&gt;
 * &lt;o:importConstants type="com.example.Baz" var="Bazzz" /&gt;
 * ...
 * #{Foo.FOO1}, #{Foo.FOO2}, #{Bar.BAR1}, #{Bar.BAR2}, #{Bazzz.BAZ1}, #{Bazzz.BAZ2}
 * </pre>
 * <p>The map is by default stored in the request scope by the simple name of the type as variable name. You can override
 * this by explicitly specifying the <code>var</code> attribute, as demonstrated for <code>com.example.Baz</code> in
 * the above example.
 * <p>
 * The resolved constants are by reference stored in the cache to improve retrieving performance. There is also a
 * runtime (no, not compiletime as that's just not possible in EL) check during retrieving the constant value.
 * If a constant value doesn't exist, then an <code>IllegalArgumentException</code> will be thrown.
 *
 * @author Bauke Scholtz
 */
public class ImportConstants extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Map<String, Map<String, Object>> CONSTANTS_CACHE = new ConcurrentHashMap<>();

	private static final String ERROR_INVALID_VAR = "The 'var' attribute may not be an EL expression.";
	private static final String ERROR_MISSING_CLASS = "Cannot find type '%s' in classpath.";
	private static final String ERROR_FIELD_ACCESS = "Cannot access constant field '%s' of type '%s'.";
	private static final String ERROR_INVALID_CONSTANT = "Type '%s' does not have the constant '%s'.";

	// Variables ------------------------------------------------------------------------------------------------------

	private String varValue;
	private TagAttribute typeAttribute;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public ImportConstants(TagConfig config) {
		super(config);
		TagAttribute var = getAttribute("var");

		if (var != null) {
			if (var.isLiteral()) {
				varValue = var.getValue();
			}
			else {
				throw new IllegalArgumentException(ERROR_INVALID_VAR);
			}
		}

		typeAttribute = getRequiredAttribute("type");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * First obtain the constants of the class by its fully qualified name as specified in the <code>type</code>
	 * attribute from the cache. If it hasn't been collected yet and is thus not present in the cache, then collect
	 * them and store in cache. Finally set the constants in the request scope by the simple name of the type, or by the
	 * name as specified in the <code>var</code> attribute, if any.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		String type = typeAttribute.getValue(context);
		Map<String, Object> constants = CONSTANTS_CACHE.get(type);

		if (constants == null) {
			constants = collectConstants(type);
			CONSTANTS_CACHE.put(type, constants);
		}

		String var = varValue;

		if (var == null) {
			int innerClass = type.lastIndexOf('$');
			int outerClass = type.lastIndexOf('.');
			var = type.substring(max(innerClass, outerClass) + 1);
		}

		context.setAttribute(var, constants);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Collect constants of the given type. That are, all public static final fields of the given type.
	 * @param type The fully qualified name of the type to collect constants for.
	 * @return Constants of the given type.
	 */
	private static Map<String, Object> collectConstants(final String type) {
		Map<String, Object> constants = new LinkedHashMap<>();

		for (Field field : toClass(type).getFields()) {
			if (isPublicStaticFinal(field)) {
				try {
					constants.put(field.getName(), field.get(null));
				}
				catch (Exception e) {
					throw new IllegalArgumentException(String.format(ERROR_FIELD_ACCESS, type, field.getName()), e);
				}
			}
		}

		return new ConstantsMap(constants, type);
	}

	/**
	 * Convert the given type, which should represent a fully qualified name, to a concrete {@link Class} instance.
	 * @param type The fully qualified name of the class.
	 * @return The concrete {@link Class} instance.
	 * @throws IllegalArgumentException When it is missing in the classpath.
	 */
	static Class<?> toClass(String type) { // Package-private so that ImportFunctions can also use it.
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
					ignore = null; // Just continue to IllegalArgumentException on original ClassNotFoundException.
				}
			}

			throw new IllegalArgumentException(String.format(ERROR_MISSING_CLASS, type), e);
		}
	}

	/**
	 * Returns whether the given field is a constant field, that is when it is public, static and final.
	 * @param field The field to be checked.
	 * @return <code>true</code> if the given field is a constant field, otherwise <code>false</code>.
	 */
	private static boolean isPublicStaticFinal(Field field) {
		int modifiers = field.getModifiers();
		return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * Specific map implementation which wraps the given map in {@link Collections#unmodifiableMap(Map)} and throws an
	 * {@link IllegalArgumentException} in {@link ConstantsMap#get(Object)} method when the key doesn't exist at all.
	 *
	 * @author Bauke Scholtz
	 */
	private static class ConstantsMap extends MapWrapper<String, Object> {

		private static final long serialVersionUID = -7699617036767530156L;

		private String type;

		public ConstantsMap(Map<String, Object> map, String type) {
			super(Collections.unmodifiableMap(map));
			this.type = type;
		}

		@Override
		public Object get(Object key) {
			if (!containsKey(key)) {
				throw new IllegalArgumentException(String.format(ERROR_INVALID_CONSTANT, type, key));
			}

			return super.get(key);
		}

		@Override
		public boolean equals(Object object) {
			return super.equals(object) && type.equals(((ConstantsMap) object).type);
		}

		@Override
		public int hashCode() {
			return super.hashCode() + type.hashCode();
		}

	}

}
