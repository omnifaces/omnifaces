/*
 * Copyright 2016 OmniFaces.
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

import static java.util.ResourceBundle.getBundle;
import static org.omnifaces.util.Faces.getLocale;
import static org.omnifaces.util.Faces.getMessageBundle;

import java.util.ResourceBundle;


/**
 * Collection of utility methods for OmniFaces internals.
 *
 * @author Bauke Scholtz
 * @since 2.5
 */
public final class OmniFaces {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String VERSION = OmniFaces.class.getPackage().getSpecificationVersion().replaceAll("-\\d+$", "");
	private static final boolean SNAPSHOT = VERSION.endsWith("-SNAPSHOT");
	private static final Long STARTUP_TIME = System.currentTimeMillis();
	private static final String DEFAULT_MESSAGE_BUNDLE = "org.omnifaces.messages";

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Returns OmniFaces version. E.g. <code>2.5</code> or <code>2.5-SNAPSHOT</code>.
	 * @return OmniFaces version.
	 */
	public static String getVersion() {
		return VERSION;
	}

	/**
	 * Returns whether current OmniFaces version is a snapshot version.
	 * @return Whether current OmniFaces version is a snapshot version.
	 */
	public static boolean isSnapshot() {
		return SNAPSHOT;
	}

	/**
	 * Returns startup time in Epoch milli.
	 * @return Startup time in Epoch milli.
	 */
	public static long getStartupTime() {
		return STARTUP_TIME;
	}

	/**
	 * Returns resource bundle message associated with given key from application message bundle as identified by
	 * <code>&lt;message-bundle&gt;</code> in <code>faces-config.xml</code>, or if it is absent, then return it from
	 * OmniFaces internal <code>org.omnifaces.messages</code> bundle.
	 * @param key The message bundle key.
	 * @return Resource bundle message associated with given key.
	 */
	public static String getMessage(String key) {
		ResourceBundle messageBundle = getMessageBundle();

		if (messageBundle == null || !messageBundle.containsKey(key)) {
			messageBundle = getBundle(DEFAULT_MESSAGE_BUNDLE, getLocale());
		}

		return messageBundle.getString(key);
	}

}