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
package org.omnifaces.resourcehandler;

import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.ResourceHandlerWrapper;

/**
 * <p>
 * A default {@link ResourceHandler} implementation which hooks on all three {@link #createResource(String)},
 * {@link #createResource(String, String)} and {@link #createResource(String, String, String)} methods. Implementors
 * should only need to override <strong>either</strong> {@link #getLibraryName()} and
 * {@link #createResourceFromLibrary(String, String)}, <strong>or</strong> {@link #decorateResource(Resource)}.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public abstract class DefaultResourceHandler extends ResourceHandlerWrapper {

    // Constants ------------------------------------------------------------------------------------------------------

    /** The default URI when a resource is not found. */
    public static final String RES_NOT_FOUND = "RES_NOT_FOUND";

    /**
     * The Faces 4+ script resource name.
     * @since 4.0
     */
    public static final String FACES_SCRIPT_RESOURCE_NAME = "faces.js";

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of this default resource handler which wraps the given resource handler.
     * @param wrapped The resource handler to be wrapped.
     */
    protected DefaultResourceHandler(ResourceHandler wrapped) {
        super(wrapped);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Delegate to {@link #decorateResource(Resource, String, String)} with result of {@link #createResource(String)}
     * from the wrapped resource handler.
     * <p>
     * Implementors should <strong>not</strong> override this.
     */
    @Override
    public Resource createResource(String resourceName) {
        return decorateResource(getWrapped().createResource(resourceName), resourceName, null);
    }

    /**
     * If library name is not null and it equals {@link #getLibraryName()}, then delegate to
     * {@link #createResourceFromLibrary(String, String)} with <code>null</code> as content type, else delegate to
     * {@link #decorateResource(Resource, String, String)} with result of {@link #createResource(String, String)}
     * from the wrapped resource handler.
     * <p>
     * Implementors should <strong>not</strong> override this.
     */
    @Override
    public Resource createResource(String resourceName, String libraryName) {
        if (libraryName != null && libraryName.equals(getLibraryName())) {
            return createResourceFromLibrary(resourceName, null);
        }
        else {
            return decorateResource(getWrapped().createResource(resourceName, libraryName), resourceName, libraryName);
        }
    }

    /**
     * If library name is not null and it equals {@link #getLibraryName()}, then delegate to
     * {@link #createResourceFromLibrary(String, String)}, else delegate to
     * {@link #decorateResource(Resource, String, String)} with result of
     * {@link #createResource(String, String, String)} from the wrapped resource handler.
     * <p>
     * Implementors should <strong>not</strong> override this.
     */
    @Override
    public Resource createResource(String resourceName, String libraryName, String contentType) {
        if (libraryName != null && libraryName.equals(getLibraryName())) {
            return createResourceFromLibrary(resourceName, contentType);
        }
        else {
            return decorateResource(getWrapped().createResource(resourceName, libraryName, contentType), resourceName, libraryName);
        }
    }

    /**
     * Returns the library name on which this resource handler implementation should listen. If a resource from
     * specifically this library name is requested, then {@link #createResourceFromLibrary(String, String)} will be
     * called to create the resource, else {@link #decorateResource(Resource)} will be called with result of the call
     * from the wrapped resource handler.
     * <p>
     * The default implementation returns <code>null</code>.
     * @return The library name on which this resource handler implementation should listen.
     */
    public String getLibraryName() {
        return null;
    }

    /**
     * Returns the library-specific resource in case a resource from specifically the library name as identified by
     * {@link #getLibraryName()} is requested.
     * <p>
     * The default implementation returns <code>null</code>.
     * @param resourceName The resource name.
     * @param contentType The content type.
     * @return The library-specific resource.
     */
    public Resource createResourceFromLibrary(String resourceName, String contentType) {
        return null;
    }

    /**
     * Decorate the given resource. This will only be called if no library-specific resource has been requested.
     * <p>
     * The default implementation delegates to {@link #decorateResource(Resource)}.
     * @param resource The resource to be decorated.
     * @param resourceName The resource name.
     * @param libraryName The library name.
     * @return The decorated resource.
     * @since 2.6
     */
    public Resource decorateResource(Resource resource, String resourceName, String libraryName) {
        return decorateResource(resource);
    }

    /**
     * Decorate the given resource. This will only be called if no library-specific resource has been requested.
     * <p>
     * The default implementation just returns the given resource unmodified.
     * @param resource The resource to be decorated.
     * @return The decorated resource.
     */
    public Resource decorateResource(Resource resource) {
        return resource;
    }

}