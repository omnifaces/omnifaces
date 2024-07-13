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
package org.omnifaces.util;

import static jakarta.faces.view.facelets.FaceletContext.FACELET_CONTEXT_KEY;
import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.ofNullable;
import static java.util.logging.Level.FINEST;
import static org.omnifaces.exceptionhandler.ViewExpiredExceptionHandler.FLASH_ATTRIBUTE_VIEW_EXPIRED;
import static org.omnifaces.util.Beans.getReference;
import static org.omnifaces.util.Components.findComponentsInChildren;
import static org.omnifaces.util.Reflection.instance;
import static org.omnifaces.util.Reflection.toClassOrNull;
import static org.omnifaces.util.Servlets.addParamToMapIfNecessary;
import static org.omnifaces.util.Servlets.formatContentDispositionHeader;
import static org.omnifaces.util.Servlets.isSecure;
import static org.omnifaces.util.Servlets.prepareRedirectURL;
import static org.omnifaces.util.Servlets.toQueryString;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.encodeURL;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.faces.FacesException;
import jakarta.faces.FacesWrapper;
import jakarta.faces.FactoryFinder;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.application.Resource;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.UIViewParameter;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.event.PhaseId;
import jakarta.faces.lifecycle.Lifecycle;
import jakarta.faces.render.RenderKit;
import jakarta.faces.render.RenderKitFactory;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.view.ViewDeclarationLanguage;
import jakarta.faces.view.ViewMetadata;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import org.omnifaces.component.ParamHolder;
import org.omnifaces.component.input.HashParam;
import org.omnifaces.component.input.ScriptParam;
import org.omnifaces.config.FacesConfigXml;
import org.omnifaces.context.OmniPartialViewContext;
import org.omnifaces.resourcehandler.ResourceIdentifier;

/**
 * <p>
 * Collection of utility methods for the Faces API that are mainly shortcuts for obtaining stuff from the provided
 * {@link FacesContext} argument. In effect, it 'flattens' the hierarchy of nested objects.
 * <p>
 * The difference with {@link Faces} is that no one method of {@link FacesLocal} obtains the {@link FacesContext} from
 * the current thread by {@link FacesContext#getCurrentInstance()}. This job is up to the caller. This is more efficient
 * in situations where multiple utility methods needs to be called at the same time. Invoking
 * {@link FacesContext#getCurrentInstance()} is at its own an extremely cheap operation, however as it's to be obtained
 * as a {@link ThreadLocal} variable, it's during the call still blocking all other running threads for some nanoseconds
 * or so.
 * <p>
 * Note that methods which are <strong>directly</strong> available on {@link FacesContext} instance itself, such as
 * {@link FacesContext#getExternalContext()}, {@link FacesContext#getViewRoot()},
 * {@link FacesContext#isValidationFailed()}, etc are not delegated by the this utility class, because it would design
 * technically not make any sense to delegate a single-depth method call like follows:
 * <pre>
 * ExternalContext externalContext = FacesLocal.getExternalContext(facesContext);
 * </pre>
 * <p>
 * instead of just calling it directly like follows:
 * <pre>
 * ExternalContext externalContext = facesContext.getExternalContext();
 * </pre>
 *
 * <h2>Usage</h2>
 * <p>
 * Here are <strong>some</strong> examples:
 * <pre>
 * FacesContext context = Faces.getContext();
 * User user = FacesLocal.getSessionAttribute(context, "user");
 * Item item = FacesLocal.evaluateExpressionGet(context, "#{item}");
 * String cookieValue = FacesLocal.getRequestCookie(context, "cookieName");
 * List&lt;Locale&gt; supportedLocales = FacesLocal.getSupportedLocales(context);
 * FacesLocal.invalidateSession(context);
 * FacesLocal.redirect(context, "login.xhtml");
 * </pre>
 * <p>
 * For a full list, check the <a href="#method.summary">method summary</a>.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @since 1.6
 * @see Servlets
 */
public final class FacesLocal {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(FacesLocal.class.getName());

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final int DEFAULT_SENDFILE_BUFFER_SIZE = 10240;
    private static final String ERROR_NO_VIEW = "There is no view.";

    // Lazy loaded properties (will only be initialized when FacesContext is available) -------------------------------

    private static Boolean extensionlessMappingEnabled;
    private static String faceletsSuffix;

    // Constructors ---------------------------------------------------------------------------------------------------

    private FacesLocal() {
        // Hide constructor.
    }

    // Lazy init ------------------------------------------------------------------------------------------------------

    private static boolean isExtensionlessMappingEnabled(FacesContext context) {
        if (extensionlessMappingEnabled == null) {
            extensionlessMappingEnabled = parseBoolean(getInitParameter(context, "jakarta.faces.AUTOMATIC_EXTENSIONLESS_MAPPING"));
        }

        return extensionlessMappingEnabled;
    }

    private static String getFaceletsSuffix(FacesContext context) {
        if (faceletsSuffix == null) {
            faceletsSuffix = coalesce(getInitParameter(context, ViewHandler.FACELETS_SUFFIX_PARAM_NAME), ViewHandler.DEFAULT_FACELETS_SUFFIX);
        }

        return faceletsSuffix;
    }

    // Faces general --------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getPackage()
     */
    @SuppressWarnings("unchecked")
    public static Package getPackage(FacesContext context) {
        if (context != null) {
            var unwrappedContext = context;

            while (unwrappedContext instanceof FacesWrapper) {
                unwrappedContext = ((FacesWrapper<FacesContext>) unwrappedContext).getWrapped();
            }

            return unwrappedContext.getClass().getPackage();
        }
        else {
            return FacesContext.class.getPackage();
        }
    }

    /**
     * @see Faces#getImplInfo()
     */
    public static String getImplInfo(FacesContext context) {
        var facesPackage = getPackage(context);
        return facesPackage.getImplementationTitle() + " " + facesPackage.getImplementationVersion();
    }

    /**
     * @see Faces#getServerInfo()
     */
    public static String getServerInfo(FacesContext context) {
        return getServletContext(context).getServerInfo();
    }

    /**
     * @see Faces#getProjectStage()
     */
    public static ProjectStage getProjectStage(FacesContext context) {
        return context.getApplication().getProjectStage();
    }

    /**
     * @see Faces#isDevelopment()
     */
    public static boolean isDevelopment(FacesContext context) {
        return getProjectStage(context) == ProjectStage.Development;
    }

    /**
     * @see Faces#isSystemTest()
     */
    public static boolean isSystemTest(FacesContext context) {
        return getProjectStage(context) == ProjectStage.SystemTest;
    }

    /**
     * @see Faces#isProduction()
     */
    public static boolean isProduction(FacesContext context) {
        return getProjectStage(context) == ProjectStage.Production;
    }

    /**
     * @see Faces#getMapping()
     */
    public static String getMapping(FacesContext context) {
        var externalContext = context.getExternalContext();

        if (externalContext.getRequestPathInfo() == null) {
            var path = externalContext.getRequestServletPath();
            var suffixPos = path.lastIndexOf('.');

            if (suffixPos > -1) {
                return path.substring(suffixPos);
            }
            else if (isExtensionlessMappingEnabled(context)) {
                return getFaceletsSuffix(context);
            }
            else {
                throw new IllegalStateException();
            }
        }
        else {
            return externalContext.getRequestServletPath();
        }
    }

    /**
     * @see Faces#isPrefixMapping()
     */
    public static boolean isPrefixMapping(FacesContext context) {
        return Faces.isPrefixMapping(getMapping(context));
    }

    /**
     * @see Faces#evaluateExpressionGet(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T evaluateExpressionGet(FacesContext context, String expression) {
        if (isEmpty(expression)) {
            return null;
        }

        return (T) context.getApplication().evaluateExpressionGet(context, expression, Object.class);
    }

    /**
     * @see Faces#evaluateExpressionSet(String, Object)
     */
    public static void evaluateExpressionSet(FacesContext context, String expression, Object value) {
        var elContext = context.getELContext();
        var valueExpression = context.getApplication().getExpressionFactory()
            .createValueExpression(elContext, expression, Object.class);
        valueExpression.setValue(elContext, value);
    }

    /**
     * @see Faces#resolveExpressionGet(Object, String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T resolveExpressionGet(FacesContext context, Object base, String property) {
        var elResolver = context.getApplication().getELResolver();
        return (T) elResolver.getValue(context.getELContext(), base, property);
    }

    /**
     * @see Faces#resolveExpressionSet(Object, String, Object)
     */
    public static void resolveExpressionSet(FacesContext context, Object base, String property, Object value) {
        var elResolver = context.getApplication().getELResolver();
        elResolver.setValue(context.getELContext(), base, property, value);
    }

    /**
     * @see Faces#getContextAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getContextAttribute(FacesContext context, String name) {
        return (T) context.getAttributes().get(name);
    }


    /**
     * @see Faces#getContextAttribute(String, Supplier)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getContextAttribute(FacesContext context, String name, Supplier<T> computeIfAbsent) {
        var value = getContextAttribute(context, name);

        if (value == null) {
            value = computeIfAbsent.get();
            setContextAttribute(context, name, value);
        }

        return (T) value;
    }

    /**
     * @see Faces#setContextAttribute(String, Object)
     */
    public static void setContextAttribute(FacesContext context, String name, Object value) {
        context.getAttributes().put(name, value);
    }

    /**
     * @see Faces#createConverter(Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> Converter<T> createConverter(FacesContext context, Object identifier) {
        if (identifier instanceof String converterId) {
            return createConverter(context, converterId);
        }
        else if (identifier instanceof Class<?> converterClass) {
            return createConverter(context, converterClass);
        }
        else if (identifier instanceof Converter<?> converterInstance) {
            return (Converter<T>) converterInstance;
        }
        else {
            return null;
        }
    }

    /**
     * @see Faces#createConverter(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> Converter<T> createConverter(FacesContext context, String identifier) {
        Converter<T> converter = context.getApplication().createConverter(identifier);

        if (converter == null) {
            converter = createConverter(context, toClassOrNull(identifier));
        }

        return converter;
    }

    /**
     * @see Faces#createConverter(Class)
     */
    @SuppressWarnings("unchecked")
    public static <T> Converter<T> createConverter(FacesContext context, Class<?> identifier) {
        if (Converter.class.isAssignableFrom(identifier)) {
            var annotation = identifier.getAnnotation(FacesConverter.class);

            if (annotation != null) {
                return (Converter<T>) getReference(identifier, annotation);
            }
            else {
                return (Converter<T>) instance(identifier);
            }
        }
        else {
            return context.getApplication().createConverter(identifier);
        }
    }

    /**
     * @see Faces#createValidator(Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> Validator<T> createValidator(FacesContext context, Object identifier) {
        if (identifier instanceof String validatorId) {
            return createValidator(context, validatorId);
        }
        else if (identifier instanceof Class<?> validatorClass) {
            return createValidator(context, validatorClass);
        }
        else if (identifier instanceof Validator<?> validatorInstance) {
            return (Validator<T>) validatorInstance;
        }
        else {
            return null;
        }
    }

    /**
     * @see Faces#createValidator(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> Validator<T> createValidator(FacesContext context, String identifier) {
        Validator<T> validator = context.getApplication().createValidator(identifier);

        if (validator == null) {
            validator = createValidator(context, toClassOrNull(identifier));
        }

        return validator;
    }

    /**
     * @see Faces#createValidator(Class)
     */
    @SuppressWarnings({ "unchecked", "unused" })
    public static <T> Validator<T> createValidator(FacesContext context, Class<?> identifier) {
        if (Validator.class.isAssignableFrom(identifier)) {
            var annotation = identifier.getAnnotation(FacesValidator.class);

            if (annotation != null) {
                return (Validator<T>) getReference(identifier, annotation);
            }
            else {
                return (Validator<T>) instance(identifier);
            }
        }
        else {
            return null;
        }
    }

    /**
     * @see Faces#createResource(String)
     */
    public static Resource createResource(FacesContext context, String resourceName) {
        return context.getApplication().getResourceHandler().createResource(resourceName);
    }

    /**
     * @see Faces#createResource(String, String)
     */
    public static Resource createResource(FacesContext context, String libraryName, String resourceName) {
        return context.getApplication().getResourceHandler().createResource(resourceName, libraryName);
    }

    /**
     * @see Faces#createResource(ResourceIdentifier)
     */
    public static Resource createResource(FacesContext context, ResourceIdentifier resourceIdentifier) {
        return context.getApplication().getResourceHandler().createResource(resourceIdentifier.getName(), resourceIdentifier.getLibrary());
    }

    /**
     * @see Faces#getLifecycle()
     */
    public static Lifecycle getLifecycle(FacesContext context) {
        return Servlets.getFacesLifecycle(getServletContext(context));
    }

    // Faces views ------------------------------------------------------------------------------------------------------

    /**
     * @see Faces#setViewRoot(String)
     */
    public static void setViewRoot(FacesContext context, String viewId) {
        context.setViewRoot(context.getApplication().getViewHandler().createView(context, viewId));
    }

    /**
     * @see Faces#getViewId()
     */
    public static String getViewId(FacesContext context) {
        var viewRoot = context.getViewRoot();
        return viewRoot != null ? viewRoot.getViewId() : null;
    }

    /**
     * @see Faces#getViewIdWithParameters()
     */
    public static String getViewIdWithParameters(FacesContext context) {
        var viewId = coalesce(getViewId(context), "");
        var viewParameters = toQueryString(getViewParameterMap(context));
        var hashParameters = getHashQueryString(context);
        return (viewParameters == null ? viewId : viewId + "?" + viewParameters) + (hashParameters == null ? "" : "#" + hashParameters);
    }

    /**
     * @see Faces#getViewName()
     */
    public static String getViewName(FacesContext context) {
        var viewId = getViewId(context);
        return viewId != null ? viewId.substring(viewId.lastIndexOf('/') + 1).split("\\.")[0] : null;
    }

    /**
     * @see Faces#getViewDeclarationLanguage()
     */
    public static ViewDeclarationLanguage getViewDeclarationLanguage(FacesContext context) {
        return context.getApplication().getViewHandler().getViewDeclarationLanguage(context, context.getViewRoot().getViewId());
    }

    /**
     * @see Faces#getRenderKit()
     */
    public static RenderKit getRenderKit(FacesContext context) {
        String renderKitId = null;
        var view = context.getViewRoot();

        if (view != null) {
            renderKitId = view.getRenderKitId();
        }

        if (renderKitId == null) {
            var application = context.getApplication();
            var viewHandler = application.getViewHandler();

            if (viewHandler != null) {
                renderKitId = viewHandler.calculateRenderKitId(context);
            }

            if (renderKitId == null) {
                renderKitId = application.getDefaultRenderKitId();

                if (renderKitId == null) {
                    renderKitId = RenderKitFactory.HTML_BASIC_RENDER_KIT;
                }
            }
        }

        return ((RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY)).getRenderKit(context, renderKitId);
    }

    /**
     * @see Faces#normalizeViewId(String)
     */
    public static String normalizeViewId(FacesContext context, String path) {
        var mapping = getMapping(context);

        if (Faces.isPrefixMapping(mapping)) {
            if (path.startsWith(mapping)) {
                return path.substring(mapping.length());
            }
        }
        else if (path.endsWith(mapping)) {
            return path.substring(0, path.lastIndexOf('.')) + getFaceletsSuffix(context);
        }

        return path;
    }

    /**
     * @see Faces#getViewParameters()
     */
    public static Collection<UIViewParameter> getViewParameters(FacesContext context) {
        var viewRoot = context.getViewRoot();
        return viewRoot != null ? ViewMetadata.getViewParameters(viewRoot) : Collections.<UIViewParameter>emptyList();
    }

    /**
     * @see Faces#getViewParameterMap()
     */
    public static Map<String, List<String>> getViewParameterMap(FacesContext context) {
        var viewParameters = getViewParameters(context);

        if (viewParameters.isEmpty()) {
            return new LinkedHashMap<>(0);
        }

        Map<String, List<String>> parameterMap = new LinkedHashMap<>(viewParameters.size());

        for (UIViewParameter viewParameter : viewParameters) {
            var value = viewParameter.getStringValue(context);

            if (value != null) {
                // <f:viewParam> doesn't support multiple values anyway, so having multiple <f:viewParam> on the
                // same request parameter shouldn't end up in repeated parameters in action URL.
                parameterMap.put(viewParameter.getName(), asList(value));
            }
        }

        return parameterMap;
    }

    /**
     * @see Faces#getHashParameters()
     */
    public static Collection<HashParam> getHashParameters(FacesContext context) {
        var viewRoot = context.getViewRoot();
        return viewRoot != null ? findComponentsInChildren(Hacks.getMetadataFacet(viewRoot), HashParam.class) : Collections.<HashParam>emptyList();
    }

    /**
     * @see Faces#getHashParameterMap()
     */
    public static Map<String, List<String>> getHashParameterMap(FacesContext context) {
        var hashParameters = getHashParameters(context);

        if (hashParameters.isEmpty()) {
            return new LinkedHashMap<>(0);
        }

        Map<String, List<String>> parameterMap = new LinkedHashMap<>(hashParameters.size());

        for (HashParam hashParameter : hashParameters) {
            if (isEmpty(hashParameter.getName())) {
                continue;
            }

            var value = hashParameter.getRenderedValue(context);

            if (!isEmpty(value)) {
                // <o:hashParam> doesn't support multiple values anyway, so having multiple <o:hashParam> on the
                // same request parameter shouldn't end up in repeated parameters in action URL.
                parameterMap.put(hashParameter.getName(), asList(value));
            }
        }

        return parameterMap;
    }

    /**
     * See {@link Faces#getHashQueryString()}
     */
    public static String getHashQueryString(FacesContext context) {
        return toQueryString(getHashParameterMap(context));
    }

    /**
     * @see Faces#getScriptParameters()
     */
    public static Collection<ScriptParam> getScriptParameters(FacesContext context) {
        var viewRoot = context.getViewRoot();
        return viewRoot != null ? findComponentsInChildren(Hacks.getMetadataFacet(viewRoot), ScriptParam.class) : Collections.<ScriptParam>emptyList();
    }

    /**
     * @see Faces#getMetadataAttributes(String)
     */
    public static Map<String, Object> getMetadataAttributes(FacesContext context, String viewId) {
        var viewHandler = context.getApplication().getViewHandler();
        var vdl = viewHandler.getViewDeclarationLanguage(context, viewId);
        var metadata = vdl.getViewMetadata(context, viewId);

        return metadata != null
            ? metadata.createMetadataView(context).getAttributes()
            : Collections.<String, Object>emptyMap();
    }

    /**
     * @see Faces#getMetadataAttributes()
     */
    public static Map<String, Object> getMetadataAttributes(FacesContext context) {
        return context.getViewRoot().getAttributes();
    }

    /**
     * @see Faces#getMetadataAttribute(String, String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getMetadataAttribute(FacesContext context, String viewId, String name) {
        return (T) getMetadataAttributes(context, viewId).get(name);
    }

    /**
     * @see Faces#getMetadataAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getMetadataAttribute(FacesContext context, String name) {
        return (T) getMetadataAttributes(context).get(name);
    }

    /**
     * @see Faces#getLocale()
     */
    public static Locale getLocale(FacesContext context) {
        Locale locale = null;

        if (context != null) {
            var viewRoot = context.getViewRoot();

            // Prefer the locale set in the view.
            if (viewRoot != null) {
                locale = viewRoot.getLocale();
            }

            // Then the client preferred locale.
            if (locale == null) {
                var clientLocale = context.getExternalContext().getRequestLocale();

                if (getSupportedLocales(context).contains(clientLocale)) {
                    locale = clientLocale;
                }
            }

            // Then the Faces default locale.
            if (locale == null) {
                locale = context.getApplication().getDefaultLocale();
            }
        }

        // Finally the system default locale.
        if (locale == null) {
            locale = Locale.getDefault();
        }

        return locale;
    }

    /**
     * @see Faces#getDefaultLocale()
     */
    public static Locale getDefaultLocale(FacesContext context) {
        return context.getApplication().getDefaultLocale();
    }

    /**
     * @see Faces#getSupportedLocales()
     */
    public static List<Locale> getSupportedLocales(FacesContext context) {
        var application = context.getApplication();
        List<Locale> supportedLocales = new ArrayList<>();
        var defaultLocale = application.getDefaultLocale();

        if (defaultLocale != null) {
            supportedLocales.add(defaultLocale);
        }

        for (var iter = application.getSupportedLocales(); iter.hasNext();) {
            var supportedLocale = iter.next();

            if (!supportedLocale.equals(defaultLocale)) {
                supportedLocales.add(supportedLocale);
            }
        }

        return supportedLocales;
    }

    /**
     * @see Faces#setLocale(Locale)
     */
    public static void setLocale(FacesContext context, Locale locale) {
        var viewRoot = context.getViewRoot();

        if (viewRoot == null) {
            throw new IllegalStateException(ERROR_NO_VIEW);
        }

        viewRoot.setLocale(locale);
    }

    /**
     * @see Faces#getMessageBundle()
     */
    public static ResourceBundle getMessageBundle(FacesContext context) {
        var messageBundle = context.getApplication().getMessageBundle();

        if (messageBundle == null) {
            return null;
        }

        return ResourceBundle.getBundle(messageBundle, getLocale(context));
    }

    /**
     * @see Faces#getResourceBundle(String)
     */
    public static ResourceBundle getResourceBundle(FacesContext context, String varName) {
        return context.getApplication().getResourceBundle(context, varName);
    }

    /**
     * @see Faces#getResourceBundles()
     */
    public static Map<String, ResourceBundle> getResourceBundles(FacesContext context) {
        var resourceBundles = FacesConfigXml.instance().getResourceBundles();
        Map<String, ResourceBundle> map = new HashMap<>(resourceBundles.size());

        for (String varName : resourceBundles.keySet()) {
            map.put(varName, getResourceBundle(context, varName));
        }

        return map;
    }

    /**
     * @see Faces#getBundleString(String)
     */
    public static String getBundleString(FacesContext context, String key) {
        for (ResourceBundle bundle : getResourceBundles(context).values()) {
            try {
                return bundle.getString(key);
            }
            catch (MissingResourceException ignore) {
                logger.log(FINEST, "Ignoring thrown exception; there is a fallback anyway.", ignore);
            }
        }

        return "???" + key + "???";
    }

    /**
     * @see Faces#navigate(String)
     */
    public static void navigate(FacesContext context, String outcome) {
        context.getApplication().getNavigationHandler().handleNavigation(context, null, outcome);
    }

    /**
     * @see Faces#getBookmarkableURL(Map, boolean)
     */
    public static String getBookmarkableURL
        (FacesContext context, Map<String, List<String>> params, boolean includeViewParams)
    {
        var viewId = getViewId(context);

        if (viewId == null) {
            throw new IllegalStateException(ERROR_NO_VIEW);
        }

        return getBookmarkableURL(context, viewId, params, includeViewParams);
    }

    /**
     * @see Faces#getBookmarkableURL(String, Map, boolean)
     */
    public static String getBookmarkableURL
        (FacesContext context, String viewId, Map<String, List<String>> params, boolean includeViewParams)
    {
        Map<String, List<String>> map = new HashMap<>();

        if (params != null) {
            for (Entry<String, List<String>> param : params.entrySet()) {
                for (String value : param.getValue()) {
                    addParamToMapIfNecessary(map, param.getKey(), value);
                }
            }
        }

        return context.getApplication().getViewHandler().getBookmarkableURL(context, viewId, map, includeViewParams);
    }

    /**
     * @see Faces#getBookmarkableURL(Collection, boolean)
     */
    public static String getBookmarkableURL
        (FacesContext context, Collection<? extends ParamHolder<?>> params, boolean includeViewParams)
    {
        var viewId = getViewId(context);

        if (viewId == null) {
            throw new IllegalStateException(ERROR_NO_VIEW);
        }

        return getBookmarkableURL(context, viewId, params, includeViewParams);
    }

    /**
     * @see Faces#getBookmarkableURL(String, Collection, boolean)
     */
    public static String getBookmarkableURL
        (FacesContext context, String viewId, Collection<? extends ParamHolder<?>> params, boolean includeViewParams)
    {
        Map<String, List<String>> map = new HashMap<>();

        if (params != null) {
            for (ParamHolder<?> param : params) {
                addParamToMapIfNecessary(map, param.getName(), param.getValue());
            }
        }

        return context.getApplication().getViewHandler().getBookmarkableURL(context, viewId, map, includeViewParams);
    }

    /**
     * @see Faces#isOutputHtml5Doctype()
     */
    public static boolean isOutputHtml5Doctype(FacesContext context) {
        var viewRoot = context.getViewRoot();

        if (viewRoot == null) {
            return false; // Can happen when view build time hasn't started yet.
        }

        var doctype = viewRoot.getDoctype();

        if (doctype == null) {
            return false; // Can happen when there is no doctype declared at all.
        }

        return "html".equalsIgnoreCase(doctype.getRootElement()) && doctype.getPublic() == null && doctype.getSystem() == null;
    }

    // Facelets -------------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getFaceletContext()
     */
    public static FaceletContext getFaceletContext(FacesContext context) {
        var faceletContext = (FaceletContext) getContextAttribute(context, FACELET_CONTEXT_KEY);

        if (faceletContext != null) {
            return faceletContext;
        }

        throw new IllegalStateException(ERROR_NO_VIEW);
    }

    /**
     * @see Faces#getFaceletAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFaceletAttribute(FacesContext context, String name) {
        return (T) getFaceletContext(context).getAttribute(name);
    }

    /**
     * @see Faces#setFaceletAttribute(String, Object)
     */
    public static void setFaceletAttribute(FacesContext context, String name, Object value) {
        getFaceletContext(context).setAttribute(name, value);
    }

    // HTTP request ---------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getRequest()
     */
    public static HttpServletRequest getRequest(FacesContext context) {
        return (HttpServletRequest) context.getExternalContext().getRequest();
    }

    /**
     * @see Faces#isAjaxRequest()
     */
    public static boolean isAjaxRequest(FacesContext context) {
        return context.getPartialViewContext().isAjaxRequest();
    }

    /**
     * @see Faces#isAjaxRequestWithPartialRendering()
     */
    public static boolean isAjaxRequestWithPartialRendering(FacesContext context) {
        var pvc = context.getPartialViewContext();
        return pvc.isAjaxRequest() && !pvc.isRenderAll();
    }

    /**
     * @see Faces#isPostback()
     */
    public static boolean isPostback(FacesContext context) {
        return "POST".equalsIgnoreCase(getRequest(context).getMethod()) && context.isPostback();
    }

    /**
     * @see Faces#getRequestParameterMap()
     */
    public static Map<String, String> getRequestParameterMap(FacesContext context) {
        return context.getExternalContext().getRequestParameterMap();
    }

    /**
     * @see Faces#getMutableRequestParameterMap()
     */
    public static Map<String, List<String>> getMutableRequestParameterMap(FacesContext context) {
        return Servlets.getMutableRequestParameterMap(getRequest(context));
    }

    /**
     * @see Faces#getRequestParameter(String)
     */
    public static String getRequestParameter(FacesContext context, String name) {
        return getRequestParameterMap(context).get(name);
    }

    /**
     * @see Faces#getRequestParameter(String, Class)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getRequestParameter(FacesContext context, String name, Class<T> type) {
        return getRequestParameter(context, name, value -> (T) ofNullable(createConverter(context, type)).map(c -> c.getAsObject(context, context.getViewRoot(), value)).orElse(value));
    }

    /**
     * @see Faces#getRequestParameter(String, Function)
     */
    public static <T> T getRequestParameter(FacesContext context, String name, Function<String, T> converter) {
        return getRequestParameter(context, name, converter, () -> null);
    }

    /**
     * @see Faces#getRequestParameter(String, Function, Supplier)
     */
    public static <T> T getRequestParameter(FacesContext context, String name, Function<String, T> converter, Supplier<T> defaultValue) {
        return ofNullable(getRequestParameter(context, name)).filter(value -> !isEmpty(value)).map(converter).orElseGet(defaultValue);
    }

    /**
     * @see Faces#getRequestParameterValuesMap()
     */
    public static Map<String, String[]> getRequestParameterValuesMap(FacesContext context) {
        return context.getExternalContext().getRequestParameterValuesMap();
    }

    /**
     * @see Faces#getRequestParameterValues(String)
     */
    public static String[] getRequestParameterValues(FacesContext context, String name) {
        return getRequestParameterValuesMap(context).get(name);
    }

    /**
     * @see Faces#getRequestParameterValues(String, Class)
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] getRequestParameterValues(FacesContext context, String name, Class<T> type) {
        var values = getRequestParameterValues(context, name);

        if (values == null) {
            return null;
        }

        Converter<T> converter = createConverter(context, type);

        if (converter == null) {
            return (T[]) values;
        }

        var convertedValues = Array.newInstance(type, values.length);

        for (var i = 0; i < values.length; i++) {
            Array.set(convertedValues, i, converter.getAsObject(context, context.getViewRoot(), values[i]));
        }

        return (T[]) convertedValues;
    }

    /**
     * @see Faces#getRequestParts()
     */
    public static Collection<Part> getRequestParts(FacesContext context) {
        try {
            return getRequest(context).getParts();
        }
        catch (IOException | ServletException e) {
            throw new FacesException(e);
        }
    }

    /**
     * @see Faces#getRequestPart(String)
     */
    public static Part getRequestPart(FacesContext context, String name) {
        try {
            return getRequest(context).getPart(name);
        }
        catch (ServletException | IOException e) {
            throw new FacesException(e);
        }
    }

    /**
     * @see Faces#getRequestParts(String)
     */
    public static Collection<Part> getRequestParts(FacesContext context, String name) {
        try {
            List<Part> parts = new ArrayList<>();

            for (Part part : getRequest(context).getParts()) {
                if (name.equals(part.getName())) {
                    parts.add(part);
                }
            }

            return unmodifiableList(parts);
        }
        catch (ServletException | IOException e) {
            throw new FacesException(e);
        }
    }

    /**
     * @see Faces#getRequestHeaderMap()
     */
    public static Map<String, String> getRequestHeaderMap(FacesContext context) {
        return context.getExternalContext().getRequestHeaderMap();
    }

    /**
     * @see Faces#getMutableRequestHeaderMap()
     */
    public static Map<String, List<String>> getMutableRequestHeaderMap(FacesContext context) {
        return Servlets.getMutableRequestHeaderMap(getRequest(context));
    }

    /**
     * @see Faces#getRequestHeader(String)
     */
    public static String getRequestHeader(FacesContext context, String name) {
        return getRequestHeaderMap(context).get(name);
    }

    /**
     * @see Faces#getRequestHeaderValuesMap()
     */
    public static Map<String, String[]> getRequestHeaderValuesMap(FacesContext context) {
        return context.getExternalContext().getRequestHeaderValuesMap();
    }

    /**
     * @see Faces#getRequestHeaderValues(String)
     */
    public static String[] getRequestHeaderValues(FacesContext context, String name) {
        return getRequestHeaderValuesMap(context).get(name);
    }

    /**
     * @see Faces#getRequestContextPath()
     */
    public static String getRequestContextPath(FacesContext context) {
        return context.getExternalContext().getRequestContextPath();
    }

    /**
     * @see Faces#getRequestServletPath()
     */
    public static String getRequestServletPath(FacesContext context) {
        return context.getExternalContext().getRequestServletPath();
    }

    /**
     * @see Faces#getRequestPathInfo()
     */
    public static String getRequestPathInfo(FacesContext context) {
        return Servlets.getRequestPathInfo(getRequest(context));
    }

    /**
     * @see Faces#getRequestHostname()
     */
    public static String getRequestHostname(FacesContext context) {
        return Servlets.getRequestHostname(getRequest(context));
    }

    /**
     * @see Faces#getRequestBaseURL()
     */
    public static String getRequestBaseURL(FacesContext context) {
        return Servlets.getRequestBaseURL(getRequest(context));
    }

    /**
     * @see Faces#getRequestDomainURL()
     */
    public static String getRequestDomainURL(FacesContext context) {
        return Servlets.getRequestDomainURL(getRequest(context));
    }

    /**
     * @see Faces#getRequestURL()
     */
    public static String getRequestURL(FacesContext context) {
        return Servlets.getRequestURL(getRequest(context));
    }

    /**
     * @see Faces#getRequestURI()
     */
    public static String getRequestURI(FacesContext context) {
        return Servlets.getRequestURI(getRequest(context));
    }

    /**
     * @see Faces#getRequestQueryString()
     */
    public static String getRequestQueryString(FacesContext context) {
        return Servlets.getRequestQueryString(getRequest(context));
    }

    /**
     * @see Faces#getRequestQueryStringMap()
     */
    public static Map<String, List<String>> getRequestQueryStringMap(FacesContext context) {
        return Servlets.getRequestQueryStringMap(getRequest(context));
    }

    /**
     * @see Faces#getRequestURLWithQueryString()
     */
    public static String getRequestURLWithQueryString(FacesContext context) {
        return Servlets.getRequestURLWithQueryString(getRequest(context));
    }

    /**
     * @see Faces#getRequestURIWithQueryString()
     */
    public static String getRequestURIWithQueryString(FacesContext context) {
        return Servlets.getRequestURIWithQueryString(getRequest(context));
    }

    /**
     * @see Faces#getRemoteAddr()
     */
    public static String getRemoteAddr(FacesContext context) {
        return Servlets.getRemoteAddr(getRequest(context));
    }

    /**
     * @see Faces#getUserAgent()
     */
    public static String getUserAgent(FacesContext context) {
        return Servlets.getUserAgent(getRequest(context));
    }

    /**
     * @see Faces#getReferrer()
     */
    public static String getReferrer(FacesContext context) {
        return Servlets.getReferrer(getRequest(context));
    }

    /**
     * @see Faces#isRequestSecure()
     */
    public static boolean isRequestSecure(FacesContext context) {
        return Servlets.isSecure(getRequest(context));
    }

    // HTTP response --------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getResponse()
     */
    public static HttpServletResponse getResponse(FacesContext context) {
        return (HttpServletResponse) context.getExternalContext().getResponse();
    }

    /**
     * @see Faces#getResponseBufferSize()
     */
    public static int getResponseBufferSize(FacesContext context) {
        return context.getExternalContext().getResponseBufferSize();
    }

    /**
     * @see Faces#getResponseCharacterEncoding()
     */
    public static String getResponseCharacterEncoding(FacesContext context) {
        return context.getExternalContext().getResponseCharacterEncoding();
    }

    /**
     * @see Faces#setResponseStatus(int)
     */
    public static void setResponseStatus(FacesContext context, int status) {
        context.getExternalContext().setResponseStatus(status);
    }

    /**
     * @see Faces#redirect(String, Object...)
     */
    public static void redirect(FacesContext context, String url, Object... paramValues) {
        var externalContext = context.getExternalContext();
        externalContext.getFlash().setRedirect(true); // MyFaces also requires this for a redirect in current request (which is incorrect).
        externalContext.getFlash().keep(FLASH_ATTRIBUTE_VIEW_EXPIRED);

        try {
            externalContext.redirect(prepareRedirectURL(getRequest(context), url, paramValues));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @see Faces#redirectPermanent(String, Object...)
     */
    public static void redirectPermanent(FacesContext context, String url, Object... paramValues) {
        context.getExternalContext().getFlash().setRedirect(true); // MyFaces also requires this for a redirect in current request (which is incorrect).
        Servlets.redirectPermanent(getResponse(context), prepareRedirectURL(getRequest(context), url, paramValues));
        context.responseComplete();
    }

    /**
     * @see Faces#refresh()
     */
    public static void refresh(FacesContext context) {
        redirect(context, getRequestURI(context));
    }

    /**
     * @see Faces#refreshWithQueryString()
     */
    public static void refreshWithQueryString(FacesContext context) {
        redirect(context, getRequestURIWithQueryString(context));
    }

    /**
     * @see Faces#resetResponse()
     */
    public static void resetResponse(FacesContext context) {
        Servlets.resetResponse(getResponse(context));
        OmniPartialViewContext.getCurrentInstance(context).resetPartialResponse();
    }

    /**
     * @see Faces#responseSendError(int, String)
     */
    public static void responseSendError(FacesContext context, int status, String message) {
        try {
            context.getExternalContext().responseSendError(status, message);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        context.responseComplete();

        // Below is a workaround for disappearing FacesContext in WildFly/Undertow. It disappears because Undertow
        // immediately performs a forward to error page instead of waiting until servlet's service is finished. When
        // the error page is a Faces page as well, then it implicitly invokes FacesServlet once again, which in turn
        // creates another FacesContext which overrides the current FacesContext in the same thread! So, when the
        // FacesContext which is created during the forward is released, it leaves the current FacesContext as null,
        // causing NPE over all place which is relying on FacesContext#getCurrentInstance().
        // This is already fixed in WildFly 8.2 / Undertow 1.1.0 as per https://issues.jboss.org/browse/UNDERTOW-322.
        if (!Faces.hasContext()) {
            Faces.setContext(context);
        }
    }

    /**
     * @see Faces#addResponseHeader(String, String)
     */
    public static void addResponseHeader(FacesContext context, String name, String value) {
        context.getExternalContext().addResponseHeader(name, value);
    }

    /**
     * @see Faces#isResponseCommitted()
     */
    public static boolean isResponseCommitted(FacesContext context) {
        return context.getExternalContext().isResponseCommitted();
    }

    /**
     * @see Faces#responseReset()
     */
    public static void responseReset(FacesContext context) {
        context.getExternalContext().responseReset();
    }

    /**
     * @see Faces#isRenderResponse()
     */
    public static boolean isRenderResponse(FacesContext context) {
        return context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE;
    }

    // FORM based authentication --------------------------------------------------------------------------------------

    /**
     * @see Faces#login(String, String)
     */
    public static void login(FacesContext context, String username, String password) throws ServletException {
        getRequest(context).login(username, password);
    }

    /**
     * @see Faces#authenticate()
     */
    public static boolean authenticate(FacesContext context) throws ServletException {
        try {
            return getRequest(context).authenticate(getResponse(context));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @see Faces#isAuthenticated()
     */
    public static boolean isAuthenticated(FacesContext context) {
        return getRemoteUser(context) != null;
    }

    /**
     * @see Faces#logout()
     */
    public static void logout(FacesContext context) throws ServletException {
        getRequest(context).logout();
    }

    /**
     * @see Faces#getRemoteUser()
     */
    public static String getRemoteUser(FacesContext context) {
        return context.getExternalContext().getRemoteUser();
    }

    /**
     * @see Faces#isUserInRole(String)
     */
    public static boolean isUserInRole(FacesContext context, String role) {
        return context.getExternalContext().isUserInRole(role);
    }

    // HTTP cookies ---------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getRequestCookie(String)
     */
    public static String getRequestCookie(FacesContext context, String name) {
        var cookie = (Cookie) context.getExternalContext().getRequestCookieMap().get(name);
        return cookie != null ? Utils.decodeURL(cookie.getValue()) : null;
    }

    /**
     * @see Faces#addResponseCookie(String, String, int)
     */
    public static void addResponseCookie(FacesContext context, String name, String value, int maxAge) {
        addResponseCookie(context, name, value, null, null, maxAge);
    }

    /**
     * @see Faces#addResponseCookie(String, String, String, int)
     */
    public static void addResponseCookie(FacesContext context, String name, String value, String path, int maxAge) {
        addResponseCookie(context, name, value, null, path, maxAge, true);
    }

    /**
     * @see Faces#addResponseCookie(String, String, String, String, int)
     */
    public static void addResponseCookie(FacesContext context, String name, String value, String domain, String path, int maxAge) {
        addResponseCookie(context, name, value, domain, path, maxAge, true);
    }

    /**
     * @see Faces#addResponseCookie(String, String, String, String, int, boolean)
     */
    public static void addResponseCookie(FacesContext context, String name, String value, String domain, String path, int maxAge, boolean httpOnly) {
        addResponseCookie(context, name, value, domain, path, maxAge, httpOnly, null);
    }

    /**
     * @see Faces#addResponseCookie(String, String, String, String, int, boolean, Map)
     */
    public static void addResponseCookie(FacesContext context, String name, String value, String domain, String path, int maxAge, boolean httpOnly, Map<String, String> attributes) {
        var externalContext = context.getExternalContext();
        var properties = new HashMap<String, Object>();

        if (!"localhost".equals(domain)) { // Chrome doesn't like domain:"localhost" on cookies.
            properties.put("domain", domain == null ? getRequestHostname(context) : domain);
        }

        if (path != null) {
            properties.put("path", path);
        }

        properties.put("maxAge", maxAge);
        properties.put("httpOnly", httpOnly);
        properties.put("secure", isSecure((HttpServletRequest) externalContext.getRequest()));

        if (attributes != null) {
            properties.putAll(attributes);
        }

        externalContext.addResponseCookie(name, encodeURL(value), properties);
    }

    /**
     * @see Faces#removeResponseCookie(String, String)
     */
    public static void removeResponseCookie(FacesContext context, String name, String path) {
        addResponseCookie(context, name, null, path, 0);
    }

    // HTTP session ---------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getSession()
     */
    public static HttpSession getSession(FacesContext context) {
        return getSession(context, true);
    }

    /**
     * @see Faces#getSession(boolean)
     */
    public static HttpSession getSession(FacesContext context, boolean create) {
        return (HttpSession) context.getExternalContext().getSession(create);
    }

    /**
     * @see Faces#getSessionId()
     */
    public static String getSessionId(FacesContext context) {
        var session = getSession(context, false);
        return session != null ? session.getId() : null;
    }

    /**
     * @see Faces#invalidateSession()
     */
    public static void invalidateSession(FacesContext context) {
        context.getExternalContext().invalidateSession();
    }

    /**
     * @see Faces#hasSession()
     */
    public static boolean hasSession(FacesContext context) {
        return getSession(context, false) != null;
    }

    /**
     * @see Faces#isSessionNew()
     */
    public static boolean isSessionNew(FacesContext context) {
        var session = getSession(context, false);
        return session != null && session.isNew();
    }

    /**
     * @see Faces#isRequestedSessionExpired()
     */
    public static boolean isRequestedSessionExpired(FacesContext context) {
        var request = getRequest(context);
        return request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid();
    }

    /**
     * @see Faces#getSessionCreationTime()
     */
    public static long getSessionCreationTime(FacesContext context) {
        return getSession(context).getCreationTime();
    }

    /**
     * @see Faces#getSessionLastAccessedTime()
     */
    public static long getSessionLastAccessedTime(FacesContext context) {
        return getSession(context).getLastAccessedTime();
    }

    /**
     * @see Faces#getSessionMaxInactiveInterval()
     */
    public static int getSessionMaxInactiveInterval(FacesContext context) {
        return context.getExternalContext().getSessionMaxInactiveInterval();
    }

    /**
     * @see Faces#setSessionMaxInactiveInterval(int)
     */
    public static void setSessionMaxInactiveInterval(FacesContext context, int seconds) {
        context.getExternalContext().setSessionMaxInactiveInterval(seconds);
    }

    // Servlet context ------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getServletContext()
     */
    public static ServletContext getServletContext(FacesContext context) {
        return (ServletContext) context.getExternalContext().getContext();
    }

    /**
     * @see Faces#getInitParameterMap()
     */
    public static Map<String, String> getInitParameterMap(FacesContext context) {
        return context.getExternalContext().getInitParameterMap();
    }

    /**
     * @see Faces#getInitParameter(String)
     */
    public static String getInitParameter(FacesContext context, String name) {
        return context.getExternalContext().getInitParameter(name);
    }

    /**
     * @see Faces#getInitParameterOrDefault(String, String)
     */
    public static String getInitParameterOrDefault(FacesContext context, String name, String defaultValue) {
        return context.getExternalContext().getInitParameterMap().getOrDefault(name, defaultValue);
    }

    /**
     * @see Faces#getMimeType(String)
     */
    public static String getMimeType(FacesContext context, String name) {
        var mimeType = context.getExternalContext().getMimeType(name);

        if (mimeType == null) {
            mimeType = DEFAULT_MIME_TYPE;
        }

        return mimeType;
    }

    /**
     * @see Faces#getResource(String)
     */
    public static URL getResource(FacesContext context, String path) throws MalformedURLException {
        return context.getExternalContext().getResource(path);
    }

    /**
     * @see Faces#getResourceAsStream(String)
     */
    public static InputStream getResourceAsStream(FacesContext context, String path) {
        return context.getExternalContext().getResourceAsStream(path);
    }

    /**
     * @see Faces#getResourcePaths(String)
     */
    public static Set<String> getResourcePaths(FacesContext context, String path) {
        return context.getExternalContext().getResourcePaths(path);
    }

    /**
     * @see Faces#getRealPath(String)
     */
    public static String getRealPath(FacesContext context, String webContentPath) {
        return context.getExternalContext().getRealPath(webContentPath);
    }

    // Request scope --------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getRequestMap()
     */
    public static Map<String, Object> getRequestMap(FacesContext context) {
        return context.getExternalContext().getRequestMap();
    }

    /**
     * @see Faces#getRequestAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getRequestAttribute(FacesContext context, String name) {
        return (T) getRequestMap(context).get(name);
    }

    /**
     * @see Faces#getRequestAttribute(String, Supplier)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getRequestAttribute(FacesContext context, String name, Supplier<T> computeIfAbsent) {
        var value = getRequestAttribute(context, name);

        if (value == null) {
            value = computeIfAbsent.get();
            setRequestAttribute(context, name, value);
        }

        return (T) value;
    }

    /**
     * @see Faces#setRequestAttribute(String, Object)
     */
    public static void setRequestAttribute(FacesContext context, String name, Object value) {
        getRequestMap(context).put(name, value);
    }

    /**
     * @see Faces#removeRequestAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T removeRequestAttribute(FacesContext context, String name) {
        return (T) getRequestMap(context).remove(name);
    }

    // Flash scope ----------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getFlash()
     */
    public static Flash getFlash(FacesContext context) {
        return context.getExternalContext().getFlash();
    }

    /**
     * @see Faces#getFlashAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFlashAttribute(FacesContext context, String name) {
        return (T) getFlash(context).get(name);
    }

    /**
     * @see Faces#getFlashAttribute(String, Supplier)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFlashAttribute(FacesContext context, String name, Supplier<T> computeIfAbsent) {
        var value = getFlashAttribute(context, name);

        if (value == null) {
            value = computeIfAbsent.get();
            setFlashAttribute(context, name, value);
        }

        return (T) value;
    }

    /**
     * @see Faces#setFlashAttribute(String, Object)
     */
    public static void setFlashAttribute(FacesContext context, String name, Object value) {
        getFlash(context).put(name, value);
    }

    /**
     * @see Faces#removeFlashAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T removeFlashAttribute(FacesContext context, String name) {
        return (T) getFlash(context).remove(name);
    }

    // View scope -----------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getViewMap()
     */
    public static Map<String, Object> getViewMap(FacesContext context) {
        return context.getViewRoot().getViewMap();
    }

    /**
     * @see Faces#getViewAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getViewAttribute(FacesContext context, String name) {
        return (T) getViewMap(context).get(name);
    }

    /**
     * @see Faces#getViewAttribute(String, Supplier)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getViewAttribute(FacesContext context, String name, Supplier<T> computeIfAbsent) {
        var value = getViewAttribute(context, name);

        if (value == null) {
            value = computeIfAbsent.get();
            setViewAttribute(context, name, value);
        }

        return (T) value;
    }

    /**
     * @see Faces#setViewAttribute(String, Object)
     */
    public static void setViewAttribute(FacesContext context, String name, Object value) {
        getViewMap(context).put(name, value);
    }

    /**
     * @see Faces#removeViewAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T removeViewAttribute(FacesContext context, String name) {
        return (T) getViewMap(context).remove(name);
    }

    // Session scope --------------------------------------------------------------------------------------------------

    /**
     * @see Faces#getSessionMap()
     */
    public static Map<String, Object> getSessionMap(FacesContext context) {
        return context.getExternalContext().getSessionMap();
    }

    /**
     * @see Faces#getSessionAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSessionAttribute(FacesContext context, String name) {
        return (T) getSessionMap(context).get(name);
    }

    /**
     * @see Faces#getSessionAttribute(String, Supplier)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSessionAttribute(FacesContext context, String name, Supplier<T> computeIfAbsent) {
        var value = getSessionAttribute(context, name);

        if (value == null) {
            value = computeIfAbsent.get();
            setSessionAttribute(context, name, value);
        }

        return (T) value;
    }

    /**
     * @see Faces#setSessionAttribute(String, Object)
     */
    public static void setSessionAttribute(FacesContext context, String name, Object value) {
        getSessionMap(context).put(name, value);
    }

    /**
     * @see Faces#removeSessionAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T removeSessionAttribute(FacesContext context, String name) {
        return (T) getSessionMap(context).remove(name);
    }

    // Application scope ----------------------------------------------------------------------------------------------

    /**
     * @see Faces#getApplicationMap()
     */
    public static Map<String, Object> getApplicationMap(FacesContext context) {
        return context.getExternalContext().getApplicationMap();
    }

    /**
     * @see Faces#getApplicationAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getApplicationAttribute(FacesContext context, String name) {
        return (T) getApplicationMap(context).get(name);
    }

    /**
     * @see Faces#getApplicationAttribute(String, Supplier)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getApplicationAttribute(FacesContext context, String name, Supplier<T> computeIfAbsent) {
        var value = getApplicationAttribute(context, name);

        if (value == null) {
            value = computeIfAbsent.get();
            setApplicationAttribute(context, name, value);
        }

        return (T) value;
    }

    /**
     * @see Faces#setApplicationAttribute(String, Object)
     */
    public static void setApplicationAttribute(FacesContext context, String name, Object value) {
        getApplicationMap(context).put(name, value);
    }

    /**
     * @see Faces#removeApplicationAttribute(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T removeApplicationAttribute(FacesContext context, String name) {
        return (T) getApplicationMap(context).remove(name);
    }

    // File download --------------------------------------------------------------------------------------------------

    /**
     * @see Faces#sendFile(File, boolean)
     */
    public static void sendFile(FacesContext context, File file, boolean attachment) throws IOException {
        sendFile(context, new FileInputStream(file), file.getName(), file.length(), attachment);
    }

    /**
     * @see Faces#sendFile(File, String, boolean)
     */
    public static void sendFile(FacesContext context, File file, String filename, boolean attachment) throws IOException {
        sendFile(context, new FileInputStream(file), filename, file.length(), attachment);
    }

    /**
     * @see Faces#sendFile(Path, boolean)
     */
    public static void sendFile(FacesContext context, Path path, boolean attachment) throws IOException {
        sendFile(context, path.toFile(), attachment);
    }

    /**
     * @see Faces#sendFile(Path, String, boolean)
     */
    public static void sendFile(FacesContext context, Path path, String filename, boolean attachment) throws IOException {
        sendFile(context, path.toFile(), filename, attachment);
    }

    /**
     * @see Faces#sendFile(byte[], String, boolean)
     */
    public static void sendFile(FacesContext context, byte[] content, String filename, boolean attachment) {
        sendFile(context, new ByteArrayInputStream(content), filename, content.length, attachment);
    }

    /**
     * @see Faces#sendFile(InputStream, String, boolean)
     */
    public static void sendFile(FacesContext context, InputStream content, String filename, boolean attachment) {
        sendFile(context, content, filename, -1, attachment);
    }

    /**
     * @see Faces#sendFile(String, boolean, org.omnifaces.util.Callback.Output)
     */
    public static void sendFile(FacesContext context, String filename, boolean attachment, Callback.Output outputCallback) {
        var externalContext = context.getExternalContext();

        // Prepare the response and set the necessary headers.
        externalContext.setResponseBufferSize(DEFAULT_SENDFILE_BUFFER_SIZE);
        externalContext.setResponseContentType(getMimeType(context, filename));
        externalContext.setResponseHeader("Content-Disposition", formatContentDispositionHeader(filename, attachment));

        // Not exactly mandatory, but this fixes at least a MSIE quirk: http://support.microsoft.com/kb/316431
        if (isSecure((HttpServletRequest) externalContext.getRequest())) {
            externalContext.setResponseHeader("Cache-Control", "public");
            externalContext.setResponseHeader("Pragma", "public");
        }

        try (var output = externalContext.getResponseOutputStream()) {
            outputCallback.writeTo(output);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        context.responseComplete();
    }

    /**
     * Internal global method to send the given input stream to the response.
     * @param input The file content as input stream.
     * @param filename The file name which should appear in content disposition header.
     * @param contentLength The content length, or -1 if it is unknown.
     * @param attachment Whether the file should be provided as attachment, or just inline.
     * @throws UncheckedIOException When HTTP response is not available anymore.
     */
    private static void sendFile(FacesContext context, InputStream input, String filename, long contentLength, boolean attachment) {
        sendFile(context, filename, attachment, output -> {
            var externalContext = context.getExternalContext();

            // If content length is known, set it. Note that setResponseContentLength() cannot be used as it takes only int.
            if (contentLength != -1) {
                externalContext.setResponseHeader("Content-Length", String.valueOf(contentLength));
            }

            var size = Utils.stream(input, output);

            // This may be on time for files smaller than the default buffer size, but is otherwise ignored anyway.
            if (contentLength == -1 && !externalContext.isResponseCommitted()) {
                externalContext.setResponseHeader("Content-Length", String.valueOf(size));
            }
        });
    }

}
