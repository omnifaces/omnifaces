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
package org.omnifaces.test.cdi.viewscoped.unloadduringaction;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * An unload request must not destroy the view scoped beans which a concurrent request of the same view is still using.
 * They must be destroyed once that request has finished. The unload of the browser itself is disabled, so that only the
 * unload request sent by the test can destroy the bean.
 */
public class ViewScopedUnloadDuringActionIT extends OmniFacesIT {

	private static final Pattern BEAN = Pattern.compile("<div id=\"bean\">([^<]*)</div>");
	private static final Pattern DESTROYED_DURING_ACTION = Pattern.compile("<div id=\"destroyedDuringAction\">([^<]*)</div>");

	/** The hidden form fields carry the view state and the implementation specific marker of the submitted form. */
	private static final String SCRIPT_SERIALIZE_HIDDEN_FORM_FIELDS = "return Array.prototype.map.call(document.querySelectorAll('#form input[type=hidden]'),"
		+ " function(input) { return encodeURIComponent(input.name) + '=' + encodeURIComponent(input.value); }).join('&');";

	@FindBy(id="bean")
	private WebElement bean;

	@FindBy(id="form")
	private WebElement form;

	@FindBy(css="#form > [name='javax.faces.ViewState']")
	private WebElement viewState;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(ViewScopedUnloadDuringActionIT.class);
	}

	@Test
	public void unloadDuringActionMustDestroyBeanOnlyAfterAction() throws Exception {
		unloadDuringAction(false);
	}

	/**
	 * Creating a new view after the unload performs the pending view state removal of the unloaded view, which may
	 * destroy the view map of the unloaded view. The action must nonetheless keep resolving its own bean.
	 */
	@Test
	public void unloadAndCreateNewViewDuringActionMustDestroyBeanOnlyAfterAction() throws Exception {
		unloadDuringAction(true);
	}

	private void unloadDuringAction(boolean createNewView) throws Exception {
		getProbe("reset");
		executeScript("OmniFaces.Unload.disable()");
		String beanId = bean.getText();
		String cookies = getCookies();
		String action = form.getAttribute("action");
		String formParams = executeScript(SCRIPT_SERIALIZE_HIDDEN_FORM_FIELDS);
		String viewStateParam = "&javax.faces.ViewState=" + URLEncoder.encode(viewState.getAttribute("value"), UTF_8.name());
		ExecutorService executor = newSingleThreadExecutor();

		try {
			Future<String> actionResponse = executor.submit(() -> readResponseBody(post(action, formParams + "&form%3Aaction=action", cookies)));
			assertEquals("true", getProbe("awaitActionStarted"), () -> "action started, action response: " + getIfDone(actionResponse));

			HttpURLConnection unload = post(action, "omnifaces.event=unload&id=" + getViewScopeId() + viewStateParam, cookies);
			assertEquals(HTTP_NO_CONTENT, unload.getResponseCode(), "unload response status");

			if (createNewView) {
				HttpURLConnection newView = openConnection(action, cookies);
				assertEquals(HTTP_OK, newView.getResponseCode(), "new view response status");
			}

			getProbe("releaseAction");
			String actionResponseBody = actionResponse.get();
			assertEquals(beanId, find(BEAN, actionResponseBody), "bean rendered after action");
			assertEquals("false", find(DESTROYED_DURING_ACTION, actionResponseBody), "bean destroyed during action");
			assertEquals("true", getProbe("awaitBeanDestroyed"), "bean destroyed after action");
		}
		finally {
			releaseActionQuietly();
			executor.shutdownNow();
		}
	}

	/**
	 * Releases the action if it is still blocked. Any failure here is ignored, so that it does not mask the actual test
	 * failure.
	 */
	private void releaseActionQuietly() {
		try {
			getProbe("releaseAction");
		}
		catch (IOException ignore) {
			// NOOP.
		}
	}

	private static String getIfDone(Future<String> response) {
		try {
			return response.isDone() ? response.get() : "(still pending)";
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return e.toString();
		}
		catch (ExecutionException e) {
			return e.getCause().toString();
		}
	}

	private static String find(Pattern pattern, String responseBody) {
		Matcher matcher = pattern.matcher(responseBody);
		assertTrue(matcher.find(), () -> "action response is rendered: " + responseBody);
		return matcher.group(1);
	}

	private String getProbe(String command) throws IOException {
		return readResponseBody(openConnection("probe/" + command, null));
	}

	private HttpURLConnection post(String action, String body, String cookies) throws IOException {
		HttpURLConnection connection = openConnection(action, cookies);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setDoOutput(true);

		try (OutputStream output = connection.getOutputStream()) {
			output.write(body.getBytes(UTF_8));
		}

		return connection;
	}

}
