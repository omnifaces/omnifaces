/*
 * Copyright 2014 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.taghandler;

import static org.omnifaces.util.FacesLocal.redirect;
import static org.omnifaces.util.FacesLocal.responseSendError;
import static org.omnifaces.util.Messages.addFlashGlobalError;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.PostValidateEvent;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.omnifaces.util.Faces;

/**
 * <p>
 * <code>&lt;o:viewParamValidationFailed&gt;</code> allows the developer to handle a view parameter validation failure
 * with either a redirect or a HTTP error status, optionally with respectively a flash message or HTTP error message.
 * This tag can be placed inside <code>&lt;f:metadata&gt;</code> or <code>&lt;f|o:viewParam&gt;</code>. When placed in
 * <code>&lt;f|o:viewParam&gt;</code>, then it will be applied when the particular view parameter has a validation
 * error as per {@link UIViewParameter#isValid()}. When placed in <code>&lt;f:metadata&gt;</code>, and no one view
 * parameter has already handled the validation error via its own  <code>&lt;o:viewParamValidationFailed&gt;</code>,
 * then it will be applied when there's a general validation error as per {@link FacesContext#isValidationFailed()}.
 * <p>
 * The <code>sendRedirect</code> attribute uses under the covers {@link Faces#redirect(String, String...)} to send the
 * redirect, so the same rules as to scheme and leading slash apply here.
 * The <code>sendError</code> attribute uses under the covers {@link Faces#responseSendError(int, String)} to send the
 * error, so you can customize HTTP error pages via <code>&lt;error-page&gt;</code> entries in <code>web.xml</code>,
 * otherwise the server-default one will be displayed instead.
 *
 * <h3>Examples</h3>
 * <p>
 * With the example below, when at least one view param is absent, then the client will be returned a HTTP 400 error.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;f:viewParam name="foo" required="true" /&gt;
 *     &lt;f:viewParam name="bar" required="true" /&gt;
 *     &lt;o:viewParamValidationFailed sendError="400" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * With the example below, only when the "foo" parameter is absent, then the client will be redirected to "login.xhtml".
 * When the "bar" parameter is absent, nothing new will happen. The process will proceed "as usual". I.e. the validation
 * error will end up as a faces message in the current view the usual way.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;f:viewParam name="foo" required="true"&gt;
 *         &lt;o:viewParamValidationFailed sendRedirect="login.xhtml" /&gt;
 *     &lt;/f:viewParam&gt;
 *     &lt;f:viewParam name="bar" required="true" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * With the example below, only when the "foo" parameter is absent, regardless of the "bar" or "baz" parameters, then
 * the client will be returned a HTTP 401 error. When the "foo" parameter is present, but either "bar" or "baz"
 * parameter is absent, then the client will be redirected to "search.xhtml".
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;f:viewParam name="foo" required="true"&gt;
 *         &lt;o:viewParamValidationFailed sendError="401" /&gt;
 *     &lt;/f:viewParam&gt;
 *     &lt;f:viewParam name="bar" required="true" /&gt;
 *     &lt;f:viewParam name="baz" required="true" /&gt;
 *     &lt;o:viewParamValidationFailed sendRedirect="search.xhtml" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * In a nutshell: the one nested in <code>&lt;f:viewParam&gt;</code> takes precedence over the one nested in
 * <code>&lt;f:metadata&gt;</code>. Also, when there are multiple <code>&lt;f:viewParam&gt;</code> tags with a
 * <code>&lt;o:viewParamValidationFailed&gt;</code>, then they will be applied in the same order as they are declared
 * in the view.
 *
 * <h3>Messaging</h3>
 * <p>
 * By default, the first occurring validation message on the parent component will be copied, or when there is none,
 * then the first occurring global message will be copied. When <code>sendRedirect</code> is used, then it will be set
 * as a global flash error message. When <code>sendError</code> is used, then it will be set as HTTP status message.
 * <p>
 * You can override this message by explicitly specifying the <code>message</code> attribute. This is applicable on
 * both <code>sendRedirect</code> and <code>sendError</code>.
 * <pre>
 * &lt;o:viewParamValidationFailed sendRedirect="search.xhtml" message="You need to perform a search." /&gt;
 * ...
 * &lt;o:viewParamValidationFailed sendError="401" message="Authentication failed. You need to login." /&gt;
 * </pre>
 *
 * <h3>Design notes</h3>
 * <p>
 * You can technically nest multiple <code>&lt;o:viewParamValidationFailed&gt;</code> inside the same parent, but this
 * is not the documented approach and the behavior is unspecified.
 * <p>
 * You can <strong>not</strong> change the HTTP status code of a redirect. This is not a JSF limitation, but a HTTP
 * limitation. The status code of a redirect will <strong>always</strong> be the one of the response of the redirect.
 * If you intend to "redirect" with a different HTTP status code, then you should be using <code>sendError</code>
 * instead and specify the desired page as <code>&lt;error-page&gt;</code> in <code>web.xml</code>.
 *
 * @author Bauke Scholtz
 * @since 1.8
 */
public class ViewParamValidationFailed extends TagHandler implements ComponentSystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Pattern HTTP_STATUS_CODE = Pattern.compile("[1-9][0-9][0-9]");

	private static final String ERROR_INVALID_PARENT =
		"%s This be a child of UIViewRoot or UIViewParameter. Encountered parent of type '%s'."
			+ " You need to enclose o:viewParamValidationFailed in f:metadata or f:viewParam.";
	private static final String ERROR_DOUBLE_ATTRIBUTE =
		"%s You cannot specify both 'sendRedirect' and 'sendError' attributes. You can specify only one of them.";
	private static final String ERROR_REQUIRED_ATTRIBUTE =
		"%s This attribute is required, it cannot be set to null.";
	private static final String ERROR_INVALID_SENDERROR =
		"%s This attribute must represent a 3-digit HTTP status code. Encountered an invalid value '%s'.";

	// Properties -----------------------------------------------------------------------------------------------------

	private ValueExpression sendRedirect;
	private ValueExpression sendError;
	private ValueExpression message;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
    public ViewParamValidationFailed(TagConfig config) {
        super(config);
    }

	// Actions --------------------------------------------------------------------------------------------------------

    /**
     * If the parent component is new, and the current request is <strong>not</strong> a postback, and the parent
     * component is an instance of {@link UIViewRoot} or {@link UIViewParameter}, and all required attributes are set,
     * then subscribe to the {@link PostValidateEvent} of the parent component.
     * @throws IllegalArgumentException When the parent component is not an instance of {@link UIViewRoot} or
     * {@link UIViewParameter}, or when both <code>sendRedirect</code> and <code>sendError</code> attributes are
     * simultaneously specified, you can speficy only one of them.
     * @throws TagException When both <code>sendRedirect</code> and <code>sendError</code> attributes are absent, it
     * will then require the <code>sendRedirect</code> attribute.
     */
    @Override
    public void apply(FaceletContext context, UIComponent parent) throws IOException {
    	if (!ComponentHandler.isNew(parent) || context.getFacesContext().isPostback()) {
    		return;
    	}

    	if (!(parent instanceof UIViewRoot || parent instanceof UIViewParameter)) {
    		throw new IllegalArgumentException(
				String.format(ERROR_INVALID_PARENT, this, parent != null ? parent.getClass().getName() : null));
    	}

        sendError = getValueExpression(context, "sendError", false);
    	sendRedirect = getValueExpression(context, "sendRedirect", sendError == null);

    	if (sendRedirect != null && sendError != null) {
        	throw new IllegalArgumentException(String.format(ERROR_DOUBLE_ATTRIBUTE, this));
    	}

    	message = getValueExpression(context, "message", false);
    	parent.subscribeToEvent(PostValidateEvent.class, this);
    }

    /**
     * If the current request is <strong>not</strong> a postback and the current response is <strong>not</strong>
     * already completed, and validation on the parent component has failed (for {@link UIViewRoot} this is checked by
     * {@link FacesContext#isValidationFailed()} and for {@link UIViewParameter} this is checked by
     * {@link UIViewParameter#isValid()}), then send either a redirect or error depending on the tag attributes set.
     * @throws IllegalArgumentException When the <code>sendError</code> attribute does not represent a valid 3-digit
     * HTTP status code.
     */
    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        FacesContext context = FacesContext.getCurrentInstance();

        if (context.isPostback() || context.getResponseComplete()) {
        	return;
        }

        UIComponent component = event.getComponent();
    	boolean validationFailed = context.isValidationFailed();

    	if (component instanceof UIViewParameter) {
    		validationFailed = !((UIViewParameter) component).isValid();
    	}

    	if (!validationFailed) {
    		return;
    	}

    	String message = evaluateMessage(context, component);

        try {
    		if (sendRedirect != null) {
    			sendRedirect(context, message);
    		}
    		else {
    			sendError(context, message);
    		}
        }
        catch (IOException e) {
            throw new FacesException(e);
    	}
    }

    private String evaluateMessage(FacesContext context, UIComponent component) {
    	Iterator<FacesMessage> messages = context.getMessages(component.getClientId(context));

    	if (!messages.hasNext()) {
    		messages = context.getMessages(null);
    	}

    	if (messages.hasNext()) {
    		return messages.next().getSummary();
    	}

    	return evaluate(context.getELContext(), message, false);
    }

    private void sendRedirect(FacesContext context, String message) throws IOException {
		String evaluatedSendRedirect = evaluate(context.getELContext(), sendRedirect, true);

		if (message != null) {
			addFlashGlobalError(message);
		}

        redirect(context, evaluatedSendRedirect);
    }

    private void sendError(FacesContext context, String message) throws IOException {
		String evaluatedSendError = evaluate(context.getELContext(), sendError, false);

		if (evaluatedSendError == null || !HTTP_STATUS_CODE.matcher(evaluatedSendError).matches()) {
			throw new IllegalArgumentException(
				String.format(ERROR_INVALID_SENDERROR, sendError, evaluatedSendError));
		}

    	responseSendError(context, Integer.valueOf(evaluatedSendError), message);
    }

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Get the value of the tag attribute associated with the given attribute name as a value expression.
	 */
	private ValueExpression getValueExpression(FaceletContext context, String attributeName, boolean required) {
		TagAttribute attribute = required ? getRequiredAttribute(attributeName) : getAttribute(attributeName);
		return (attribute != null) ? attribute.getValueExpression(context, Object.class) : null;
	}

	/**
	 * Evaluate the given value expression as string.
	 */
	private static String evaluate(ELContext context, ValueExpression expression, boolean required) {
		Object value = expression.getValue(context);

		if (required && isEmpty(value)) {
			throw new IllegalArgumentException(String.format(ERROR_REQUIRED_ATTRIBUTE, expression.toString()));
		}

		return (value != null) ? value.toString() : null;
	}

}