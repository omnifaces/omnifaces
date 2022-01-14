/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.component.input;

import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME;
import static jakarta.faces.event.PhaseId.RENDER_RESPONSE;
import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static org.omnifaces.config.OmniFaces.OMNIFACES_EVENT_PARAM_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.config.OmniFaces.getMessage;
import static org.omnifaces.el.functions.Numbers.formatBytes;
import static org.omnifaces.util.Ajax.update;
import static org.omnifaces.util.Components.addFacesScriptResource;
import static org.omnifaces.util.Components.addScriptResource;
import static org.omnifaces.util.Components.getMessageComponent;
import static org.omnifaces.util.Components.getMessagesComponent;
import static org.omnifaces.util.Components.validateHasParent;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.Faces.getLocale;
import static org.omnifaces.util.Faces.isDevelopment;
import static org.omnifaces.util.Faces.isRenderResponse;
import static org.omnifaces.util.FacesLocal.getMimeType;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.getRequestParts;
import static org.omnifaces.util.FacesLocal.isAjaxRequest;
import static org.omnifaces.util.Messages.addError;
import static org.omnifaces.util.Servlets.getSubmittedFileName;
import static org.omnifaces.util.Utils.coalesce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.html.HtmlInputFile;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Part;

import org.omnifaces.util.Components;
import org.omnifaces.util.Servlets;
import org.omnifaces.util.State;
import org.omnifaces.util.Utils;

/**
 * <p>
 * The <code>&lt;o:inputFile&gt;</code> is a component that extends the standard <code>&lt;h:inputFile&gt;</code> and
 * adds support for <code>multiple</code>, <code>directory</code>, <code>accept</code> and <code>maxsize</code>
 * attributes, along with built-in server side validation on <code>accept</code> and <code>maxsize</code> attributes.
 * Additionally, it makes sure that the value of HTML file input element is never rendered. The standard
 * <code>&lt;h:inputFile&gt;</code> renders <code>Part#toString()</code> to it which is unnecessary.
 *
 * <h2>Usage</h2>
 * <p>
 * You can use it the same way as <code>&lt;h:inputFile&gt;</code>, you only need to change <code>h:</code> into
 * <code>o:</code> to get the extra support for <code>multiple</code>, <code>directory</code> and <code>accept</code>
 * attributes. Here's are some usage examples.
 *
 * <h3>Single file selection</h3>
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
 *         String name = Servlets.getSubmittedFileName(file);
 *         String type = file.getContentType();
 *         long size = file.getSize();
 *         InputStream content = file.getInputStream();
 *         // ...
 *     }
 * }
 * </pre>
 * <p>
 * Note that it's strongly recommended to use {@link Servlets#getSubmittedFileName(Part)} to obtain the submitted file
 * name to make sure that any path is stripped off. Some browsers are known to incorrectly include the client side path
 * or even a fake path along with the file name.
 *
 * <h3>Multiple file selection</h3>
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
 *             String name = Servlets.getSubmittedFileName(file);
 *             String type = file.getContentType();
 *             long size = file.getSize();
 *             InputStream content = file.getInputStream();
 *             // ...
 *         }
 *     }
 * }
 * </pre>
 *
 * <h3>Folder selection</h3>
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
 *             String name = Servlets.getSubmittedFileName(file);
 *             String type = file.getContentType();
 *             long size = file.getSize();
 *             InputStream content = file.getInputStream();
 *             // ...
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * Do note that this does not send physical folders, but only files contained in those folders.
 *
 * <h3>Media type filtering</h3>
 * <p>
 * The <code>accept</code> attribute can be set with a comma separated string of media types of files to filter in
 * browse dialog. An overview of all registered media types can be found at
 * <a href="https://www.iana.org/assignments/media-types">IANA</a>.
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile id="file" value="#{bean.losslessImageFile}" accept="image/png,image/gif" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 *     &lt;h:message for="file" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile id="file" value="#{bean.anyImageFile}" accept="image/*" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 *     &lt;h:message for="file" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile id="file" value="#{bean.anyMediaFile}" accept="audio/*,image/*,video/*" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 *     &lt;h:message for="file" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * This will also be validated in the server side using a built-in validator. Do note that the <code>accept</code>
 * attribute only filters in client side and validates in server side based on the file extension, and this does thus
 * not strictly validate the file's actual content. To cover that as well, you should in the bean's action method parse
 * the file's actual content using the tool suited for the specific media type, such as <code>ImageIO#read()</code> for
 * image files, and then checking if it returns the expected result.
 * <p>
 * The default message for server side validation of <code>accept</code> attribute is:
 * <blockquote>{0}: Media type of file ''{1}'' does not match ''{2}''</blockquote>
 * <p>
 * Where <code>{0}</code> is the component's label and <code>{1}</code> is the submitted file name and <code>{2}</code>
 * is the value of <code>accept</code> attribute.
 * <p>
 * You can override the default message by the <code>acceptMessage</code> attribute:
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile id="file" value="#{bean.anyImageFile}" accept="image/*" acceptMessage="File {1} is unacceptable!" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 *     &lt;h:message for="file" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * Or by the custom message bundle file as identified by <code>&lt;application&gt;&lt;message-bundle&gt;</code> in
 * <code>faces-config.xml</code>. The message key is <code>org.omnifaces.component.input.InputFile.accept</code>.
 * <pre>
 * org.omnifaces.component.input.InputFile.accept = File {1} is unacceptable!
 * </pre>
 *
 * <h3>File size validation</h3>
 * <p>
 * The <code>maxsize</code> attribute can be set with the maximum file size in bytes which will be validated on each
 * selected file in the client side if the client supports HTML5 File API. This validation will be performed by custom
 * JavaScript in client side instead of by JSF in server side. This only requires that there is a
 * <code>&lt;h:message&gt;</code> or <code>&lt;h:messages&gt;</code> component and that it has its <code>id</code> set.
 * <pre>
 * &lt;o:inputFile id="file" ... /&gt;
 * &lt;h:message id="messageForFile" for="file" /&gt; &lt;!-- This must have 'id' attribute set! --&gt;
 * </pre>
 * <p>
 * This way the client side can trigger JSF via an ajax request to update the message component with the client side
 * validation message. Noted should be that the file(s) will <strong>not</strong> be sent, hereby saving network
 * bandwidth.
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile id="file" value="#{bean.file}" maxsize="#{10 * 1024 * 1024}" /&gt; &lt;!-- 10MiB --&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 *     &lt;h:message id="messageForFile" for="file" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * This will also be validated in the server side using a built-in validator.
 * <p>
 * The default message for both client side and server side validation of <code>maxsize</code> attribute is:
 * <blockquote>{0}: Size of file ''{1}'' is larger than maximum of {2}</blockquote>
 * <p>
 * Where <code>{0}</code> is the component's label and <code>{1}</code> is the submitted file name and <code>{2}</code>
 * is the value of <code>maxsize</code> attribute.
 * <p>
 * You can override the default message by the <code>maxsizeMessage</code> attribute:
 * <pre>
 * &lt;h:form enctype="multipart/form-data"&gt;
 *     &lt;o:inputFile id="file" value="#{bean.file}" maxsize="#{10 * 1024 * 1024}" maxsizeMessage="File {1} is too big!" /&gt;
 *     &lt;h:commandButton value="Upload" action="#{bean.upload}" /&gt;
 *     &lt;h:message id="messageForFile" for="file" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * Or by the custom message bundle file as identified by <code>&lt;application&gt;&lt;message-bundle&gt;</code> in
 * <code>faces-config.xml</code>. The message key is <code>org.omnifaces.component.input.InputFile.maxsize</code>.
 * <pre>
 * org.omnifaces.component.input.InputFile.maxsize = File {1} is too big!
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 2.5
 */
@FacesComponent(InputFile.COMPONENT_TYPE)
public class InputFile extends HtmlInputFile {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The component type, which is {@value org.omnifaces.component.input.InputFile#COMPONENT_TYPE}. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.InputFile";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String SCRIPT_ONCHANGE = "if(!OmniFaces.InputFile.validate(event,this,'%s',%s))return false;%s";

	private static final String ERROR_MISSING_MESSAGE_COMPONENT =
		"o:inputFile client side validation of maxsize requires a message(s) component with a fixed ID.";

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		multiple, directory, accept, acceptMessage, maxsize, maxsizeMessage;
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());
	private transient Object transientSubmittedValue;
	private String messageComponentClientId;

	// Init -----------------------------------------------------------------------------------------------------------

	/**
	 * The constructor instructs JSF to register all scripts during the render response phase if necessary.
	 */
	public InputFile() {
		subscribeToRequestBeforePhase(RENDER_RESPONSE, this::registerScriptsIfNecessary);
	}

	private void registerScriptsIfNecessary() {
		// This is supposed to be declared via @ResourceDependency, but JSF 3 and Faces 4 use a different script
		// resource name which cannot be resolved statically.
		addFacesScriptResource(); // Required for jsf.ajax.request.
		addScriptResource(OMNIFACES_LIBRARY_NAME, OMNIFACES_SCRIPT_NAME);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * This override checks if client side validation on maxsize has failed and if multi file upload is enabled.
	 * If client side validation on maxsize has failed, then it will render the message. If multi file upload is
	 * enabled, then it will set all parts as submitted value instead of only the last part as done in h:inputFile.
	 */
	@Override
	public void decode(FacesContext context) {
		if ("validationFailed".equals(getRequestParameter(context, OMNIFACES_EVENT_PARAM_NAME))
			&& getClientId(context).equals(getRequestParameter(context, BEHAVIOR_SOURCE_PARAM_NAME)))
		{
			String fileName = getRequestParameter(context, "fileName");
			addError(getClientId(context), getMaxsizeMessage(), Components.getLabel(this), fileName, formatBytes(getMaxsize()));
			setValid(false);
			context.validationFailed();
			update(getMessageComponentClientId());
		}
		else {
			super.decode(context);
			Object submittedValue = getSubmittedValue();

			if (submittedValue instanceof Part && isMultiple()) {
				setSubmittedValue(getRequestParts(context, ((Part) submittedValue).getName()));
			}
		}
	}

	/**
	 * This override will convert the individual parts if multi file upload is enabled and collect only non-null parts
	 * having a non-empty file name and a file size above zero.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Object getConvertedValue(FacesContext context, Object submittedValue) {
		if (isMultiple()) {
			List<Part> convertedParts = new ArrayList<>();

			for (Part submittedPart : (List<Part>) submittedValue) {
				Object convertedPart = super.getConvertedValue(context, submittedPart);

				if (convertedPart instanceof Part && !Utils.isEmpty(convertedPart)) { // Do not import static! UIInput has an isEmpty() as well.
					convertedParts.add((Part) convertedPart);
				}
			}

			return unmodifiableList(convertedParts);
		}

		Object convertedPart = super.getConvertedValue(context, submittedValue);
		return Utils.isEmpty(convertedPart) ? null : convertedPart;
	}

	/**
	 * This override will server-side validate any <code>accept</code> and <code>maxsize</code> for each part.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void validateValue(FacesContext context, Object convertedValue) {
		Collection<Part> convertedParts = null;

		if (convertedValue instanceof Part) {
			convertedParts = singleton((Part) convertedValue);
		}
		else if (convertedValue instanceof List) {
			convertedParts = (List<Part>) convertedValue;
		}

		if (convertedParts != null) {
			validateParts(context, convertedParts);
		}

		if (isValid()) {
			super.validateValue(context, convertedValue);
		}
		else if (isAjaxRequest(context)) {
			update(getMessageComponentClientId());
		}
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
	 * <p>
	 * They're written as passthrough attributes because in Mojarra the <code>startElement()</code> takes place in
	 * {@link #encodeEnd(FacesContext)} instead of {@link #encodeBegin(FacesContext)}.
	 */
	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		Map<String, Object> passThroughAttributes = getPassThroughAttributes();

		if (isMultiple()) {
			passThroughAttributes.put("multiple", true); // https://caniuse.com/#feat=input-file-multiple
		}

		if (isDirectory()) {
			passThroughAttributes.put("directory", true); // Firefox 46+ (Firefox 42-45 requires enabling via about:config).
			passThroughAttributes.put("webkitdirectory", true); // Chrome 11+, Safari 4+ and Edge.
		}

		String accept = getAccept();

		if (accept != null) {
			passThroughAttributes.put("accept", accept); // https://caniuse.com/#feat=input-file-accept
		}

		Long maxsize = getMaxsize();

		if (maxsize != null) {
			validateHierarchy();
			setOnchange(format(SCRIPT_ONCHANGE, getMessageComponentClientId(), maxsize, coalesce(getOnchange(), "")));
		}

		super.encodeEnd(context);
	}

	/**
	 * Validate the component hierarchy. This should only be called when project stage is <code>Development</code>.
	 * @throws IllegalStateException When component hierarchy is wrong.
	 */
	protected void validateHierarchy() {
		validateHasParent(this, UIForm.class);

		if (isDevelopment() && getMessageComponentClientId() == null) {
			throw new IllegalStateException(ERROR_MISSING_MESSAGE_COMPONENT);
		}
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * An override which ensures that the Faces implementation being used doesn't save it in the state.
	 * The {@link Part} does namely not belong there.
	 */
	@Override
	public Object getSubmittedValue() {
		return transientSubmittedValue;
	}

	/**
	 * An override which ensures that the Faces implementation being used doesn't save it in the state.
	 * The {@link Part} does namely not belong there.
	 */
	@Override
	public void setSubmittedValue(Object submittedValue) {
		this.transientSubmittedValue = submittedValue;
	}

	/**
	 * Returns whether or not to allow multiple file selection.
	 * This implicitly defaults to <code>true</code> when <code>directory</code> attribute is <code>true</code>.
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
	 * When <code>true</code>, this implicitly defaults the <code>multiple</code> attribute to <code>true</code>.
	 * @param directory Whether or not to enable directory selection.
	 */
	public void setDirectory(boolean directory) {
		state.put(PropertyKeys.directory, directory);
	}

	/**
	 * Returns comma separated string of mime types of files to filter in client side file browse dialog.
	 * This is also validated in server side.
	 * @return Comma separated string of mime types of files to filter in client side file browse dialog.
	 */
	public String getAccept() {
		return state.get(PropertyKeys.accept);
	}

	/**
	 * Sets comma separated string of media types of files to filter in client side file browse dialog.
	 * @param accept Comma separated string of mime types of files to filter in client side file browse dialog.
	 */
	public void setAccept(String accept) {
		state.put(PropertyKeys.accept, accept);
	}

	/**
	 * Returns validation message to be displayed when the condition in <code>accept</code> attribute is violated.
	 * @return Validation message to be displayed when the condition in <code>accept</code> attribute is violated.
	 */
	public String getAcceptMessage() {
		return state.get(PropertyKeys.acceptMessage, getMessage(COMPONENT_TYPE + ".accept"));
	}

	/**
	 * Sets validation message to be displayed when the condition in <code>accept</code> attribute is violated.
	 * @param acceptMessage Validation message to be displayed when the condition in <code>accept</code> attribute is
	 * violated.
	 */
	public void setAcceptMessage(String acceptMessage) {
		state.put(PropertyKeys.acceptMessage, acceptMessage);
	}

	/**
	 * Returns maximum size in bytes for each selected file.
	 * This is validated in both client and server side.
	 * @return Maximum size in bytes for each selected file.
	 */
	public Long getMaxsize() {
		return state.get(PropertyKeys.maxsize);
	}

	/**
	 * Sets maximum size in bytes for each selected file.
	 * @param maxsize Maximum size in bytes for each selected file.
	 */
	public void setMaxsize(Long maxsize) {
		state.put(PropertyKeys.maxsize, maxsize);
	}

	/**
	 * Returns validation message to be displayed when the condition in <code>maxsize</code> attribute is violated.
	 * @return Validation message to be displayed when the condition in <code>maxsize</code> attribute is violated.
	 */
	public String getMaxsizeMessage() {
		return state.get(PropertyKeys.maxsizeMessage, getMessage(COMPONENT_TYPE + ".maxsize"));
	}

	/**
	 * Sets validation message to be displayed when the condition in <code>maxsize</code> attribute is violated.
	 * @param maxsizeMessage Validation message to be displayed when the condition in <code>maxsize</code> attribute is
	 * violated.
	 */
	public void setMaxsizeMessage(String maxsizeMessage) {
		state.put(PropertyKeys.maxsizeMessage, maxsizeMessage);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private void validateParts(FacesContext context, Collection<Part> parts) {
		String accept = getAccept();
		Long maxsize = getMaxsize();

		if (accept == null && maxsize == null) {
			return;
		}

		for (Part part : parts) {
			validatePart(context, part, accept, maxsize);
		}
	}

	private void validatePart(FacesContext context, Part part, String accept, Long maxsize) {
		String fileName = getSubmittedFileName(part);
		String message = null;
		String param = null;

		if (accept != null) {
			String contentType = isEmpty(fileName) ? part.getContentType() : getMimeType(context, fileName.toLowerCase(getLocale()));

			if (contentType == null || !contentType.matches(convertAcceptToRegex(accept))) {
				message = getAcceptMessage();
				param = accept;
			}
		}

		if (message == null && maxsize != null && part.getSize() > maxsize) {
			message = getMaxsizeMessage();
			param = formatBytes(maxsize);
		}

		if (message != null) {
			addError(getClientId(context), message, Components.getLabel(this), fileName, param);
			setValid(false);
		}
	}

	private String convertAcceptToRegex(String accept) {
		String[] parts = accept.replaceAll("\\s*", "").split("(?<=[*,])|(?=[*,])");
		StringBuilder regex = new StringBuilder();

		for (String part : parts) {
			switch (part) {
				case "*": regex.append(".*"); break;
				case ",": regex.append("|"); break;
				default: regex.append(Pattern.quote(part)); break;
			}
		}

		return regex.toString();
	}

	private String getMessageComponentClientId() {
		if (messageComponentClientId != null) {
			return messageComponentClientId;
		}

		UIComponent component = getMessageComponent(this);

		if (component == null || component.getId() == null) {
			component = getMessagesComponent();
		}

		messageComponentClientId = (component != null && component.getId() != null) ? component.getClientId() : null;
		return messageComponentClientId;
	}

}