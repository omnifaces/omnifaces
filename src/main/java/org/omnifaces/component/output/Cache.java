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
package org.omnifaces.component.output;

import static jakarta.faces.event.PhaseId.RENDER_RESPONSE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.omnifaces.component.output.Cache.PropertyKeys.disabled;
import static org.omnifaces.component.output.Cache.PropertyKeys.key;
import static org.omnifaces.component.output.Cache.PropertyKeys.reset;
import static org.omnifaces.component.output.Cache.PropertyKeys.scope;
import static org.omnifaces.component.output.Cache.PropertyKeys.time;
import static org.omnifaces.component.output.Cache.PropertyKeys.useBuffer;
import static org.omnifaces.filter.OnDemandResponseBufferFilter.BUFFERED_RESPONSE;
import static org.omnifaces.util.Events.subscribeToRequestAfterPhase;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Faces.getRequestAttribute;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PreRenderViewEvent;
import jakarta.faces.event.SystemEvent;

import org.omnifaces.component.output.cache.el.CacheValue;
import org.omnifaces.component.output.cache.el.CachingValueExpression;
import org.omnifaces.filter.OnDemandResponseBufferFilter;
import org.omnifaces.io.ResettableBuffer;
import org.omnifaces.io.ResettableBufferedOutputStream;
import org.omnifaces.io.ResettableBufferedWriter;
import org.omnifaces.servlet.BufferedHttpServletResponse;
import org.omnifaces.servlet.HttpServletResponseOutputWrapper;
import org.omnifaces.util.State;
import org.omnifaces.util.cache.CacheEntry;
import org.omnifaces.util.cache.CacheFactory;
import org.omnifaces.util.cache.CacheInitializer;
import org.omnifaces.util.cache.CacheInstancePerScopeProvider;
import org.omnifaces.util.cache.CacheProvider;
import org.omnifaces.util.cache.DefaultCache;
import org.omnifaces.util.cache.DefaultCacheProvider;
import org.omnifaces.util.cache.LruCache;
import org.omnifaces.util.cache.TimeToLiveCache;

/**
 * <p>
 * The <code>&lt;o:cache&gt;</code> component allows to cache a fragment of rendered markup. The first
 * request for a page that has this component on it will cause this markup to be put into the cache. Then
 * for subsequent requests the cached content is used directly and none of the components, backing beans
 * and services that were used to generate this content in the first place will be consulted.
 * <p>
 * Caching can take place in application scope, or in session scope. For individual fragments a
 * time can be specified for which the cached content is valid. After this time is elapsed, the very
 * first request to the page containing the cache component in question will cause new content to be
 * rendered and put into the cache. A default time can be set per scope in web.xml.
 * <p>
 * For each scope a maximum capacity can be set. If the capacity for that scope is exceeded, an element will be
 * removed following a least recently used policy (LRU).
 * <p>
 * Via a cache provider mechanism an alternative cache implementation can be configured in web.xml. The default
 * cache is based on {@link LruCache}.
 *
 * <h2>Setting a custom caching provider</h2>
 * <p>
 * A custom caching provider can be set by using the <code>org.omnifaces.CACHE_PROVIDER</code> context
 * parameter in web.xml to point to an implementation of <code>org.omnifaces.component.output.cache.CacheProvider</code>.
 * For example:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.CACHE_PROVIDER&lt;/param-name&gt;
 *     &lt;param-value&gt;com.example.MyProvider&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * The default provider, <code>org.omnifaces.component.output.cache.DefaultCacheProvider</code> can be used as an
 * example.
 *
 * <h2>Global settings</h2>
 * <p>
 * For the default provider, the maximum capacity and the default time to live can be specified for the
 * supported scopes "session" and "application". If the maximum capacity is reached, an entry will be
 * evicted following a least recently used policy. The default time to live specifies for how long
 * entries are considered to be valid. A value for the <code>time</code> attribute on this component
 * will override this global default time to live. The following context parameters can be used in web.xml:
 * <table><caption>All available context parameters</caption>
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_SETTING_APPLICATION_MAX_CAPACITY</code>
 * </td><td>
 * Sets the maximum number of elements that will be stored per web module (application scope).
 * Default: no limit
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_SETTING_SESSION_MAX_CAPACITY</code>
 * </td><td>
 * Sets the maximum number of elements that will be stored per session.
 * Default: no limit.
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_SETTING_APPLICATION_TTL</code>
 * </td><td>
 * Sets the maximum amount of time in seconds that cached content is valid for the application scope.
 * Can be overriden by individal cache components.
 * Default: no limit.
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_SETTING_SESSION_TTL</code>
 * </td><td>
 * Sets the maximum amount of time in seconds that cached content is valid for the session scope.
 * Can be overriden by individal cache components.
 * Default: no limit.
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_INSTALL_BUFFER_FILTER</code>
 * </td><td>
 * Boolean that when <code>true</code> installs a Servlet Filter (Servlet 3.0+ only) that works in conjunction with the
 * <code>useBuffer</code> attribute of the Cache component to enable an alternative way to grab the content that needs
 * to be cached. This is a convenience setting that is a short-cut for installing the
 * <code>org.omnifaces.servlet.BufferedHttpServletResponse</code> filter manually. If more finegrained control is needed
 * regarding which place in the filter chain the filter appears and which resources it exactly filters, this setting
 * should not be used and the mentioned filter should be manually configured.
 * Default: <code>false</code>.
 * </td></tr>
 * </table>
 *
 * @since 1.1
 * @author Arjan Tijms
 * @see org.omnifaces.util.cache.Cache
 * @see CacheEntry
 * @see CacheFactory
 * @see CacheInitializer
 * @see CacheInstancePerScopeProvider
 * @see CacheProvider
 * @see DefaultCache
 * @see DefaultCacheProvider
 * @see LruCache
 * @see TimeToLiveCache
 * @see CacheValue
 * @see CachingValueExpression
 * @see OnDemandResponseBufferFilter
 * @see BufferedHttpServletResponse
 * @see HttpServletResponseOutputWrapper
 * @see ResettableBuffer
 * @see ResettableBufferedOutputStream
 * @see ResettableBufferedWriter
 * @see OutputFamily
 */
@FacesComponent(Cache.COMPONENT_TYPE)
public class Cache extends OutputFamily {

    /** The component type, which is {@value org.omnifaces.component.output.Cache#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.output.Cache";

    /** The default scope, which is "session". */
    public static final String DEFAULT_SCOPE = "session";

    private static final String VALUE_SET = "org.omnifaces.cache.VALUE_SET";
    private static final String START_CONTENT_MARKER = "<!-- START CACHE FOR %s -->";
    private static final String END_CONTENT_MARKER = "<!-- END CACHE FOR %s -->";

    private static final String ERROR_NO_BUFFERED_RESPONSE = format(
        "No buffered response found in request, but 'useBuffer' set to true. Check setting the '%s' context parameter or installing the '%s' filter manually.",
        CacheInitializer.CACHE_INSTALL_BUFFER_FILTER, OnDemandResponseBufferFilter.class
    );
    private static final Class<? extends SystemEvent> PRE_RENDER = PreRenderViewEvent.class;

    private final State state = new State(getStateHelper());

    enum PropertyKeys {
        key, scope, time, useBuffer, reset, disabled
    }

    /**
     * Constructs the component.
     */
    public Cache() {
        var context = FacesContext.getCurrentInstance();

        // Execute the following code in PreRenderView, since at construction time the "useBuffer" and "key" attributes
        // have not been set, and there is no @PostContruct for UIComponents.
        subscribeToViewEvent(PRE_RENDER, () -> processPreRenderViewEvent(context));
    }

    private void processPreRenderViewEvent(FacesContext context) {
        if (!isDisabled() && isUseBuffer() && !hasCachedValue(context)) {
            var bufferedResponse = (BufferedHttpServletResponse) getRequestAttribute(BUFFERED_RESPONSE);

            if (bufferedResponse == null) {
                throw new IllegalStateException(ERROR_NO_BUFFERED_RESPONSE);
            }

            // Start buffering the response from now on
            bufferedResponse.setPassThrough(false);

            // After the RENDER_RESPONSE phase, copy the area we need to cache from the response buffer
            // and insert it into our cache
            subscribeToRequestAfterPhase(RENDER_RESPONSE, () -> processPostRenderResponsePhase(context, bufferedResponse));
        }
    }

    private void processPostRenderResponsePhase(FacesContext context, BufferedHttpServletResponse bufferedResponse) {
        String content = null;

        try {
            content = getContentFromBuffer(bufferedResponse.getBufferAsString());
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (content != null) {
            cacheContent(context, content);
        }
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {

        if (isDisabled()) {
            super.encodeChildren(context);
            return;
        }

        var key = getKeyWithDefault(context);
        var responseWriter = context.getResponseWriter();
        var scopedCache = getCacheImpl(context);

        if (isReset()) {
            scopedCache.remove(key);
        }

        var childRendering = scopedCache.get(key);

        if (childRendering == null) {
            var bufferWriter = new StringWriter();
            var bufferedResponseWriter = responseWriter.cloneWithWriter(bufferWriter);
            context.setResponseWriter(bufferedResponseWriter);

            try {
                if (isUseBuffer()) {
                    bufferedResponseWriter.write(getStartContentMarker());
                }

                super.encodeChildren(context);

                if (isUseBuffer()) {
                    bufferedResponseWriter.write(getEndContentMarker());
                }
            } finally {
                context.setResponseWriter(responseWriter);
            }

            childRendering = bufferWriter.toString();
            cacheContent(context, scopedCache, key, childRendering);
        }

        responseWriter.write(childRendering);
    }

    /**
     * Gets a named attribute associated with the main cache entry this component is using to store
     * the rendering of its child components.
     *
     * @param context the current FacesContext
     * @param name name of the attribute to retrieve a value for
     * @return value associated with the named attribute
     * @since 1.2
     */
    public Serializable getCacheAttribute(FacesContext context, String name) {
        return getCacheImpl(context).getAttribute(getKeyWithDefault(context), name);
    }

    /**
     * Sets a named attribute associated with the main cache entry this component is using to store
     * the rendering of its child components.
     *
     * @param context the current FacesContext
     * @param name name of the attribute under which the value is stored
     * @param value the value that is to be stored
     * @since 1.2
     */
    public void setCacheAttribute(FacesContext context, String name, Serializable value) {
        getCacheImpl(context).putAttribute(getKeyWithDefault(context), name, value, getTime());
    }

    @Override
    protected boolean isVisitable(VisitContext visitContext) {

        var context = visitContext.getFacesContext();

        // Visit us and our children if a value for the cache was set in this request, or
        // if no value was cached yet.
        return isDisabled() || isCachedValueJustSet(context) || !hasCachedValue(context);
    }

    private void cacheContent(FacesContext context, String content) {
        cacheContent(context, CacheFactory.getCache(context, getScope()), getKeyWithDefault(context), content);
    }

    private void cacheContent(FacesContext context, org.omnifaces.util.cache.Cache scopedCache, String key, String content) {
        int time = getTime();
        if (time > 0) {
            scopedCache.put(key, content, time);
        } else {
            scopedCache.put(key, content);
        }

        // Marker to register we added a value to the cache during this request
        context.getExternalContext().getRequestMap().put(VALUE_SET, TRUE);
    }

    private String getKeyWithDefault(FacesContext context) {
        var key = getKey();
        if (key == null) {
            key = context.getViewRoot().getViewId() + "_" + this.getClientId(context);
        }

        return key;
    }

    private org.omnifaces.util.cache.Cache getCacheImpl(FacesContext context) {
        return CacheFactory.getCache(context, getScope());
    }

    /**
     *
     * @param context the FacesContext
     * @return true if a value was inserted in the cache during this request, false otherwise
     */
    private static boolean isCachedValueJustSet(FacesContext context) {
        return TRUE.equals(context.getExternalContext().getRequestMap().get(VALUE_SET));
    }

    /**
     *
     * @param context the FacesContext
     * @return true if there is a value in the cache corresponding to this component, false otherwise
     */
    private boolean hasCachedValue(FacesContext context) {
        return CacheFactory.getCache(context, getScope()).get(getKeyWithDefault(context)) != null;
    }

    private String getStartContentMarker() {
        return format(START_CONTENT_MARKER, getClientId());
    }

    private String getEndContentMarker() {
        return format(END_CONTENT_MARKER, getClientId());
    }

    private String getContentFromBuffer(String buffer) {
        var startMarker = getStartContentMarker();
        var startIndex = buffer.indexOf(startMarker);

        if (startIndex != -1) {

            var endMarker = getEndContentMarker();
            var endIndex = buffer.indexOf(endMarker);

            if (endIndex != -1) {

                return buffer.substring(startIndex + startMarker.length(), endIndex);
            }
        }

        return null;
    }


    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns key used to store content in the cache.
     * @return Key used to store content in the cache.
     */
    public String getKey() {
        return state.get(key);
    }

    /**
     * Optional key used to store content in the cache. If no key is specified, a key is calculated
     * based on the client Id of this component.
     * <p>
     * While auto-generated keys can be convenient, note that in the face of dynamic behavior on a view the
     * id of a component and thus the cache key can change in ways that are difficult to predict.
     * <p>
     * Keys are relative to the scope for which they are defined, meaning a key "foo" for the a session scoped
     * cache will not conflict with a key of the same name for an application scoped cache.
     * @param keyValue Key used to store content in the cache.
     */
    public void setKey(String keyValue) {
        state.put(key, keyValue);
    }

    /**
     * Returns scope identifier used to set the scope in which caching takes place. Default is <code>session</code>.
     * @return Scope identifier used to set the scope in which caching takes place.
     */
    public String getScope() {
        return state.get(scope, DEFAULT_SCOPE);
    }

    /**
     * Optional scope identifier used to set the scope in which caching takes place. If no scope is specified,
     * the default scope "session" will be used.
     * <p>
     * The supported scopes depent on the caching provider that is installed via the
     * <code>org.omnifaces.CACHE_PROVIDER</code>. If no such provider is installed, a default one is used
     * that supports scopes "session" and "application".
     * <p>
     * A runtime exception will be thrown if an unsupported value for scope is used.
     * @param scopeValue Scope identifier used to set the scope in which caching takes place.
     */
    public void setScope(String scopeValue) {
        state.put(scope, scopeValue);
    }

    /**
     * Returns amount of time in seconds for which the cached content is valid (TTL). Default is <code>-1</code>.
     * @return Amount of time in seconds for which the cached content is valid (TTL).
     */
    public Integer getTime() {
        return state.get(time, -1);
    }

    /**
     * Optional amount of time in seconds for which the cached content is valid (TTL). This is counted
     * from the moment content is actually added to the cache. If no time is provided the content will be subject
     * to the global cache settings, and in absence of these are subject to the behavior of the cache implementation
     * that is used. The default cache implementation will simply cache indefinitely.
     * <p>
     * Whether the content is actually removed from the cache (to preserve memory) after the given time has elapsed is
     * dependend on the actual cache implementation that is used. The default cache implementation will
     * <strong>NOT</strong> do this automatically, but will instead remove it only when the cache item is being accessed
     * again.
     * <p>
     * Following the above, new content will only be inserted into the cache following a page request. A time of e.g. <code>30</code>
     * <strong>will not</strong> cause new content to be inserted into the cache at <code>30</code> seconds intervals.
     * <p>
     * Note that this component <strong>does not</strong> support a cache loader and locking mechanism. This means after content times out,
     * several simultaneous page requests may render the same content and it's undetermined which of those will end up being cached.
     * @param timeValue Amount of time in seconds for which the cached content is valid (TTL).
     */
    public void setTime(Integer timeValue) {
        state.put(time, timeValue);
    }

    /**
     * Returns whether to switch to an alternative method to grab the content generated by the children of this component. Default is <code>false</code>.
     * @return Whether to switch to an alternative method to grab the content generated by the children of this component.
     */
    public boolean isUseBuffer() {
        return state.get(useBuffer, FALSE);
    }

    /**
     * Switches to an alternative method to grab the content generated by the children of this component.
     * <p>
     * Normally this content is obtained by replacing the so-called response writer when the parent Cache
     * component delegates rendering to its children. However, in some cases (like <code>h:form</code>) there is an amount
     * of post-processing being done on the response outside the context of this parent - child delegation.
     * <p>
     * Via this switch, the full response is buffered if the cache doesn't contain content for this component and special
     * markers are inserted surrounding the children's rendering. Afterwards, the content between the markers (if any) is
     * extracted and inserted into the cache. Note that the full response is only buffered incase there's no value in the cache.
     * For all other requests this buffering will not happen.
     * <p>
     * Since this is clearly a more resource intensive and invasive method to grab content, it's not enabled by default.
     * In addition to setting this attribute to <code>true</code>, the <code>org.omnifaces.servlet.BufferedHttpServletResponse</code>
     * Servlet Filter needs to be configured to filter the Faces Servlet (or alternatively just the pages for which the buffering
     * method should be used).
     * @param useBufferValue Whether to switch to an alternative method to grab the content generated by the children of this component
     */
    public void setUseBuffer(boolean useBufferValue) {
        state.put(useBuffer, useBufferValue);
    }

    /**
     * Returns whether to reset this cache. Default is <code>false</code>.
     * @return Whether to reset this cache.
     */
    public boolean isReset() {
        return state.get(reset, FALSE);
    }

    /**
     * Resets the cache when set to <code>true</code>.
     * <p>
     * This attribute can be used to reset the cache, meaning that the first time the cache component is rendered
     * again, it will re-render its children and will cause any cached value expressions (via <code>o:cacheValue</code>) to be
     * re-evaluated when its next referenced.
     * <p>
     * Note that this value has to remain true until the cache component is rendered, after that it should be set to false
     * again otherwise the cached content will be reset again at the next rendering.
     * @param resetValue Whether to reset this cache.
     */
    public void setReset(boolean resetValue) {
        state.put(reset, resetValue);
    }

    /**
     * Returns whether this cache is disabled. Default is <code>false</code>.
     * @return Whether this cache is disabled.
     * @since 1.8
     */
    public boolean isDisabled() {
        return state.get(disabled, FALSE);
    }

    /**
     * Disables the cache when set to <code>true</code>. Default is <code>false</code>.
     * <p>
     * This attribute can be used to disable the cache (temporarily). In disabled state the children will be rendered directly
     * and cached content (if any) will not be used, nor will the rendering outcome of the children be cached.
     * <p>
     * When the attribute is set to <code>false</code> again any content that was cached before the cache was disabled will be
     * shown again if said content is still available in the cache. The content that was rendered when the cache was disabled
     * has no effect on any such previously cached content.
     * @param disabledValue Whether this cache is disabled.
     * @since 1.8
     */
    public void setDisabled(boolean disabledValue) {
        state.put(disabled, disabledValue);
    }

}