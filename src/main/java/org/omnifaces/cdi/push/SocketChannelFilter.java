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
package org.omnifaces.cdi.push;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.cdi.PushContext;
import org.omnifaces.filter.HttpFilter;

/**
 * <p>
 * This filter checks during an incoming web socket handshake request if the socket channel is registered and returns
 * HTTP 400 error otherwise. In other words, only channels explicitly declared in <code>&lt;o:socket channel&gt;</code>
 * during the current application's lifetime are allowed. If the server is configured to persist and revive sessions
 * during shutdown/restart, then the registered channels will be restored.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public class SocketChannelFilter extends HttpFilter {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The context-relative filter URL pattern where the socket channel filter should listen on. */
	public static final String URL_PATTERN = PushContext.URI_PREFIX + "/*";

	// Variables ------------------------------------------------------------------------------------------------------

	@Inject
	private SocketScopeManager scope;

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException, IOException {
		String channel = request.getRequestURI().substring((request.getContextPath() + URL_PATTERN).length() - 1);
		String scopeId = request.getQueryString();

		if (scope.isRegistered(channel, scopeId)) {
			chain.doFilter(request, response);
		}
		else {
			response.sendError(SC_BAD_REQUEST);
		}
	}

}