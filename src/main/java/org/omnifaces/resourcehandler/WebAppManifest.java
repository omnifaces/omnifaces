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

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.omnifaces.util.FacesLocal.createResource;
import static org.omnifaces.util.ResourcePaths.addLeadingSlashIfNecessary;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.formatURLWithQueryString;
import static org.omnifaces.util.Utils.openConnection;

import java.net.URLConnection;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import jakarta.faces.application.Resource;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.context.FacesContext;

import org.omnifaces.config.WebXml;
import org.omnifaces.util.Faces;
import org.omnifaces.util.FacesLocal;

/**
 * <p>
 * Please refer to {@link PWAResourceHandler} for usage instructions.
 *
 * @author Bauke Scholtz
 * @since 3.6
 * @see PWAResourceHandler
 */
public abstract class WebAppManifest {

	// Constants ------------------------------------------------------------------------------------------------------

	/**
	 * Enumeration of text direction types, to be used in {@link WebAppManifest#getDir()}.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/dir">https://developer.mozilla.org/en-US/docs/Web/Manifest/dir</a>
	 */
	protected enum Dir {
		LTR, RTL, AUTO;

		private final String value;

		private Dir() {
			value = name().toLowerCase();
		}

		@Override
		public String toString() {
			return value;
		}
	}

	/**
	 * Enumeration of display modes, to be used in {@link WebAppManifest#getDisplay()}.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/display">https://developer.mozilla.org/en-US/docs/Web/Manifest/display</a>
	 */
	protected enum Display {
		FULLSCREEN, STANDALONE, MINIMAL_UI, BROWSER;

		private final String value;

		private Display() {
			value = name().toLowerCase().replace('_', '-');
		}

		@Override
		public String toString() {
			return value;
		}
	}

	/**
	 * Enumeration of orientation modes, to be used in {@link WebAppManifest#getOrientation()}.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/orientation">https://developer.mozilla.org/en-US/docs/Web/Manifest/orientation</a>
	 */
	protected enum Orientation {
		ANY, NATURAL, LANDSCAPE, LANDSCAPE_PRIMARY, LANDSCAPE_SECONDARY, PORTRAIT, PORTRAIT_PRIMARY, PORTRAIT_SECONDARY;

		private final String value;

		private Orientation() {
			value = name().toLowerCase().replace('_', '-');
		}

		@Override
		public String toString() {
			return value;
		}
	}

	/**
	 * Enumeration of categories, to be used in {@link WebAppManifest#getCategories()}.
	 * @see <a href="https://github.com/w3c/manifest/wiki/Categories">https://github.com/w3c/manifest/wiki/Categories</a>
	 */
	protected enum Category {
		BOOKS, BUSINESS, EDUCATION, ENTERTAINMENT, FINANCE, FITNESS, FOOD, GAMES, GOVERNMENT, HEALTH, KIDS, LIFESTYLE,
		MAGAZINES, MEDICAL, MUSIC, NAVIGATION, NEWS, PERSONALIZATION, PHOTO, POLITICS, PRODUCTIVITY, SECURITY, SHOPPING,
		SOCIAL, SPORTS, TRAVEL, UTILITIES, WEATHER;

		private final String value;

		private Category() {
			value = name().toLowerCase();
		}

		@Override
		public String toString() {
			return value;
		}
	}

	/**
	 * Enumeration of related application platforms, to be used in {@link RelatedApplication#getPlatform()}.
	 * @see <a href="https://github.com/w3c/manifest/wiki/Platforms">https://github.com/w3c/manifest/wiki/Platforms</a>
	 */
	protected enum Platform {
		CHROME_WEB_STORE, PLAY, ITUNES, WINDOWS;

		private final String value;

		private Platform() {
			value = name().toLowerCase().replace('_', '-');
		}

		@Override
		public String toString() {
			return value;
		}
	}


	// Properties -----------------------------------------------------------------------------------------------------

	private Collection<String> cacheableViewIds;


	// Required -------------------------------------------------------------------------------------------------------

	/**
	 * Returns the name of your web application.
	 * @return The name of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/name">https://developer.mozilla.org/en-US/docs/Web/Manifest/name</a>
	 */
	public abstract String getName();

	/**
	 * Returns the icons of your web application.
	 * @return The icons of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/icons">https://developer.mozilla.org/en-US/docs/Web/Manifest/icons</a>
	 */
	public abstract Collection<ImageResource> getIcons();


	// Recommended ----------------------------------------------------------------------------------------------------

	/**
	 * Returns the default language of your web application. The default implementation returns {@link Faces#getDefaultLocale()} with a fallback of {@link Locale#getDefault()}.
	 * @return The default language of your web application.
	 */
	public String getLang() {
		return coalesce(Faces.getDefaultLocale(), Locale.getDefault()).toLanguageTag();
	}

	/**
	 * Returns the default text direction type of your web application. The default implementation returns {@link Dir#AUTO}.
	 * @return The default text direction type of your web application.
	 */
	public Dir getDir() {
		return Dir.AUTO;
	}

	/**
	 * Returns the default display mode of your web application. The default implementation returns {@link Display#BROWSER}.
	 * @return The default display mode of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/display">https://developer.mozilla.org/en-US/docs/Web/Manifest/display</a>
	 */
	public Display getDisplay() {
		return Display.BROWSER;
	}

	/**
	 * Returns the default orientation mode of your web application. The default implementaiton returns {@link Orientation#ANY}.
	 * @return The default orientation mode of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/orientation">https://developer.mozilla.org/en-US/docs/Web/Manifest/orientation</a>
	 */
	public Orientation getOrientation() {
		return Orientation.ANY;
	}

	/**
	 * Returns the default home URL of your web application. The default implementation returns {@link Faces#getRequestBaseURL()}.
	 * @return The default home URL of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/start_url">https://developer.mozilla.org/en-US/docs/Web/Manifest/start_url</a>
	 */
	public String getStartUrl() {
		return Faces.getRequestBaseURL();
	}

	/**
	 * Returns a collection of Faces view IDs which should be cached via the service worker so that they are available offline.
	 * The default implementation returns Faces view IDs derived from {@link WebXml#getWelcomeFiles()}.
	 * If this method returns an empty collection, then no service worker will be generated.
	 * @return A collection of Faces view IDs which should be cached via the service worker so that they are available offline.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Offline_Service_workers">https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Offline_Service_workers</a>
	 * @since 3.7
	 */
	protected Collection<String> getCacheableViewIds() {
		if (cacheableViewIds == null) {
			FacesContext context = Faces.getContext();
			String contextPath = FacesLocal.getRequestContextPath(context);

			Set<String> welcomeFileURLs = WebXml.instance().getWelcomeFiles().stream().map(welcomeFile -> contextPath + addLeadingSlashIfNecessary(welcomeFile)).collect(toSet());
			welcomeFileURLs.add(contextPath + "/");

			ViewHandler viewHandler = context.getApplication().getViewHandler();
			cacheableViewIds = viewHandler.getViews(context, "/").filter(viewId -> welcomeFileURLs.contains(viewHandler.getActionURL(context, viewId))).collect(toSet());
		}

		return cacheableViewIds;
	}

	/**
	 * Returns the Faces view ID which should represent the "You're offline!" error page.
	 * The default implementation returns <code>null</code>, meaning that there is no such one.
	 * If {@link #getCacheableViewIds()} returns an empty collection, then this method will be ignored.
	 * @return the Faces view ID which should represent the "You're offline!" error page.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Offline_Service_workers">https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Offline_Service_workers</a>
	 * @since 3.7
	 */
	protected String getOfflineViewId() {
		return null;
	}


	// Optional -------------------------------------------------------------------------------------------------------

	/**
	 * Returns the scope of this manifest. The default implementation returns <code>null</code>.
	 * @return The scope of this manifest.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/scope">https://developer.mozilla.org/en-US/docs/Web/Manifest/scope</a>
	 */
	public String getScope() {
		return null;
	}

	/**
	 * Returns the short name of your web application. The default implementation returns <code>null</code>.
	 * @return The short name of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/short_name">https://developer.mozilla.org/en-US/docs/Web/Manifest/short_name</a>
	 */
	public String getShortName() {
		return null;
	}

	/**
	 * Returns the description of your web application. The default implementation returns <code>null</code>.
	 * @return The description of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/description">https://developer.mozilla.org/en-US/docs/Web/Manifest/description</a>
	 */
	public String getDescription() {
		return null;
	}

	/**
	 * Returns the theme color of your web application. The default implementation returns <code>null</code>.
	 * @return The theme color of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/theme_color">https://developer.mozilla.org/en-US/docs/Web/Manifest/theme_color</a>
	 */
	public String getThemeColor() {
		return null;
	}

	/**
	 * Returns the placeholder background color of your web application. The default implementation returns <code>null</code>.
	 * @return The placeholder background color of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/background_color">https://developer.mozilla.org/en-US/docs/Web/Manifest/background_color</a>
	 */
	public String getBackgroundColor() {
		return null;
	}

	/**
	 * Returns a collection of categories where your web application supposedly belongs to. The default implementation returns an empty set.
	 * @return A collection of categories where your web application supposedly belongs to.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/categories">https://developer.mozilla.org/en-US/docs/Web/Manifest/categories</a>
	 */
	public Collection<Category> getCategories() {
		return emptySet();
	}

	/**
	 * Returns the IARC rating ID of your web application. The default implementation returns <code>null</code>.
	 * @return The IARC rating ID of your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/iarc_rating_id">https://developer.mozilla.org/en-US/docs/Web/Manifest/iarc_rating_id</a>
	 */
	public String getIarcRatingId() {
		return null;
	}

	/**
	 * Returns a collection of related (native) applications that provide similar/equivalent functionality as your web application. The default implementation returns an empty set.
	 * @return A collection of related (native) applications that provide similar/equivalent functionality as your web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/related_applications">https://developer.mozilla.org/en-US/docs/Web/Manifest/related_applications</a>
	 */
	public Collection<RelatedApplication> getRelatedApplications() {
		return emptySet();
	}

	/**
	 * Returns whether the applications listed in {@link #getRelatedApplications()} should be preferred over the web application. The default implementation returns <code>false</code>.
	 * @return Whether the applications listed in {@link #getRelatedApplications()} should be preferred over the web application.
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Manifest/prefer_related_applications">https://developer.mozilla.org/en-US/docs/Web/Manifest/prefer_related_applications</a>
	 */
	public boolean isPreferRelatedApplications() {
		return false;
	}


	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * To be used in {@link WebAppManifest#getIcons()}.
	 */
	protected static final class ImageResource {

		private String src;
		private String sizes;
		private String type;

		private ImageResource(String resourceIdentifier, Size... sizes) {
			requireNonNull(resourceIdentifier, "resourceIdentifier");
			FacesContext context = Faces.getContext();
			Resource resource = createResource(context, new ResourceIdentifier(resourceIdentifier));

			if (resource == null) {
				throw new IllegalArgumentException("Cannot find resource '" + resourceIdentifier + "'");
			}

			String requestPath = resource.getRequestPath();
			URLConnection connection = openConnection(context, resource);

			if (connection != null) {
				requestPath = formatURLWithQueryString(requestPath, "v=" + connection.getLastModified());
			}

			this.src = requestPath;
			this.sizes = stream(sizes).map(Size::getValue).distinct().collect(joining(" "));
			this.type = resource.getContentType();
		}

		/**
		 * Creates image resource of given resource identifier and sizes.
		 * @param resourceIdentifier The Faces resource identifier. E.g. <code>library:path/name.png</code>
		 * @param sizes The supported sizes of this image resource.
		 * @return Image resource of given resource identifier and sizes.
		 * @throws NullPointerException When resource identifier is null.
		 * @throws IllegalArgumentException When resource cannot be found.
		 */
		public static ImageResource of(String resourceIdentifier, Size... sizes) {
			return new ImageResource(resourceIdentifier, sizes);
		}

		/**
		 * Returns the source of this image resource.
		 * @return The source of this image resource.
		 */
		public String getSrc() {
			return src;
		}

		/**
		 * Returns the supported sizes of this image resource.
		 * @return The supported sizes of this image resource.
		 */
		public String getSizes() {
			return sizes;
		}

		/**
		 * Returns the content type of this image resource.
		 * @return The content type of this image resource.
		 */
		public String getType() {
			return type;
		}

		@Override
		public boolean equals(Object object) {
			// Basic checks.
			if (object == this) {
				return true;
			}
			if (object == null || object.getClass() != getClass()) {
				return false;
			}

			// Property checks.
			ImageResource other = (ImageResource) object;
			return Objects.equals(src, other.src)
				&& Objects.equals(sizes, other.sizes)
				&& Objects.equals(type, other.type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(src, sizes, type);
		}
	}

	/**
	 * To be used in {@link ImageResource#getSizes()}.
	 */
	protected static final class Size {

		/** 16x16 */
		public static final Size SIZE_16 = Size.of(16);

		/** 32x32 */
		public static final Size SIZE_32 = Size.of(32);

		/** 48x48 */
		public static final Size SIZE_48 = Size.of(48);

		/** 64x64 */
		public static final Size SIZE_64 = Size.of(64);

		/** 72x72 */
		public static final Size SIZE_72 = Size.of(72);

		/** 96x96 */
		public static final Size SIZE_96 = Size.of(96);

		/** 120x120 */
		public static final Size SIZE_120 = Size.of(120);

		/** 128x128 */
		public static final Size SIZE_128 = Size.of(128);

		/** 144x144 */
		public static final Size SIZE_144 = Size.of(144);

		/** 152x152 */
		public static final Size SIZE_152 = Size.of(152);

		/** 168x168 */
		public static final Size SIZE_168 = Size.of(168);

		/** 180x180 */
		public static final Size SIZE_180 = Size.of(180);

		/** 192x192 */
		public static final Size SIZE_192 = Size.of(192);

		/** 256x256 */
		public static final Size SIZE_256 = Size.of(256);

		/** 384x384 */
		public static final Size SIZE_384 = Size.of(384);

		/** 512x512 */
		public static final Size SIZE_512 = Size.of(512);

		private String value;

		private Size(int width, int height) {
			value = width + "x" + height;
		}

		/**
		 * Creates a size having same width and height of given value.
		 * @param value The value.
		 * @return A size having same width and height of given value.
		 * @throws IllegalArgumentException When value is 0 or less.
		 */
		public static Size of(int value) {
			if (value <= 0) {
				throw new IllegalArgumentException("size");
			}
			return new Size(value, value);
		}

		/**
		 * Creates a size of given width and height.
		 * @param width The width.
		 * @param height The height.
		 * @return A size of given width and height.
		 * @throws IllegalArgumentException When width or height is 0 or less.
		 */
		public static Size of(int width, int height) {
			if (width <= 0) {
				throw new IllegalArgumentException("width");
			}
			if (height <= 0) {
				throw new IllegalArgumentException("height");
			}
			return new Size(width, height);
		}

		/**
		 * Returns the value of this size.
		 * @return The value of this size.
		 */
		public String getValue() {
			return value;
		}

		@Override
		public boolean equals(Object object) {
			// Basic checks.
			if (object == this) {
				return true;
			}
			if (object == null || object.getClass() != getClass()) {
				return false;
			}

			// Property checks.
			Size other = (Size) object;
			return Objects.equals(value, other.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}
	}

	/**
	 * To be used in {@link WebAppManifest#getRelatedApplications()}
	 */
	protected static final class RelatedApplication {

		private Platform platform;
		private String url;
		private String id;

		private RelatedApplication(Platform platform, String url, String id) {
			requireNonNull(platform, "platform");
			requireNonNull(url, "url");

			this.platform = platform;
			this.url = url;
			this.id = id;
		}

		/**
		 * Creates a related application of given platform and URL.
		 * @param platform The platform on which the application can be found.
		 * @param url The URL at which the application can be found.
		 * @return A related application of given platform and URL.
		 * @throws NullPointerException When platform or url is null.
		 */
		public static RelatedApplication of(Platform platform, String url) {
			return of(platform, url, null);
		}

		/**
		 * Creates a related application of given platform and URL and ID.
		 * @param platform The platform on which the application can be found.
		 * @param url The URL at which the application can be found.
		 * @param id The ID used to represent the application on the specified platform.
		 * @return A related application of given platform and URL and ID.
		 * @throws NullPointerException When platform or url is null.
		 */
		public static RelatedApplication of(Platform platform, String url, String id) {
			return new RelatedApplication(platform, url, id);
		}

		/**
		 * Returns the platform on which the application can be found.
		 * @return The platform on which the application can be found.
		 */
		public Platform getPlatform() {
			return platform;
		}

		/**
		 * Returns the URL at which the application can be found.
		 * @return The URL at which the application can be found.
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * Returns the ID used to represent the application on the specified platform.
		 * @return The ID used to represent the application on the specified platform.
		 */
		public String getId() {
			return id;
		}

		@Override
		public boolean equals(Object object) {
			// Basic checks.
			if (object == this) {
				return true;
			}
			if (object == null || object.getClass() != getClass()) {
				return false;
			}

			// Property checks.
			RelatedApplication other = (RelatedApplication) object;
			return Objects.equals(platform, other.platform)
				&& Objects.equals(url, other.url)
				&& Objects.equals(id, other.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(platform, url, id);
		}
	}

}
