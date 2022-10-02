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
package org.omnifaces.eventlistener;

import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME;
import static jakarta.faces.event.PhaseId.ANY_PHASE;
import static jakarta.faces.event.PhaseId.RENDER_RESPONSE;
import static jakarta.faces.event.PhaseId.RESTORE_VIEW;
import static jakarta.faces.render.ResponseStateManager.VIEW_STATE_PARAM;
import static java.lang.System.nanoTime;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toList;
import static org.omnifaces.config.OmniFaces.OMNIFACES_EVENT_PARAM_NAME;
import static org.omnifaces.util.Components.getActionExpressionsAndListeners;
import static org.omnifaces.util.Components.getCurrentActionSource;
import static org.omnifaces.util.FacesLocal.getRemoteAddr;
import static org.omnifaces.util.FacesLocal.getRemoteUser;
import static org.omnifaces.util.FacesLocal.getRequest;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.getRequestParameterValuesMap;
import static org.omnifaces.util.FacesLocal.getRequestURIWithQueryString;
import static org.omnifaces.util.FacesLocal.getSessionId;
import static org.omnifaces.util.Utils.coalesce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;

import org.omnifaces.util.Components;
import org.omnifaces.util.Faces;

/**
 * <p>
 * The <code>FacesRequestLogger</code> is a {@link PhaseListener} which logs the Faces request detail as {@link Level#INFO}.
 * The log format is as below:
 * <pre>
 * method{url, user, action, params, messages, timer}
 * </pre>
 * Where:
 * <ul>
 * <li><code>method</code>: the HTTP request method, usually <code>GET</code> or <code>POST</code>.</li>
 * <li><code>url</code>: the request URI with query string.</li>
 * <li><code>user</code>: the user detail, composed of:<ul>
 *     <li><code>ip</code>: the user IP, as obtained by {@link Faces#getRemoteAddr()}.</li>
 *     <li><code>login</code>: the user login, as obtained by {@link Faces#getRemoteUser()}.</li>
 *     <li><code>session</code>: the session ID, as obtained by {@link Faces#getSessionId()}.</li>
 *     <li><code>viewState</code>: the server side Faces view state identifier, if any.</li></ul>
 * <li><code>action</code>: the action detail, composed of:<ul>
 *     <li><code>source</code>: the action source, as obtained by {@link Components#getCurrentActionSource()}.</li>
 *     <li><code>event</code>: the action event name, if any.</li>
 *     <li><code>methods</code>: the action methods, as obtained by {@link Components#getActionExpressionsAndListeners(UIComponent)}</li>
 *     <li><code>validationFailed</code>: whether Faces validation has failed.</li></ul></li>
 * <li><code>params</code>: the HTTP request parameters whereby any parameters whose name matches <code>jakarta.faces.*</code> are skipped,
 * and whose name ends with <code>password</code> or <code>token</code> are masked with value <code>********</code>.</li>
 * <li><code>messages</code>: all Faces messages added so far.</li>
 * <li><code>timer</code>: the duration of each phase measured in milliseconds, or -1 if the phase has been skipped, composed of:<ul>
 *     <li><code>0</code>: total time.</li>
 *     <li><code>1</code>: duration of {@link PhaseId#RESTORE_VIEW}.</li>
 *     <li><code>2</code>: duration of {@link PhaseId#APPLY_REQUEST_VALUES}.</li>
 *     <li><code>3</code>: duration of {@link PhaseId#PROCESS_VALIDATIONS}.</li>
 *     <li><code>4</code>: duration of {@link PhaseId#UPDATE_MODEL_VALUES}.</li>
 *     <li><code>5</code>: duration of {@link PhaseId#INVOKE_APPLICATION}.</li>
 *     <li><code>6</code>: duration of {@link PhaseId#RENDER_RESPONSE}.</li></ul></li></ul>
 *
 * <h2>Installation</h2>
 * <p>
 * Register it as <code>&lt;phase-listener&gt;</code> in <code>faces-config.xml</code>.
 * <pre>
 * &lt;lifecycle&gt;
 *     &lt;phase-listener&gt;org.omnifaces.eventlistener.FacesRequestLogger&lt;/phase-listener&gt;
 * &lt;/lifecycle&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 3.0
 */
public class FacesRequestLogger extends DefaultPhaseListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(FacesRequestLogger.class.getName());

	private static final Pattern PASSWORD_REQUEST_PARAMETER_PATTERN = Pattern.compile(".*(password|token)$", CASE_INSENSITIVE);

	/**
	 * Listen on any phase.
	 */
	public FacesRequestLogger() {
		super(PhaseId.ANY_PHASE);
	}

	/**
	 * Before any phase, start the timer.
	 */
	@Override
	public void beforePhase(PhaseEvent event) {
		FacesContext context = event.getFacesContext();
		getPhaseTimer(context).start(event.getPhaseId());
	}

	/**
	 * After any phase, stop the timer, and if the current phase is RENDER_RESPONSE, or the response is complete, then log the Faces request
	 * detail.
	 */
	@Override
	public void afterPhase(PhaseEvent event) {
		FacesContext context = event.getFacesContext();
		getPhaseTimer(context).stop(event.getPhaseId());

		if (!(event.getPhaseId() == RENDER_RESPONSE || context.getResponseComplete()) || !logger.isLoggable(INFO)) {
			return;
		}

		try {
			logger.log(INFO, () -> getRequest(context).getMethod() + "=" + getLogDetails(context));
		}
		catch (Exception e) {
			logger.log(SEVERE, "Logging failed", e);
		}
	}

	/**
	 * You can override this if you need more fine grained control over log details.
	 * @param context The involved faces context.
	 * @return Log details.
	 */
	protected Map<String, Object> getLogDetails(FacesContext context) {
		Map<String, Object> logDetails = new LinkedHashMap<>();
		logDetails.put("url", getRequestURIWithQueryString(context));
		logDetails.put("user", getUserDetails(context));
		logDetails.put("action", getActionDetails(context));
		logDetails.put("params", getRequestParameters(context));
		logDetails.put("messages", getFacesMessages(context));
		logDetails.put("timer", getPhaseTimer(context));
		return logDetails;
	}

	/**
	 * You can override this if you need more fine grained control over logging of user details.
	 * @param context The involved faces context.
	 * @return User details.
	 */
	protected Map<String, Object> getUserDetails(FacesContext context) {
		Map<String, Object> userDetails = new LinkedHashMap<>();
		userDetails.put("ip", getRemoteAddr(context));
		userDetails.put("login", getRemoteUser(context));
		userDetails.put("session", getSessionId(context));
		if (!context.getApplication().getStateManager().isSavingStateInClient(context)) {
			userDetails.put("viewState", getRequestParameter(context, VIEW_STATE_PARAM));
		}
		return userDetails;
	}

	/**
	 * You can override this if you need more fine grained control over logging of action details.
	 * @param context The involved faces context.
	 * @return Action details.
	 */
	protected Map<String, Object> getActionDetails(FacesContext context) {
		UIComponent actionSource = getCurrentActionSource();
		Map<String, Object> actionDetails = new LinkedHashMap<>();
		actionDetails.put("source", actionSource != null ? actionSource.getClientId(context) : null);
		actionDetails.put("event", coalesce(getRequestParameter(context, BEHAVIOR_EVENT_PARAM_NAME), getRequestParameter(context, OMNIFACES_EVENT_PARAM_NAME)));
		actionDetails.put("methods", getActionExpressionsAndListeners(actionSource));
		actionDetails.put("validationFailed", context.isValidationFailed());
		return actionDetails;
	}

	/**
	 * You can override this if you need more fine grained control over logging of request parameters.
	 * @param context The involved faces context.
	 * @return Request parameters.
	 */
	protected Map<String, String> getRequestParameters(FacesContext context) {
		Set<Entry<String, String[]>> params = getRequestParameterValuesMap(context).entrySet();
		Map<String, String> filteredParams = new TreeMap<>();

		for (Entry<String, String[]> entry : params) {
			String name = entry.getKey();

			if (name.startsWith("jakarta.faces.")) {
				continue; // Faces internal stuff is not interesting.
			}

			String[] values = entry.getValue();
			String value = (values != null && values.length == 1) ? values[0] : Arrays.toString(values);

			if (value != null && getPasswordRequestParameterPattern(context).matcher(name).matches()) {
				value = "********"; // Mask passwords and tokens.
			}

			filteredParams.put(name, value);
		}

		return filteredParams;
	}

	/**
	 * You can override this if you need to change the default pattern for password based request parameters which will be filtered in
	 * {@link #getRequestParameters(FacesContext)}.
	 * The default pattern matches every request parameter name ending with "password" or "token", case insensitive.
	 * @param context The involved faces context.
	 * @return Pattern for password request parameters.
	 */
	protected Pattern getPasswordRequestParameterPattern(FacesContext context) {
		return PASSWORD_REQUEST_PARAMETER_PATTERN;
	}

	/**
	 * You can override this if you need more fine grained control over logging of faces messages.
	 * @param context The involved faces context.
	 * @return Faces messages.
	 */
	protected Map<String, List<String>> getFacesMessages(FacesContext context) {
		Map<String, List<String>> facesMessages = new TreeMap<>();
		context.getMessages(null).forEachRemaining(collectGlobalMessageSummaries(facesMessages));
		context.getClientIdsWithMessages().forEachRemaining(collectMessageSummariesByClientId(context, facesMessages));
		return facesMessages;
	}

	private static Consumer<? super FacesMessage> collectGlobalMessageSummaries(Map<String, List<String>> facesMessages) {
		return message -> facesMessages.computeIfAbsent("", v -> new ArrayList<>()).add(message.getSummary());
	}

	private static Consumer<? super String> collectMessageSummariesByClientId(FacesContext context, Map<String, List<String>> facesMessages) {
		return clientId -> facesMessages.put(coalesce(clientId, ""), context.getMessageList(clientId).stream().map(FacesMessage::getSummary).collect(toList()));
	}

	private static PhaseTimer getPhaseTimer(FacesContext context) {
		return getRequestAttribute(context, PhaseTimer.class.getName(), PhaseTimer::new);
	}

	private static class PhaseTimer {

		private Map<Integer, Long> startTimes = new HashMap<>();
		private Map<Integer, Long> endTimes = new HashMap<>();

		public void start(PhaseId phaseId) {
			startTimes.putIfAbsent(phaseId.getOrdinal(), nanoTime());
		}

		public void stop(PhaseId phaseId) {
			endTimes.put(phaseId.getOrdinal(), nanoTime());
		}

		public String getDuration(PhaseId phase) {
			Long startTime = startTimes.get(phase == ANY_PHASE ? RESTORE_VIEW.getOrdinal() : phase.getOrdinal());
			Long endTime = endTimes.get(phase == ANY_PHASE ? Collections.max(endTimes.keySet()) : phase.getOrdinal());
			return (startTime != null && endTime != null ?  ((endTime - startTime) / 1_000_000) : -1) + "ms";
		}

		@Override
		public String toString() {
			Map<Integer, String> duration = new TreeMap<>();

			for (PhaseId phase : PhaseId.VALUES) {
				duration.put(phase.getOrdinal(), getDuration(phase));
			}

			return duration.toString();
		}
	}

}