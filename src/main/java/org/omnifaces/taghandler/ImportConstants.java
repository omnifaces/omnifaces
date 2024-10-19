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
package org.omnifaces.taghandler;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.omnifaces.taghandler.ImportFunctions.getClassLoader;
import static org.omnifaces.taghandler.ImportFunctions.toClass;
import static org.omnifaces.util.Facelets.getStringLiteral;
import static org.omnifaces.util.Utils.isOneOf;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.faces.component.UIComponent;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;

import org.omnifaces.util.MapWrapper;
import org.omnifaces.util.Utils;

/**
 * <p>
 * The <code>&lt;o:importConstants&gt;</code> taghandler allows the developer to have a mapping of all constant field
 * values of the given fully qualified name of a type in the request scope. The constant field values are those public
 * static final fields. This works for classes, interfaces and enums.
 *
 * <h2>Usage</h2>
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
 *
 * public enum Faz implements Bar {
 *     FAZ1, FAZ2;
 * }
 * </pre>
 * <p>The constant field values of the above types can be mapped into the request scope as follows:
 * <pre>
 * &lt;o:importConstants type="com.example.Foo" /&gt;
 * &lt;o:importConstants type="com.example.Bar" /&gt;
 * &lt;o:importConstants type="com.example.Baz" var="Bazzz" /&gt;
 * &lt;o:importConstants type="com.example.Faz" /&gt;
 * ...
 * #{Foo.FOO1}, #{Foo.FOO2}, #{Bar.BAR1}, #{Bar.BAR2}, #{Bazzz.BAZ1}, #{Bazzz.BAZ2}, #{Faz.FAZ1}, #{Faz.BAR2}
 * ...
 * &lt;h:selectOneMenu&gt;
 *     &lt;f:selectItems value="#{Faz.values()}" /&gt; &lt;!-- FAZ1, FAZ2, BAR1, BAR2 --&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>
 * <p>The map is by default stored in the request scope by the simple name of the type as variable name. You can override
 * this by explicitly specifying the <code>var</code> attribute, as demonstrated for <code>com.example.Baz</code> in
 * the above example.
 * <p>
 * The resolved constants are by reference stored in the cache to improve retrieving performance. There is also a
 * runtime (no, not compiletime as that's just not possible in EL) check during retrieving the constant value.
 * If a constant value doesn't exist, then an <code>IllegalArgumentException</code> will be thrown.
 * <p>
 * Since version 4.3, you can use the <code>loader</code> attribute to specify an object whose class loader will be used
 * to load the class specified in the <code>type</code> attribute. The class loader of the given object is resolved as
 * specified in {@link Utils#getClassLoader(Object)}. In the end this should allow you to use a more specific class when
 * there are duplicate instances in the runtime classpath, e.g. via multiple (plugin) libraries.
 * <p>
 * Since version 4.6, when the class specified in the <code>type</code> attribute is an <code>enum</code>, such as
 * <code>Baz</code> or <code>Faz</code> in the above example, then you can use <code>#{Faz.members()}</code> to
 * exclusively access enum members rather than all constant field values.
 * <pre>
 * &lt;h:selectOneMenu&gt;
 *     &lt;f:selectItems value="#{Faz.members()}" /&gt; &lt;!-- FAZ1, FAZ2 --&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>

 * <h2>JSF 2.3</h2>
 * <p>
 * JSF 2.3 also offers a <code>&lt;f:importConstants&gt;</code>, however it requires being placed in
 * <code>&lt;f:metadata&gt;</code> which may not be appropriate when you intend to import constants only from
 * a include, tagfile or a composite component.
 *
 * @author Bauke Scholtz
 */
public class ImportConstants extends TagHandler {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Map<String, Map<String, Object>> CONSTANTS_CACHE = new ConcurrentHashMap<>();

    private static final String ERROR_FIELD_ACCESS = "Cannot access constant field '%s' of type '%s'.";
    private static final String ERROR_INVALID_CONSTANT = "Type '%s' does not have the constant '%s'.";
    private static final String ERROR_INVALID_TYPE = "Type '%s' is not an enum.";

    // Variables ------------------------------------------------------------------------------------------------------

    private final String varValue;
    private final TagAttribute typeAttribute;
    private final TagAttribute loaderAttribute;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * The tag constructor.
     * @param config The tag config.
     */
    public ImportConstants(TagConfig config) {
        super(config);
        varValue = getStringLiteral(getAttribute("var"), "var");
        typeAttribute = getRequiredAttribute("type");
        loaderAttribute = getAttribute("loader");
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
        var type = typeAttribute.getValue(context);
        var constants = CONSTANTS_CACHE.get(type);

        if (constants == null) {
            var loader = getClassLoader(context, loaderAttribute);
            constants = collectConstants(type, loader);
            CONSTANTS_CACHE.put(type, constants);
        }

        var var = varValue;

        if (var == null) {
            var innerClass = type.lastIndexOf('$');
            var outerClass = type.lastIndexOf('.');
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
    private static Map<String, Object> collectConstants(String type, ClassLoader loader) {
        var constants = new LinkedHashMap<String, Object>();
        var typeClass = toClass(type, loader);

        for (var declaredType : getDeclaredTypes(typeClass)) {
            for (var field : declaredType.getDeclaredFields()) {
                if (isPublicStaticFinal(field)) {
                    try {
                        constants.putIfAbsent(field.getName(), field.get(null));
                    }
                    catch (Exception e) {
                        throw new IllegalArgumentException(format(ERROR_FIELD_ACCESS, type, field.getName()), e);
                    }
                }
            }
        }

        return new ConstantsMap(constants, typeClass);
    }

    /**
     * Returns an ordered set of all declared types of given type except for Object.class.
     */
    private static Set<Class<?>> getDeclaredTypes(Class<?> type) {
        var declaredTypes = new LinkedHashSet<Class<?>>();
        declaredTypes.add(type);
        fillAllSuperClasses(type, declaredTypes);
        new LinkedHashSet<>(declaredTypes).forEach(declaredType -> fillAllInterfaces(declaredType, declaredTypes));
        return Collections.unmodifiableSet(declaredTypes);
    }

    private static void fillAllSuperClasses(Class<?> type, Set<Class<?>> set) {
        for (var sc = type.getSuperclass(); !isOneOf(sc, null, Object.class); sc = sc.getSuperclass()) {
            set.add(sc);
        }
    }

    private static void fillAllInterfaces(Class<?> type, Set<Class<?>> set) {
        for (var i : type.getInterfaces()) {
            if (set.add(i)) {
                fillAllInterfaces(i, set);
            }
        }
    }

    /**
     * Returns whether the given field is a constant field, that is when it is public, static and final.
     * @param field The field to be checked.
     * @return <code>true</code> if the given field is a constant field, otherwise <code>false</code>.
     */
    private static boolean isPublicStaticFinal(Field field) {
        var modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
    }

    // Nested classes -------------------------------------------------------------------------------------------------

    /**
     * Specific map implementation which wraps the given map in {@link Collections#unmodifiableMap(Map)} and throws an
     * {@link IllegalArgumentException} in {@link ConstantsMap#get(Object)} method when the key doesn't exist at all.
     *
     * <p>
     * Since 4.6 this class is public instead of private in order to allow the EL implementation to see the new
     * {@link #members()} method.
     *
     * @author Bauke Scholtz
     */
    public static class ConstantsMap extends MapWrapper<String, Object> {

        private static final long serialVersionUID = 2L;

        private final Class<?> type;

        public ConstantsMap(Map<String, Object> map, Class<?> type) {
            super(Collections.unmodifiableMap(map));
            this.type = type;
        }

        @Override
        public Object get(Object key) {
            if (!containsKey(key)) {
                throw new IllegalArgumentException(format(ERROR_INVALID_CONSTANT, type.toString(), type));
            }

            return super.get(key);
        }

        /**
         * Returns Exclusively enum members in case the type is an enum.
         * @return Exclusively enum members in case the type is an enum.
         * @throws IllegalStateException in case the type is not an enum.
         * @since 4.6
         */
        public Collection<Object> members() {
            if (!type.isEnum()) {
                throw new IllegalStateException(format(ERROR_INVALID_TYPE, type.toString()));
            }

            return asList(type.getEnumConstants());
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
