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
import javax.imageio.ImageIO;
import javax.servlet.http.Part;

import org.omnifaces.util.Servlets;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:inputFile&gt;</code> is a component that extends the standard <code>&lt;h:inputFile&gt;</code> and
 * adds support for <code>multiple</code>, <code>directory</code> and <code>accept</code> attributes. Additionally, it
 * makes sure that the value of HTML file input element is never rendered. The standard <code>&lt;h:inputFile&gt;</code>
 * renders <code>Part#toString()</code> to it which is unnecessary.
 *
 * <h3>Usage</h3>
 * <p>
 * You can use it the same way as <code>&lt;h:inputFile&gt;</code>, you only need to change <code>h:</code> into
 * <code>o:</code> to get the extra support for <code>multiple</code>, <code>directory</code> and <code>accept</code>
 * attributes. Here's are some usage examples.
 * <p>
 * <strong>Single file selection</strong>
 * <p>
 * It is basically not different from <code>&lt;h:inputFile&gt;</code>. You might as good use it instead.
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile value="#{bean.file}" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <pre>
 * private Part file; // +getter+setter
 *
 * public void upload() {
 *     if (file != null) {
 *         System.out.println("Name: " + Servlets.getSubmittedFileName(file));
 *         System.out.println("Type: " + file.getContentType());
 *         System.out.println("Size: " + file.getSize());
 *     }
 * }
 * </pre>
 * <p>
 * Note that it's strongly recommended to use {@link Servlets#getSubmittedFileName(Part)} to obtain the submitted file
 * name to make sure that any path is stripped off. Some browsers are known to incorrectly include the client side path
 * or even a fake path along with the file name.
 * <p>
 * <strong>Multiple file selection</strong>
 * <p>
 * The <code>multiple</code> attribute can be set to <code>true</code> to enable multiple file selection.
 * With this setting the enduser can use control/command/shift keys to select multiple files.
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile value="#{bean.files}" multiple="true" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <pre>
 * private List&lt;Part&gt; files; // +getter+setter
 *
 * public void upload() {
 *     if (files != null) {
 *         for (Part file : files) {
 *             System.out.println("Name: " + Servlets.getSubmittedFileName(file));
 *             System.out.println("Type: " + file.getContentType());
 *             System.out.println("Size: " + file.getSize());
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * <strong>Folder selection</strong>
 * <p>
 * The <code>directory</code> attribute can be set to <code>true</code> to enable folder selection. This implicitly also
 * sets <code>multiple</code> attribute to <code>true</code> and renders an additional <code>webkitdirectory</code>
 * attribute to HTML for better browser compatibility.
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile value="#{bean.files}" directory="true" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <pre>
 * private List&lt;Part&gt; files; // +getter+setter
 *
 * public void upload() {
 *     if (files != null) {
 *         for (Part file : files) {
 *             System.out.println("Name: " + Servlets.getSubmittedFileName(file));
 *             System.out.println("Type: " + file.getContentType());
 *             System.out.println("Size: " + file.getSize());
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * Do note that this does not send physical folders, but only files contained in those folders.
 * <p>
 * <strong>Media type filtering</strong>
 * <p>
 * The <code>accept</code> attribute can be set with a comma separated string of media types of files to filter in
 * browse dialog. An overview of all registered media types can be found at
 * <a href="http://www.iana.org/assignments/media-types">IANA</a>.
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile value="#{bean.losslessImageFile}" accept="image/png,image/gif" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile value="#{bean.anyImageFile}" accept="image/*" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile value="#{bean.anyMediaFile}" accept="audio/*,image/*,video/*" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * Do note that this does not strictly validate the file content type, but it only filters based on the file extension.
 * You would still need to enforce server side validation on the desired media type(s). E.g. by just parsing it using
 * the tool suited for the specific media type such as {@link ImageIO#read(java.io.InputStream)} for image files and
 * then checking if it returns the expected result.
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
		multiple, directory, accept;
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
	 * This override returns null during render response as it doesn't make sense to render <code>Part#toString()</code>
	 * as value of file input, moreover it's for HTML security reasons discouraged to prefill the value of a file input
	 * even though browsers will ignore it.
	 */
	@Override
	public Object getValue() {
		return isRenderResponse() ? null : super.getValue();
	}

	/**
	 * This override will render <code>multiple</code>, <code>directory</code> and <code>accept</code> attributes
	 * accordingly. As the <code>directory</code> attribute is relatively new, for better browser compatibility the
	 * <code>webkitdirectory</code> attribute will also be written along it.
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

		writeAttribute(writer, this, "accept"); // http://caniuse.com/#feat=input-file-accept
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
	 * @param directory Whether or not to enable directory selection.
	 */
	public void setDirectory(boolean directory) {
		state.put(PropertyKeys.directory, directory);
	}

	/**
	 * Returns comma separated string of mime types of files to filter in browse dialog.
	 * @return Comma separated string of mime types of files to filter in browse dialog.
	 */
	public String getAccept() {
		return state.get(PropertyKeys.accept);
	}

	/**
	 * Returns comma separated string of media types of files to filter in browse dialog.
	 * @param accept Comma separated string of mime types of files to filter in browse dialog.
	 */
	public void setAccept(String accept) {
		state.put(PropertyKeys.accept, accept);
	}

}