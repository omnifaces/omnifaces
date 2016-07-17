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
package org.omnifaces.component.input;

import static java.lang.Boolean.FALSE;
import static org.omnifaces.util.Faces.isRenderResponse;
import static org.omnifaces.util.FacesLocal.getRequestParts;
import static org.omnifaces.util.Renderers.writeAttribute;

import java.io.IOException;

import javax.faces.component.FacesComponent;
import javax.faces.component.html.HtmlInputFile;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.ConverterException;
import javax.servlet.http.Part;

import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:inputFile&gt;</code> is a component that extends the standard <code>&lt;h:inputFile&gt;</code> and
 * adds support for <code>multiple</code> and <code>directory</code> attributes.
 *
 * @author Bauke Scholtz
 * @since 2.5
 */
@FacesComponent(InputFile.COMPONENT_TYPE)
public class InputFile extends HtmlInputFile {

	// Public constants -----------------------------------------------------------------------------------------------

	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.InputFile";

	// Private constants ----------------------------------------------------------------------------------------------

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		multiple, directory;
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * This override checks if multi file upload was enabled and if so, then return a collection of parts instead of
	 * only the last part as done in h:inputFile.
	 */
	@Override
	protected Object getConvertedValue(FacesContext context, Object newSubmittedValue) throws ConverterException {
		Object convertedValue = super.getConvertedValue(context, newSubmittedValue);

		if ((convertedValue instanceof Part) && isMultiple()) {
			return getRequestParts(context, ((Part) convertedValue).getName());
		}

		return convertedValue;
	}

	/**
	 * This override returns null during render response as it doesn't make sense to print {@link Part#toString()} as
	 * value of file input, moreover it's for HTML security reasons disallowed to prefill the value of a file input
	 * even though browsers will ignore it.
	 */
	@Override
	public Object getValue() {
		return isRenderResponse() ? null : super.getValue();
	}

	/**
	 * This override will render <code>multiple</code> and <code>directory</code> attributes accordingly. As the
	 * <code>directory</code> attribute is relatively new, for better compatibility the <code>webkitdirectory</code>
	 * attribute will also be written.
	 */
	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();

		if (isMultiple()) {
			writeAttribute(writer, this, "multiple"); // http://caniuse.com/#feat=input-file-multiple
		}

		if (isDirectory()) {
			writeAttribute(writer, this, "directory"); // Firefox 46+ (Firefox 42-45 requires enabling via about:config).
			writeAttribute(writer, this, "directory", "webkitdirectory"); // Chrome 11+, Safari 4+ and Edge.
		}

		super.encodeEnd(context);
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns whether or not to allow multiple file selection.
	 * This implicitly returns true when directory selection is enabled.
	 * @return Whether or not to allow multiple file selection.
	 */
	public boolean isMultiple() {
		return state.get(PropertyKeys.multiple, isDirectory());
	}

	/**
	 * Sets whether or not to allow multiple file selection.
	 * @param multiple Whether or not to allow multiple file selection.
	 */
	public void setMultiple(boolean multiple) {
		state.put(PropertyKeys.multiple, multiple);
	}

	/**
	 * Returns whether or not to enable directory selection.
	 * @return Whether or not to enable directory selection.
	 */
	public boolean isDirectory() {
		return state.get(PropertyKeys.directory, FALSE);
	}

	/**
	 * Sets whether or not to enable directory selection.
	 * Do note that this does not send physical folders, but only files contained in those folders.
	 * @param directory Whether or not to enable directory selection.
	 */
	public void setDirectory(boolean directory) {
		state.put(PropertyKeys.directory, directory);
	}

}