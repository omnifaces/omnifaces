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

import static java.util.Collections.list;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.omnifaces.taghandler.ImportFunctions.getClassLoader;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Facelets.getStringLiteral;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagAttributeException;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;

import org.omnifaces.util.Faces;
import org.omnifaces.util.FacesLocal;
import org.omnifaces.util.MapWrapper;
import org.omnifaces.util.Utils;

/**
 * <p>
 * The <code>&lt;o:loadBundle&gt;</code> taghandler basically extends the standard <code>&lt;f:loadBundle&gt;</code>
 * with a new <code>loader</code> attribute allowing you to explicitly set the desired {@link ClassLoader} where
 * the resource bundle should be looked up. Also the {@link Locale} of the bundle is obtained with better default values
 * than the default Faces implementation.
 * <p>
 * You can use the <code>loader</code> attribute to specify an object whose class loader will be used to load the
 * resource bundle specified in the <code>basename</code> attribute. The class loader of the given object is resolved as
 * specified in {@link Utils#getClassLoader(Object)}. In the end this should allow you to use a more specific resource
 * bundle when there are duplicate instances in the runtime classpath, e.g. via multiple (plugin) libraries.
 * <p>
 * The locale of the resource bundle is obtained as specified in {@link Faces#getLocale()}.
 *
 * <h2>Usage</h2>
 * <p>
 * You can use it the same way as <code>&lt;f:loadBundle&gt;</code>, you only need to change <code>f:</code> into
 * <code>o:</code> to get the extra support for <code>loader</code> attribute and the improved locale resolving.
 *
 * @author Bauke Scholtz
 * @since 4.3
 */
public class LoadBundle extends TagHandler {

    // Variables ------------------------------------------------------------------------------------------------------

    private String varValue;
    private TagAttribute basenameAttribute;
    private TagAttribute loaderAttribute;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * The tag constructor.
     * @param config The tag config.
     */
    public LoadBundle(TagConfig config) {
        super(config);
        varValue = getStringLiteral(getRequiredAttribute("var"), "var");
        basenameAttribute = getRequiredAttribute("basename");
        loaderAttribute = getAttribute("loader");
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * First obtain the resource bundle by its name as specified in the <code>basename</code> attribute with the locale
     * which is obtained as specified in {@link Faces#getLocale()} and the class loader which is obtained as specified
     * in {@link Utils#getClassLoader(Object)} with the <code>loader</code> attribute as argument. Finally set the
     * resource bundle in the request scope by the name as specified in the <code>var</code> attribute.
     */
    @Override
    public void apply(FaceletContext context, UIComponent parent) throws IOException {
        ResourceBundle bundle;

        try {
            String basename = basenameAttribute.getValue(context);
            Locale locale = getLocale(context, parent);
            ClassLoader classLoader = getClassLoader(context, loaderAttribute);
            bundle = ResourceBundle.getBundle(basename, locale, classLoader);
        }
        catch (Exception e) {
            throw new TagAttributeException(tag, basenameAttribute, e);
        }

        context.getFacesContext().getExternalContext().getRequestMap().put(varValue, new BundleMap(bundle));
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Returns the locale associated with the given component conform {@link Faces#getLocale()}.
     * @param context The involved facelet context.
     * @param component The component to find the locale in.
     * @return The locale associated with the given component.
     */
    private static Locale getLocale(FaceletContext context, UIComponent component) {
        UIViewRoot view = (component instanceof UIViewRoot) ? (UIViewRoot) component : getClosestParent(component, UIViewRoot.class);

        if (view != null) {
            Locale locale = view.getLocale();

            if (locale != null) {
                return locale;
            }
        }

        return FacesLocal.getLocale(context.getFacesContext());
    }

    // Nested classes -------------------------------------------------------------------------------------------------

    /**
     * Specific map implementation which wraps the given resource bundle in {@link Collections#unmodifiableMap(Map)} and
     * returns {@code ???key???} in {@link BundleMap#get(Object)} method when the key doesn't exist at all.
     *
     * @author Bauke Scholtz
     */
    private static class BundleMap extends MapWrapper<String, String> {

        private static final long serialVersionUID = 1L;

        private transient ResourceBundle bundle;

        public BundleMap(ResourceBundle bundle) {
            super(list(bundle.getKeys()).stream().collect(toUnmodifiableMap(identity(), bundle::getString)));
            this.bundle = bundle;
        }

        @Override
        public String get(Object key) {
            if (!containsKey(key)) {
                return "???" + key + "???";
            }

            return super.get(key);
        }

        @Override
        public boolean equals(Object object) {
            return super.equals(object) && bundle.equals(((BundleMap) object).bundle);
        }

        @Override
        public int hashCode() {
            return super.hashCode() + bundle.hashCode();
        }

    }

}
