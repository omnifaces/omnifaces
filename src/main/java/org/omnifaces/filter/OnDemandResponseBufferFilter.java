/*
 * Copyright 2012 OmniFaces.
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
package org.omnifaces.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.component.output.Cache;
import org.omnifaces.servlet.BufferedHttpServletResponse;

/**
 * Filter that wraps the response with one capable of buffering on command.
 * <p>
 * The response wrapper is additionally make available as a request attribute, so it's always obtainable even if a following filter
 * wraps the response again. By default the response wrapper is in "pass-through" mode, meaning it will do no buffering. Anywhere during the
 * request it can be switched to buffer by setting its <code>passThrough</code> argument to false.
 * <p>
 * The next call to obtain the response's writer will then provide one that buffers. Do note that any existing writer that has been obtained
 * before <code>passThrough</code> was set to false and is used afterwards, will <b>not</b> automatically start buffering. Only newly obtained
 * writers will buffer from that point on.
 * <p>
 * If at the end of the request, when this filter resumes control again, the response is still buffering (<code>passThrough</code> is false)
 * its buffer will be automatically flushed to the underlying response. If however the buffer is not empty, but <code>passThrough</code> is true,
 * no such flushing will be done and it's assumed the application has taken care of this.
 *
 * @author Arjan Tijms
 * @since 1.2
 * @see Cache
 */
public class OnDemandResponseBufferFilter extends HttpFilter {

	public static final String BUFFERED_RESPONSE = "org.omnifaces.servlet.BUFFERED_RESPONSE";

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException,
			IOException {

		BufferedHttpServletResponse bufferedResponse = new BufferedHttpServletResponse(response);

		// By default don't buffer, code has to activate this explicitly.
		bufferedResponse.setPassThrough(true);

		request.setAttribute(BUFFERED_RESPONSE, bufferedResponse);

		try {
			chain.doFilter(request, bufferedResponse);
		} finally {
			if (!bufferedResponse.isPassThrough()) {
				// TODO: output stream support
				response.getWriter().write(bufferedResponse.getBufferAsString());
			}
		}

	}

}
