/*
 * Copyright 2018 OmniFaces
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
package org.omnifaces.config;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.application.Application;

/**
 * <p>
 * This configuration interface parses the <code>/WEB-INF/faces-config.xml</code> and all
 * <code>/META-INF/faces-config.xml</code> files found in the classpath and offers methods to obtain information from
 * them which is not available by the standard JSF API.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
 * <pre>
 * // Get a mapping of all &lt;resource-bundle&gt; vars and base names.
 * Map&lt;String, String&gt; resourceBundles = FacesConfigXml.instance().getResourceBundles();
 * </pre>
 * <pre>
 * // Get an ordered list of all &lt;supported-locale&gt; values with &lt;default-locale&gt; as first item.
 * List&lt;Locale&gt; supportedLocales = FacesConfigXml.instance().getSupportedLocales();
 * </pre>
 * <p>
 * Since OmniFaces 3.1, you can if necessary even inject it.
 * <pre>
 * &#64;Inject
 * private FacesConfigXml facesConfigXml;
 * </pre>
 *
 * @author Bauke Scholtz
 * @author Michele Mariotti
 * @since 2.1
 * @see FacesConfigXmlSingleton
 */
public interface FacesConfigXml {

	// Enum singleton -------------------------------------------------------------------------------------------------

	/**
	 * Returns the lazily loaded enum singleton instance.
	 * @deprecated Since 3.1; Use {@link #instance()} instead.
	 */
	@Deprecated
	public static final FacesConfigXml INSTANCE = new FacesConfigXml() {

		@Override
		public Map<String, String> getResourceBundles() {
			return FacesConfigXml.instance().getResourceBundles();
		}

		@Override
		public List<Locale> getSupportedLocales() {
			return FacesConfigXml.instance().getSupportedLocales();
		}
	};

	/**
	 * Returns the lazily loaded enum singleton instance.
	 * @return The lazily loaded enum singleton instance.
	 * @since 3.1
	 */
	public static FacesConfigXml instance() {
		return FacesConfigXmlSingleton.INSTANCE;
	}

	// Getters --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a mapping of all resource bundle base names by var.
	 * @return A mapping of all resource bundle base names by var.
	 */
	public Map<String, String> getResourceBundles();

	/**
	 * Returns an ordered list of all supported locales on this application, with the default locale as the first
	 * item, if any. This will return an empty list if there are no locales definied in <code>faces-config.xml</code>.
	 * @return An ordered list of all supported locales on this application, with the default locale as the first
	 * item, if any.
	 * @see Application#getDefaultLocale()
	 * @see Application#getSupportedLocales()
	 * @since 2.2
	 */
	public List<Locale> getSupportedLocales();

}