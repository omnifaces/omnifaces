/*
 * Copyright 2015 OmniFaces.
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
import static javax.websocket.server.HandshakeRequest.SEC_WEBSOCKET_KEY;

import java.io.IOException;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.cdi.PushContext;
import org.omnifaces.filter.HttpFilter;

/**
 * <p>
 * This filter checks if the socket channel is registered and returns HTTP 400 error otherwise. In other words, only
 * channels explicitly declared in <code>&lt;o:socket channel&gt;</code> during the current HTTP session are allowed.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public class SocketChannelFilter extends HttpFilter {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The context-relative filter URL pattern where the web socket channel filter should listen on. */
	public static final String URL_PATTERN = PushContext.URI_PREFIX + "/*";

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException, IOException {
		if (session != null && request.getHeader(SEC_WEBSOCKET_KEY) != null) {
			Set<String> registeredChannels = (Set<String>) session.getAttribute(Socket.class.getName());

			if (registeredChannels != null) {
				String channel = request.getRequestURI().substring((request.getContextPath() + URL_PATTERN).length() - 1);

				if (registeredChannels.contains(channel)) {
					chain.doFilter(request, response);
					return;
				}
			}
		}

		response.sendError(SC_BAD_REQUEST);
	}

}