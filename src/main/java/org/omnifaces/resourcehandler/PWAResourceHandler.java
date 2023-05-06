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

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.Components.addScript;
import static org.omnifaces.util.Components.addScriptResource;
import static org.omnifaces.util.Components.forEachComponent;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getRequestDomainURL;
import static org.omnifaces.util.Faces.getResourceAsStream;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.Bean;
import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIGraphic;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewScoped;

import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;
import org.omnifaces.util.FacesLocal;
import org.omnifaces.util.Json;

/**
 * <p>
 * This {@link ResourceHandler} generates the <code>manifest.json</code> and also an offline-aware <code>sw.js</code>
 * based on any {@link WebAppManifest} found in the runtime classpath. Historical note: this class was introduced in 3.6
 * as <code>WebAppManifestResourceHandler</code> without any service worker related logic and since 3.7 renamed to
 * <code>PWAResourceHandler</code> after having service worker related logic added.
 *
 * <h2>Usage</h2>
 * <ol>
 * <li>Create a class which extends {@link WebAppManifest} in your web application project.</li>
 * <li>Give it the appropriate CDI scope annotation, e.g {@link ApplicationScoped}, {@link SessionScoped} or even
 * {@link RequestScoped} (note that a {@link ViewScoped} won't work).</li>
 * <li>Override properties accordingly conform javadoc and the rules in
 * <a href="https://www.w3.org/TR/appmanifest/">the W3 spec</a>.</li>
 * <li>Reference it as <code>#{resource['omnifaces:manifest.json']}</code> in your template.
 * </ol>
 * <p>
 * Here's a concrete example:
 * <pre>
 * package com.example;
 *
 * import java.util.Arrays;
 * import java.util.Collection;
 * import javax.enterprise.context.ApplicationScoped;
 *
 * import org.omnifaces.resourcehandler.WebAppManifest;
 *
 * &#64;ApplicationScoped
 * public class ExampleWebAppManifest extends WebAppManifest {
 *
 *     &#64;Override
 *     public String getName() {
 *         return "Example Application Name";
 *     }
 *
 *     &#64;Override
 *     public String getShortName() {
 *         return "EAN";
 *     }
 *
 *     &#64;Override
 *     public Collection&lt;ImageResource&gt; getIcons() {
 *         return Arrays.asList(
 *             ImageResource.of("logo.svg"),
 *             ImageResource.of("logo-120x120.png", Size.SIZE_120),
 *             ImageResource.of("logo-180x180.png", Size.SIZE_180),
 *             ImageResource.of("logo-192x192.png", Size.SIZE_192),
 *             ImageResource.of("logo-512x512.png", Size.SIZE_512)
 *         );
 *     }
 *
 *     &#64;Override
 *     public String getThemeColor() {
 *         return "#cc9900";
 *     }
 *
 *     &#64;Override
 *     public String getBackgroundColor() {
 *         return "#ffffff";
 *     }
 *
 *     &#64;Override
 *     public Display getDisplay() {
 *         return Display.STANDALONE;
 *     }
 *
 *     &#64;Override
 *     public Collection&lt;Category&gt; getCategories() {
 *         return Arrays.asList(Category.BUSINESS, Category.FINANCE);
 *     }
 *
 *     &#64;Override
 *     public Collection&lt;RelatedApplication&gt; getRelatedApplications() {
 *         return Arrays.asList(
 *             RelatedApplication.of(Platform.PLAY, "https://play.google.com/store/apps/details?id=com.example.app1", "com.example.app1"),
 *             RelatedApplication.of(Platform.ITUNES, "https://itunes.apple.com/app/example-app1/id123456789")
 *         );
 *     }
 * }
 * </pre>
 * <p>
 * Reference it in your template exactly as follows, with the exact library name of <code>omnifaces</code> and
 * exact resource name of <code>manifest.json</code>. You cannot change these values.
 * <pre>
 * &lt;link rel="manifest" href="#{resource['omnifaces:manifest.json']}" crossorigin="use-credentials" /&gt;
 * </pre>
 * <p>
 * The <code>crossorigin</code> attribute is optional, you can drop it, but it's mandatory if you've put the
 * {@link SessionScoped} annotation on your {@link WebAppManifest} bean, else the browser won't retain the session
 * cookies while downloading the <code>manifest.json</code> and then this resource handler won't be able to maintain the
 * server side cache, see also next section.
 * <p>
 * Note: you do not need to explicitly register this resource handler. It's already automatically registered.
 *
 * <h2>Server side caching</h2>
 * <p>
 * Basically, the CDI scope annotation being used is determinative for the autogenerated <code>v=</code> query
 * parameter indicating the last modified timestamp. If you make your {@link WebAppManifest} bean {@link RequestScoped},
 * then it'll change on every request and the browser will be forced to re-download it. If you can however guarantee
 * that the properties of your {@link WebAppManifest} are static, and thus you can safely make it
 * {@link ApplicationScoped}, then the <code>v=</code> query parameter will basically represent the timestamp of
 * the first time the bean is instantiated.
 *
 * <h2>Offline-aware service worker</h2>
 * <p>
 * The generated <code>sw.js</code> will by default auto-register the {@link WebAppManifest#getStartUrl()} and all
 * welcome files from <code>web.xml</code> as cacheable resources which are also available offline. You can override
 * the welcome files with {@link WebAppManifest#getCacheableViewIds()}. E.g.
 * <pre>
 * &#64;Override
 * public Collection&lt;String&gt; getCacheableViewIds() {
 *     return Arrays.asList("/index.xhtml", "/contact.xhtml", "/support.xhtml");
 * }
 * </pre>
 * <p>
 * If this method returns an empty collection, i.e. there are no cacheable resources at all, and thus also no offline
 * resources at all, then no service worker file will be generated as it won't have any use then.
 * <p>
 * In case you want to show a custom page as "You are offline!" error page, then you can specify it by overriding
 * the {@link WebAppManifest#getOfflineViewId()}.
 * <pre>
 * &#64;Override
 * public String getOfflineViewId() {
 *     return "/offline.xhtml";
 * }
 * </pre>
 * <p>
 * Whereby the <code>offline.xhtml</code> should contain something like this:
 * <pre>
 * &lt;h1&gt;Whoops! You appear to be offline!&lt;/h1&gt;
 * &lt;p&gt;Please check your connection and then try refreshing this page.&lt;/p&gt;
 * </pre>
 * <p>
 * For each of those "cacheable view IDs" and "offline view IDs", the JSF view briefly will be built in in order to
 * extract all <code>&lt;x:outputStylesheet&gt;</code>,<code>&lt;x:outputScript&gt;</code> and
 * <code>&lt;x:graphicImage&gt;</code> resources and add them to cacheable resources of the service worker as well.
 * <p>
 * If the {@link WebAppManifest#getCacheableViewIds()} returns an empty collection, then no <code>sw.js</code> will
 * be generated, and {@link WebAppManifest#getOfflineViewId()} will also be ignored.
 *
 * <h2>Client side events</h2>
 * <p>
 * In the client side, you can listen on <code>omnifaces.offline</code> and <code>omnifaces.online</code> events in the
 * <code>window</code> whether the client is currently online or offline.
 * <pre>
 * window.addEventListener("omnifaces.online", function(event) {
 *     var url = event.detail.url;
 *     // ..
 * });
 * window.addEventListener("omnifaces.offline", function(event) {
 *     var url = event.detail.url;
 *     var error = event.detail.error;
 *     // ...
 * });
 * </pre>
 * <p>
 * Or when you're using jQuery:
 * <pre>
 * $(window).on("omnifaces.online", function(event) {
 *     var url = event.detail.url;
 *     // ..
 * });
 * $(window).on("omnifaces.offline", function(event) {
 *     var url = event.detail.url;
 *     var error = event.detail.error;
 *     // ...
 * });
 * </pre>
 * <p>
 * This gives you the opportunity to set a global flag and/or show some sort of notification.
 * The <code>event.detail</code> will contain at least the <code>url</code> which was being requested through the
 * service worker, and in case of the <code>omnifaces.offline</code> event, there will also be an <code>error</code>
 * which represents the original network error object thrown by
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API"><code>fetch()</code></a>.
 *
 * @author Bauke Scholtz
 * @since 3.7
 * @see WebAppManifest
 * @see <a href="https://www.w3.org/TR/appmanifest">https://www.w3.org/TR/appmanifest</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest">https://developer.mozilla.org/en-US/docs/Web/Manifest</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API">https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API</a>
 */
public class PWAResourceHandler extends DefaultResourceHandler {

	private static final Logger logger = Logger.getLogger(PWAResourceHandler.class.getName());

	private static final String WARNING_NO_CACHEABLE_VIEW_IDS =
			"WebAppManifest#getCacheableViewIds() returned an empty collection, so no sw.js file will be generated.";
	private static final String WARNING_INVALID_CACHEABLE_VIEW_ID =
			"Cacheable view ID '%s' does not seem to exist, so it will be skipped for sw.js. Perhaps the WebAppManifest#getCacheableViewIds() returned a typo?";
	private static final String WARNING_INVALID_OFFLINE_VIEW_ID =
			"Offline view ID '%s' does not seem to exist, so it will be skipped for sw.js. Perhaps the WebAppManifest#getOfflineViewId() returned a typo?";

	public static final String MANIFEST_RESOURCE_NAME = "manifest.json";
	public static final String SERVICEWORKER_RESOURCE_NAME = "sw.js";
	public static final String SCRIPT_INIT = "OmniFaces.ServiceWorker.init('%s','%s')";

	private final Bean<WebAppManifest> manifestBean;

	private byte[] manifestContents;
	private byte[] serviceWorkerContents;
	private long lastModified;

	/**
	 * Creates a new instance of this web app manifest resource handler which wraps the given resource handler.
	 * This will also try to resolve the concrete implementation of {@link WebAppManifest}.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public PWAResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
		manifestBean = Beans.resolve(WebAppManifest.class); // Unfortunately, @Inject isn't yet supported in ResourceHandler.
	}

	@Override
	public Resource decorateResource(Resource resource, String resourceName, String libraryName) {
		if (manifestBean == null || !OMNIFACES_LIBRARY_NAME.equals(libraryName)) {
			return resource;
		}

		boolean manifestResourceRequest = MANIFEST_RESOURCE_NAME.equals(resourceName);
		boolean serviceWorkerResourceRequest = SERVICEWORKER_RESOURCE_NAME.equals(resourceName);

		if (!(manifestResourceRequest || serviceWorkerResourceRequest)) {
			return resource;
		}

		WebAppManifest manifest = Beans.getInstance(manifestBean, false);

		if (manifest == null) {
			manifest = Beans.getInstance(manifestBean, true);
			lastModified = 0;
		}

		FacesContext context = Faces.getContext();
		boolean resourceContentsRequest = super.isResourceRequest(context);

		if (resourceContentsRequest && lastModified == 0) {
			manifestContents = Json.encode(manifest, PWAResourceHandler::camelCaseToSnakeCase).getBytes(UTF_8);
			serviceWorkerContents = getServiceWorkerContents(manifest).getBytes(UTF_8);
			lastModified = System.currentTimeMillis();
		}

		if (manifestResourceRequest) {
			if (!resourceContentsRequest) {
				if (!manifest.getCacheableViewIds().isEmpty()) {
					addScriptResource(JSF_SCRIPT_LIBRARY_NAME, JSF_SCRIPT_RESOURCE_NAME); // Ensure it's always included BEFORE omnifaces.js.
					addScriptResource(OMNIFACES_LIBRARY_NAME, OMNIFACES_SCRIPT_NAME);
					addScript(format(SCRIPT_INIT, getServiceWorkerUrl(context), getServiceWorkerScope(context)));
				}
				else {
					logger.warning(WARNING_NO_CACHEABLE_VIEW_IDS);
				}
			}

			return createManifestResource();
		}
		else {
			return createServiceWorkerResource();
		}
	}

	private DynamicResource createManifestResource() {
		return new DynamicResource(MANIFEST_RESOURCE_NAME, OMNIFACES_LIBRARY_NAME, "application/json") {
			@Override
			public InputStream getInputStream() throws IOException {
				return new ByteArrayInputStream(manifestContents);
			}

			@Override
			public long getLastModified() {
				return lastModified;
			}
		};
	}

	private DynamicResource createServiceWorkerResource() {
		return new DynamicResource(SERVICEWORKER_RESOURCE_NAME, OMNIFACES_LIBRARY_NAME, "application/javascript") {
			@Override
			public InputStream getInputStream() throws IOException {
				return new ByteArrayInputStream(serviceWorkerContents);
			}

			@Override
			public long getLastModified() {
				return lastModified;
			}

			@Override
			public Map<String, String> getResponseHeaders() {
				Map<String, String> responseHeaders = super.getResponseHeaders();
				responseHeaders.put("Service-Worker-Allowed", getServiceWorkerScope(getContext()));
				return responseHeaders;
			}
		};
	}

	private static String camelCaseToSnakeCase(String string) {
		return string.codePoints().collect(StringBuilder::new, (sb, cp) -> {
			if (isUpperCase(cp)) {
				sb.append('_').appendCodePoint(toLowerCase(cp));
			}
			else {
				sb.appendCodePoint(cp);
			}
		}, (sb1, sb2) -> {}).toString();
	}

	private static String getServiceWorkerContents(WebAppManifest manifest) {
		if (manifest.getCacheableViewIds().isEmpty()) {
			return "";
		}
		else {
			try (Scanner scanner = new Scanner(getResourceAsStream("/" + OMNIFACES_LIBRARY_NAME + "/" + SERVICEWORKER_RESOURCE_NAME), UTF_8.name())) {
				return scanner.useDelimiter("\\A").next()
					.replace("$cacheableResources", Json.encode(getCacheableResources(manifest)))
					.replace("$offlineResource", Json.encode(getOfflineResource(manifest)));
			}
		}
	}

	private static Collection<String> getCacheableResources(WebAppManifest manifest) {
		FacesContext context = Faces.getContext();
		ViewHandler viewHandler = context.getApplication().getViewHandler();
		Collection<String> viewIds = new LinkedHashSet<>(manifest.getCacheableViewIds());
		Collection<String> cacheableResources = new LinkedHashSet<>();
		cacheableResources.add(manifest.getStartUrl().replaceFirst(Pattern.quote(getRequestDomainURL()), ""));

		if (manifest.getOfflineViewId() != null) {
			viewIds.add(manifest.getOfflineViewId());
		}

		for (String viewId : viewIds) {
			ViewDeclarationLanguage viewDeclarationLanguage = viewHandler.getViewDeclarationLanguage(context, viewId);

			if (!viewDeclarationLanguage.viewExists(context, viewId)) {
				logger.warning(format(viewId.equals(manifest.getOfflineViewId()) ? WARNING_INVALID_OFFLINE_VIEW_ID : WARNING_INVALID_CACHEABLE_VIEW_ID, viewId));
				continue;
			}

			cacheableResources.add(viewHandler.getActionURL(context, viewId));
			UIViewRoot view = viewHandler.createView(context, viewId);

			try {
				context.setViewRoot(view); // YES, this is safe to do so during a ResourceHandler#isResourceRequest(), but it's otherwise dirty!
				viewDeclarationLanguage.buildView(context, view);
				context.getApplication().publishEvent(context, PreRenderViewEvent.class, view);
			}
			catch (Exception e) {
				throw new FacesException("Cannot build the view " + viewId, e);
			}

			forEachComponent(context).fromRoot(view).ofTypes(UIGraphic.class, UIOutput.class).invoke(component -> {
				if (component instanceof UIGraphic && ((UIGraphic) component).getValue() != null) {
					cacheableResources.add(((UIGraphic) component).getValue().toString());
				}
				else if (component.getAttributes().get("name") != null) {
					String url = getResourceUrl(context, (String) component.getAttributes().get("library"), (String) component.getAttributes().get("name"));

					if (url != null) {
						cacheableResources.add(url);
					}
				}
			});
		}

		cacheableResources.add(getResourceUrl(context, JSF_SCRIPT_LIBRARY_NAME, JSF_SCRIPT_RESOURCE_NAME));
		cacheableResources.add(getResourceUrl(context, OMNIFACES_LIBRARY_NAME, OMNIFACES_SCRIPT_NAME));
		return cacheableResources;
	}

	private static String getOfflineResource(WebAppManifest manifest) {
		if (manifest.getOfflineViewId() != null) {
			FacesContext context = Faces.getContext();
			ViewHandler viewHandler = context.getApplication().getViewHandler();
			return viewHandler.getActionURL(context, manifest.getOfflineViewId());
		}
		else {
			return null;
		}
	}

	private static String getServiceWorkerUrl(FacesContext context) {
		return context.getExternalContext().encodeResourceURL(FacesLocal.createResource(context, OMNIFACES_LIBRARY_NAME, SERVICEWORKER_RESOURCE_NAME).getRequestPath());
	}

	private static String getServiceWorkerScope(FacesContext context) {
		return getRequestContextPath(context) + "/";
	}

	private static String getResourceUrl(FacesContext context, String libraryName, String resourceName) {
		Resource resource = FacesLocal.createResource(context, libraryName, resourceName);

		if (resource == null) {
			return null;
		}

		return resource.getRequestPath().replaceAll("([?&])v=.*?([&#]|$)", "$2"); // Strips the v= parameter indicating the cache bust version.
	}

}