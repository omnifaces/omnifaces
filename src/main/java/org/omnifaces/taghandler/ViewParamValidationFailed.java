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
package org.omnifaces.taghandler;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.omnifaces.util.Events.subscribeToRequestComponentEvent;
import static org.omnifaces.util.Facelets.getValueExpression;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.FacesLocal.redirect;
import static org.omnifaces.util.FacesLocal.responseSendError;
import static org.omnifaces.util.Messages.addFlashGlobalError;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIViewParameter;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.PostValidateEvent;
import jakarta.faces.view.facelets.ComponentHandler;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;

import org.omnifaces.util.Faces;

/**
 * <p>
 * <code>&lt;o:viewParamValidationFailed&gt;</code> allows the developer to handle a view parameter validation failure
 * with either a redirect or an HTTP error status, optionally with respectively a flash message or HTTP error message.
 * This tag can be placed inside <code>&lt;f:metadata&gt;</code> or <code>&lt;f|o:viewParam&gt;</code>. When placed in
 * <code>&lt;f|o:viewParam&gt;</code>, it will be applied when the particular view parameter has a validation
 * error as per {@link UIViewParameter#isValid()}. When placed in <code>&lt;f:metadata&gt;</code>, and no one view
 * parameter has already handled the validation error via its own <code>&lt;o:viewParamValidationFailed&gt;</code>,
 * it will be applied when there's a general validation error as per {@link FacesContext#isValidationFailed()}.
 * <p>
 * When the <code>sendRedirect</code> attribute is set, a call to {@link Faces#redirect(String, Object...)} is made
 * internally to send the redirect. So, the same rules as to scheme and leading slash apply here.
 * When the <code>sendError</code> attribute is set, a call to {@link Faces#responseSendError(int, String)} is made
 * internally to send the error. You can therefore customize HTTP error pages via <code>&lt;error-page&gt;</code>
 * entries in <code>web.xml</code>. Otherwise the server-default one will be displayed instead.
 *
 * <h2>&lt;f:viewParam required="true"&gt; fail</h2>
 * <p>
 * As a precaution; be aware that <code>&lt;f:viewParam required="true"&gt;</code> has a design error in current
 * Mojarra and MyFaces releases (as of now, Mojarra 2.2.7 and MyFaces 2.2.4). When the parameter is not specified in
 * the query string, it is retrieved as <code>null</code>, which causes an internal <code>isRequired()</code> check to
 * be performed instead of delegating the check to the standard <code>UIInput</code> implementation. This has the
 * consequence that <code>PreValidateEvent</code> and <code>PostValidateEvent</code> listeners are never invoked, which
 * the <code>&lt;o:viewParamValidationFailed&gt;</code> is actually relying on. This is fixed in
 * <code>&lt;o:viewParam&gt;</code>.
 *
 * <h2>Examples</h2>
 * <p>
 * In the example below the client will be presented an HTTP 400 error when at least one view param is absent.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:viewParam name="foo" required="true" /&gt;
 *     &lt;o:viewParam name="bar" required="true" /&gt;
 *     &lt;o:viewParamValidationFailed sendError="400" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * In the example below the client will be redirected to "login.xhtml" when the "foo" parameter is absent, regardless of
 * the "bar" parameter. When the "foo" parameter is present, but the "bar" parameter is absent, nothing new will happen.
 * The process will proceed "as usual". I.e. the validation error will end up as a faces message in the current view the
 * usual way.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:viewParam name="foo" required="true"&gt;
 *         &lt;o:viewParamValidationFailed sendRedirect="login.xhtml" /&gt;
 *     &lt;/o:viewParam&gt;
 *     &lt;o:viewParam name="bar" required="true" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * In the example below the client will be presented an HTTP 401 error when the "foo" parameter is absent, regardless of
 * the "bar" or "baz" parameters. When the "foo" parameter is present, but either the "bar" or "baz" parameter is
 * absent, the client will be redirected to "search.xhtml".
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:viewParam name="foo" required="true"&gt;
 *         &lt;o:viewParamValidationFailed sendError="401" /&gt;
 *     &lt;/o:viewParam&gt;
 *     &lt;o:viewParam name="bar" required="true" /&gt;
 *     &lt;o:viewParam name="baz" required="true" /&gt;
 *     &lt;o:viewParamValidationFailed sendRedirect="search.xhtml" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * In a nutshell: when there are multiple <code>&lt;o:viewParamValidationFailed&gt;</code> tags, they will be
 * applied in the same order as they are declared in the view. So, with the example above, the one nested in
 * <code>&lt;f|o:viewParam&gt;</code> takes precedence over the one nested in <code>&lt;f:metadata&gt;</code>.
 *
 * <h2>Messaging</h2>
 * <p>
 * By default, the first occurring faces message on the parent component will be copied, or when there is none then the
 * first occurring global faces message will be copied. When <code>sendRedirect</code> is used, it will be set
 * as a global flash error message. When <code>sendError</code> is used, it will be set as HTTP status message.
 * <p>
 * You can override this message by explicitly specifying the <code>message</code> attribute. This is applicable for
 * both <code>sendRedirect</code> and <code>sendError</code>.
 * <pre>
 * &lt;o:viewParamValidationFailed sendRedirect="search.xhtml" message="You need to perform a search." /&gt;
 * ...
 * &lt;o:viewParamValidationFailed sendError="401" message="Authentication failed. You need to login." /&gt;
 * </pre>
 *
 * <p>
 * Note, although all of above examples use <code>required="true"</code>, this does not mean that you can only use
 * <code>&lt;o:viewParamValidationFailed&gt;</code> in combination with <code>required="true"</code> validation. You
 * can use it in combination with any kind of conversion/validation on <code>&lt;f|o:viewParam&gt;</code>, even bean
 * validation.
 *
 * <h2>Design notes</h2>
 * <p>
 * You can technically nest multiple <code>&lt;o:viewParamValidationFailed&gt;</code> inside the same parent, but this
 * is not the documented approach and only the first one would be used.
 * <p>
 * You can <strong>not</strong> change the HTTP status code of a redirect. This is not a JSF limitation, but an HTTP
 * limitation. The status code of a redirect will <strong>always</strong> end up as the one of the redirected response.
 * If you intend to "redirect" with a different HTTP status code, then you should be using <code>sendError</code>
 * instead and specify the desired page as <code>&lt;error-page&gt;</code> in <code>web.xml</code>.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public class ViewParamValidationFailed extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Pattern HTTP_STATUS_CODE = Pattern.compile("[1-9][0-9][0-9]");

	private static final String ERROR_INVALID_PARENT =
		"%s This must be a child of UIViewRoot or UIViewParameter. Encountered parent of type '%s'."
			+ " You need to enclose it in f:metadata or f|o:viewParam.";
	private static final String ERROR_MISSING_ATTRIBUTE =
		"%s You need to specify either 'sendRedirect' or 'sendError' attribute.";
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
	 * If the parent component is an instance of {@link UIViewRoot} or {@link UIViewParameter} and is new, and the
	 * current request is <strong>not</strong> a postback, and <strong>not</strong> in render response, and all required
	 * attributes are set, then subscribe the parent component to the {@link PostValidateEvent}. This will invoke the
	 * {@link #processViewParamValidationFailed(ComponentSystemEvent)} method after validation.
	 * @throws IllegalStateException When the parent component is not an instance of {@link UIViewRoot} or
	 * {@link UIViewParameter}, or when there's already another <code>&lt;o:viewParamValidationFailed&gt;</code> tag
	 * registered on the same parent.
	 * @throws IllegalArgumentException When both <code>sendRedirect</code> and <code>sendError</code> attributes are
	 * missing or simultaneously specified.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!(parent instanceof UIViewRoot || parent instanceof UIViewParameter)) {
			throw new IllegalStateException(format(ERROR_INVALID_PARENT, this, parent.getClass().getName()));
		}

		FacesContext facesContext = context.getFacesContext();

		if (!ComponentHandler.isNew(parent) || facesContext.isPostback() || facesContext.getRenderResponse()) {
			return;
		}

		sendRedirect = getValueExpression(context, getAttribute("sendRedirect"), String.class);
		sendError = getValueExpression(context, getAttribute("sendError"), String.class);
		message = getValueExpression(context, getAttribute("message"), String.class);

		if (sendRedirect == null && sendError == null) {
			throw new IllegalArgumentException(format(ERROR_MISSING_ATTRIBUTE, this));
		}
		else if (sendRedirect != null && sendError != null) {
			throw new IllegalArgumentException(format(ERROR_DOUBLE_ATTRIBUTE, this));
		}

		subscribeToRequestComponentEvent(parent, PostValidateEvent.class, this::processViewParamValidationFailed);
	}

	/**
	 * If the current request is <strong>not</strong> a postback and the current response is <strong>not</strong>
	 * already completed, and validation on the parent component has failed (for {@link UIViewRoot} this is checked by
	 * {@link FacesContext#isValidationFailed()} and for {@link UIViewParameter} this is checked by
	 * {@link UIViewParameter#isValid()}), then send either a redirect or error depending on the tag attributes set.
	 * @param event The component system event.
	 * @throws IllegalArgumentException When the <code>sendError</code> attribute does not represent a valid 3-digit
	 * HTTP status code.
	 */
	protected void processViewParamValidationFailed(ComponentSystemEvent event) {
		FacesContext context = getContext();
		UIComponent component = event.getComponent();

		if (component instanceof UIViewParameter ? ((UIViewParameter) component).isValid() : !context.isValidationFailed()) {
			return; // Validation has not failed.
		}

		if (TRUE.equals(context.getAttributes().put(getClass().getName(), TRUE))) {
			return; // Validation fail has already been handled before. We can't send redirect or error multiple times.
		}

		String firstFacesMessage = coalesce(
			cleanupFacesMessagesAndGetFirst(context.getMessages(component.getClientId(context))), // Prefer own message.
			cleanupFacesMessagesAndGetFirst(context.getMessages(null)), // Then global messages.
			cleanupFacesMessagesAndGetFirst(context.getMessages()) // Cleanup remainder.
		);

		evaluateAttributesAndHandleSendRedirectOrError(context, firstFacesMessage);
	}

	private String cleanupFacesMessagesAndGetFirst(Iterator<FacesMessage> facesMessages) {
		String firstFacesMessage = null;

		while (facesMessages.hasNext()) {
			FacesMessage facesMessage = facesMessages.next();

			if (firstFacesMessage == null) {
				firstFacesMessage = facesMessage.getSummary();
			}

			facesMessages.remove(); // Avoid warning "Faces message has been enqueued but is not displayed".
		}

		return firstFacesMessage;
	}

	private void evaluateAttributesAndHandleSendRedirectOrError(FacesContext context, String defaultMessage) {
		ELContext elContext = context.getELContext();
		String evaluatedMessage = evaluate(elContext, message, false);

		if (evaluatedMessage == null) {
			evaluatedMessage = defaultMessage;
		}

		if (sendRedirect != null) {
			String evaluatedSendRedirect = evaluate(elContext, sendRedirect, true);

			if (!isEmpty(evaluatedMessage)) {
				addFlashGlobalError(evaluatedMessage);
			}

			redirect(context, evaluatedSendRedirect);
		}
		else {
			String evaluatedSendError = evaluate(elContext, sendError, true);

			if (!HTTP_STATUS_CODE.matcher(evaluatedSendError).matches()) {
				throw new IllegalArgumentException(
					format(ERROR_INVALID_SENDERROR, sendError, evaluatedSendError));
			}

			responseSendError(context, Integer.valueOf(evaluatedSendError), evaluatedMessage);
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Evaluate the given value expression as string.
	 */
	private static String evaluate(ELContext context, ValueExpression expression, boolean required) {
		Object value = (expression != null) ? expression.getValue(context) : null;

		if (required && isEmpty(value)) {
			throw new IllegalArgumentException(format(ERROR_REQUIRED_ATTRIBUTE, expression));
		}

		return (value != null) ? value.toString() : null;
	}

}