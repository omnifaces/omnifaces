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
package org.omnifaces.context;

import static jakarta.faces.render.ResponseStateManager.VIEW_STATE_PARAM;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;
import static org.omnifaces.config.OmniFaces.OMNIFACES_EVENT_HEADER_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_EVENT_PARAM_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_VIEW_SCOPE_HEADER_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_VIEW_STATE_HEADER_NAME;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.ExternalContextWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.cdi.viewscope.ViewScopeManager;
import org.omnifaces.util.Faces;

/**
 * OmniFaces external context. This external context performs the following tasks:
 * <ol>
 * <li>Since 2.2: Take care that the {@link Flash} will be ignored during an unload request.
 * <li>Since 3.14.26: If the current request is an unload request from {@link ViewScoped} carrying the event in a request header, then return the request
 * parameters from the request headers without parsing the request body, so that the unload also succeeds when the client aborted while the request body was
 * still in transit.
 * </ol>
 *
 * @author Bauke Scholtz
 * @since 2.2
 * @see OmniExternalContextFactory
 */
public class OmniExternalContext extends ExternalContextWrapper {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Flash DUMMY_FLASH = new DummyFlash();

    // Variables ------------------------------------------------------------------------------------------------------

    private Map<String, String> unloadRequestParameterMap;
    private Map<String, String[]> unloadRequestParameterValuesMap;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct a new OmniFaces external context around the given wrapped external context.
     *
     * @param wrapped The wrapped external context.
     */
    public OmniExternalContext(ExternalContext wrapped) {
        super(wrapped);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * If the current request is an unload request from {@link ViewScoped}, then return a dummy flash scope which does not modify the flash state.
     */
    @Override
    public Flash getFlash() {
        if (ViewScopeManager.isUnloadRequest(Faces.getContext())) {
            return DUMMY_FLASH;
        }

        return super.getFlash();
    }

    /**
     * If the current request is an unload request from {@link ViewScoped} carrying the event in a request header, then return the request parameters from the
     * request headers, else return the original request parameter map.
     */
    @Override
    public Map<String, String> getRequestParameterMap() {
        return isUnloadRequestWithHeaders() ? unloadRequestParameterMap : super.getRequestParameterMap();
    }

    /**
     * If the current request is an unload request from {@link ViewScoped} carrying the event in a request header, then return the request parameters from the
     * request headers, else return the original request parameter values map.
     */
    @Override
    public Map<String, String[]> getRequestParameterValuesMap() {
        if (!isUnloadRequestWithHeaders()) {
            return super.getRequestParameterValuesMap();
        }

        if (unloadRequestParameterValuesMap == null) {
            unloadRequestParameterValuesMap = unmodifiableMap(
                unloadRequestParameterMap.entrySet().stream()
                    .collect(toMap(Entry::getKey, entry -> new String[] { entry.getValue() }))
            );
        }

        return unloadRequestParameterValuesMap;
    }

    /**
     * If the current request is an unload request from {@link ViewScoped} carrying the event in a request header, then return the names of the request
     * parameters from the request headers, else return the original request parameter names.
     */
    @Override
    public Iterator<String> getRequestParameterNames() {
        return isUnloadRequestWithHeaders() ? unloadRequestParameterMap.keySet().iterator() : super.getRequestParameterNames();
    }

    /**
     * Forget the request parameters from the request headers of the previous request.
     */
    @Override
    public void setRequest(Object request) {
        super.setRequest(request);
        unloadRequestParameterMap = null;
        unloadRequestParameterValuesMap = null;
    }

    private boolean isUnloadRequestWithHeaders() {
        if (unloadRequestParameterMap == null) {
            unloadRequestParameterMap = createUnloadRequestParameterMap(getRequestHeaderMap());
        }

        return !unloadRequestParameterMap.isEmpty();
    }

    /**
     * Returns the unload request parameters from the given request headers, or an empty map when the event header is absent, in which case the request body is
     * the only source of the unload request parameters.
     */
    private static Map<String, String> createUnloadRequestParameterMap(Map<String, String> headers) {
        if (!"unload".equals(headers.get(OMNIFACES_EVENT_HEADER_NAME))) {
            return emptyMap();
        }

        var parameters = new HashMap<String, String>();
        parameters.put(OMNIFACES_EVENT_PARAM_NAME, "unload");
        parameters.put("id", headers.get(OMNIFACES_VIEW_SCOPE_HEADER_NAME));
        parameters.put(VIEW_STATE_PARAM, headers.get(OMNIFACES_VIEW_STATE_HEADER_NAME));
        parameters.values().removeIf(Objects::isNull);
        return unmodifiableMap(parameters);
    }

    // Inner classes --------------------------------------------------------------------------------------------------

    /**
     * A dummy flash class which does absolutely nothing with regard to the flash scope.
     */
    private static class DummyFlash extends Flash {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Object put(String key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> m) {
            // NOOP.
        }

        @Override
        public void clear() {
            // NOOP.
        }

        @Override
        public Set<String> keySet() {
            return emptySet();
        }

        @Override
        public Collection<Object> values() {
            return emptySet();
        }

        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet() {
            return emptySet();
        }

        @Override
        public boolean isKeepMessages() {
            return false;
        }

        @Override
        public void setKeepMessages(boolean newValue) {
            // NOOP.
        }

        @Override
        public boolean isRedirect() {
            return false;
        }

        @Override
        public void setRedirect(boolean newValue) {
            // NOOP.
        }

        @Override
        public void putNow(String key, Object value) {
            // NOOP.
        }

        @Override
        public void keep(String key) {
            // NOOP.
        }

        @Override
        public void doPrePhaseActions(FacesContext ctx) {
            // NOOP.
        }

        @Override
        public void doPostPhaseActions(FacesContext ctx) {
            // NOOP.
        }

    }

}
