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

import static java.util.ResourceBundle.getBundle;
import static org.omnifaces.util.Faces.getLocale;
import static org.omnifaces.util.Faces.getMessageBundle;
import static org.omnifaces.util.Utils.coalesce;

import java.util.ResourceBundle;


/**
 * Collection of constants and utility methods for OmniFaces internals.
 *
 * @author Bauke Scholtz
 * @since 2.5
 */
public final class OmniFaces {

	// Public constants -----------------------------------------------------------------------------------------------

	/** Returns the "omnifaces" resource library name. */
	public static final String LIBRARY_NAME = "omnifaces";

	/** Returns the "omnifaces.js" main script name. */
	public static final String SCRIPT_NAME = "omnifaces.js";

	/** Returns the "unload.js" unload script name. */
	public static final String UNLOAD_SCRIPT_NAME = "unload.js";

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String VERSION = OmniFaces.class.getPackage() == null ? "UNKNOWN" : coalesce(OmniFaces.class.getPackage().getImplementationVersion(), "DEV-SNAPSHOT");
	private static final boolean SNAPSHOT = VERSION.contains("-"); // -SNAPSHOT, -RCx
	private static final Long STARTUP_TIME = System.currentTimeMillis();
	private static final String DEFAULT_MESSAGE_BUNDLE = "org.omnifaces.messages";

	// Constructors ---------------------------------------------------------------------------------------------------

	private OmniFaces() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Returns OmniFaces version.
	 * This is extracted from Implementation-Version field of /META-INF/MANIFEST.MF file of omnifaces.jar file.
	 * Release versions will return version in format <code>2.7.4</code>.
	 * Snapshot versions will return version in format <code>2.7.4-SNAPSHOT</code>.
	 * Local development versions (because MANIFEST.MF entry is missing) will return version in format <code>DEV-SNAPSHOT</code>.
	 * Unknown versions (because package is missing) will return version in format <code>UNKNOWN</code>.
	 * @return OmniFaces version.
	 */
	public static String getVersion() {
		return VERSION;
	}

	/**
	 * Returns whether current OmniFaces version is a SNAPSHOT or RC version.
	 * @return Whether current OmniFaces version is a SNAPSHOT or RC version.
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