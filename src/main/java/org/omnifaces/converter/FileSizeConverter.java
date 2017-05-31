/*
 * Copyright 2016 OmniFaces
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
package org.omnifaces.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import java.math.BigInteger;

/**
 * <p>
 * The <code>omnifaces.FileSizeConverter</code> is the JSF converter version of
 * <a href="https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FileUtils.html#byteCountToDisplaySize(java.math.BigInteger)">org.apache.commons.io.FileUtils.byteCountToDisplaySize(BigInteger)</a>.
 * <p>
 * Reverse conversion is invalid (String to Number), throws exception.
 * <h3>Usage</h3>
  * <pre>
 * &lt;h:outputText value="#{bean.fileSizeProperty}" converter="omnifaces.FileSizeConverter" /&gt;
 * </pre>
 *
 * @since 2.6
 */
@FacesConverter("omnifaces.FileSizeConverter")
public class FileSizeConverter implements Converter {

	private static final BigInteger ONE_KB = BigInteger.valueOf(1024L);
	private static final BigInteger ONE_MB = ONE_KB.multiply(ONE_KB);
	private static final BigInteger ONE_GB = ONE_KB.multiply(ONE_MB);
	private static final BigInteger ONE_TB = ONE_KB.multiply(ONE_GB);
	private static final BigInteger ONE_PB = ONE_KB.multiply(ONE_TB);
	private static final BigInteger ONE_EB = ONE_KB.multiply(ONE_PB);
	private static final BigInteger ONE_ZB = ONE_KB.multiply(ONE_EB);
//	private static final BigInteger ONE_YB = ONE_KB.multiply(ONE_ZB);

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		throw new ConverterException("Invalid conversion: String to Number");
	}

	private static String byteCountToDisplaySize(final BigInteger size) {
		String displaySize;

		if (size.divide(ONE_EB).compareTo(BigInteger.ZERO) > 0) {
			displaySize = String.valueOf(size.divide(ONE_EB)) + " EB";
		} else if (size.divide(ONE_PB).compareTo(BigInteger.ZERO) > 0) {
			displaySize = String.valueOf(size.divide(ONE_PB)) + " PB";
		} else if (size.divide(ONE_TB).compareTo(BigInteger.ZERO) > 0) {
			displaySize = String.valueOf(size.divide(ONE_TB)) + " TB";
		} else if (size.divide(ONE_GB).compareTo(BigInteger.ZERO) > 0) {
			displaySize = String.valueOf(size.divide(ONE_GB)) + " GB";
		} else if (size.divide(ONE_MB).compareTo(BigInteger.ZERO) > 0) {
			displaySize = String.valueOf(size.divide(ONE_MB)) + " MB";
		} else if (size.divide(ONE_KB).compareTo(BigInteger.ZERO) > 0) {
			displaySize = String.valueOf(size.divide(ONE_KB)) + " KB";
		} else {
			displaySize = String.valueOf(size) + " bytes";
		}
		return displaySize;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == null) {
			return null;
		}

		if (BigInteger.class.isInstance(value)) {
			return byteCountToDisplaySize((BigInteger) value);
		} else {
			return byteCountToDisplaySize(BigInteger.valueOf(((Number) value).longValue()));
		}
	}

}