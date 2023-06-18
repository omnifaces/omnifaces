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
package org.omnifaces.test.util;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import jakarta.servlet.http.Part;

import org.junit.jupiter.api.Test;
import org.omnifaces.util.Servlets;

import com.google.common.collect.ImmutableMap;

public class TestServlets {

	private static final String[][] MULTIPART_CONTENT_DISPOSITIONS = {

		// https://datatracker.ietf.org/doc/html/rfc6266#section-5
		{ "form-data; filename=example.html", "example.html" },
		{ "form-data; FILENAME= \"an example.html\"", "an example.html" },
		{ "form-data; filename*= UTF-8''%e2%82%ac%20rates", "€ rates" },
		{ "form-data;filename=\"EURO rates\";filename*=utf-8''%e2%82%ac%20rates", "€ rates" },

		// http://test.greenbytes.de/tech/tc2231
		{ "form-data; filename=\"f\\oo.html\"", "foo.html" },
		{ "form-data; filename=\"\\\"quoting\\\" tested.html\"", "\"quoting\" tested.html" },
		{ "form-data; filename=\"Here's a semicolon;.html\"", "Here's a semicolon;.html" },
		{ "form-data; foo=\"bar\"; filename=\"foo.html\"", "foo.html" },
		{ "form-data; foo=\"\\\"\\\\\";filename=\"foo.html\"", "foo.html" },
		{ "form-data; FILENAME=\"foo.html\"", "foo.html" },
		{ "form-data; filename='foo.bar'", "'foo.bar'" },
		{ "form-data; filename=\"foo-ä.html\"", "foo-ä.html" },
		{ "form-data; filename=\"foo-Ã¤.html\"", "foo-Ã¤.html" },
		{ "form-data; filename=\"foo-%41.html\"", "foo-%41.html" },
		{ "form-data; filename=\"50%.html\"", "50%.html" },
		{ "form-data; filename=\"foo-%\\41.html\"", "foo-%41.html" },
		{ "form-data; filename=\"ä-%41.html\"", "ä-%41.html" },
		{ "form-data; filename=\"foo-%c3%a4-%e2%82%ac.html\"", "foo-%c3%a4-%e2%82%ac.html" },
		{ "form-data; filename =\"foo.html\"", "foo.html" },
		{ "form-data; filename=\"/foo.html\"", "foo.html" },
		{ "form-data; filename=\"\\\\foo.html\"", "foo.html" },
		{ "form-data; example=\"filename=example.txt\"; filename=\"foo.html\"", "foo.html" },
		{ "form-data; filename*=iso-8859-1''foo-%E4.html", "foo-ä.html" },
		{ "form-data; filename*=UTF-8''foo-%c3%a4-%e2%82%ac.html", "foo-ä-€.html" },
		{ "form-data; filename*=UTF-8''foo-a%cc%88.html", "foo-ä.html" },
		{ "form-data; filename*= UTF-8''foo-%c3%a4.html", "foo-ä.html" },
		{ "form-data; filename* =UTF-8''foo-%c3%a4.html", "foo-ä.html" },
		{ "form-data; filename*=UTF-8''A-%2541.html", "A-%41.html" },
		{ "form-data; filename*=UTF-8''%5cfoo.html", "foo.html" },
		{ "form-data; filename=\"foo-ae.html\"; filename*=UTF-8''foo-%c3%a4.html", "foo-ä.html" },
		{ "form-data; filename*=UTF-8''foo-%c3%a4.html; filename=\"foo-ae.html\"", "foo-ä.html" },
		{ "form-data; filename*0*=ISO-8859-15''euro-sign%3d%a4; filename*=ISO-8859-1''currency-sign%3d%a4", "currency-sign=¤" },
		{ "form-data; foobar=x; filename=\"foo.html\"", "foo.html" },
		{ "form-data; filename=C:\\fakepath\\foo.html", "foo.html" },
		{ "form-data; filename=\"c:\\fakepath\\foo.html\"", "foo.html" },
		{ "form-data; filename=c:/fakepath/foo.html", "foo.html" },
		{ "form-data; filename=\"C:/fakepath/foo.html\"", "foo.html" },
	};

	private static final String[][] DOWNLOAD_CONTENT_DISPOSITIONS = {
		{ "example.html", "attachment;filename=\"example.html\"; filename*=UTF-8''example.html" },
		{ "an example.html", "attachment;filename=\"an%20example.html\"; filename*=UTF-8''an%20example.html" },
		{ "€ rates", "attachment;filename=\"%E2%82%AC%20rates\"; filename*=UTF-8''%E2%82%AC%20rates" },
		{ "foo.html", "attachment;filename=\"foo.html\"; filename*=UTF-8''foo.html" },
		{ "\"quoting\" tested.html", "attachment;filename=\"%22quoting%22%20tested.html\"; filename*=UTF-8''%22quoting%22%20tested.html" },
		{ "Here's a semicolon;.html", "attachment;filename=\"Here%27s%20a%20semicolon%3B.html\"; filename*=UTF-8''Here%27s%20a%20semicolon%3B.html" },
		{ "'foo.bar'", "attachment;filename=\"%27foo.bar%27\"; filename*=UTF-8''%27foo.bar%27" },
		{ "foo-ä.html", "attachment;filename=\"foo-%C3%A4.html\"; filename*=UTF-8''foo-%C3%A4.html" },
		{ "foo-Ã¤.html", "attachment;filename=\"foo-%C3%83%C2%A4.html\"; filename*=UTF-8''foo-%C3%83%C2%A4.html" },
		{ "foo-%41.html", "attachment;filename=\"foo-%2541.html\"; filename*=UTF-8''foo-%2541.html" },
		{ "50%.html", "attachment;filename=\"50%25.html\"; filename*=UTF-8''50%25.html" },
		{ "foo-%41.html", "attachment;filename=\"foo-%2541.html\"; filename*=UTF-8''foo-%2541.html" },
		{ "ä-%41.html", "attachment;filename=\"%C3%A4-%2541.html\"; filename*=UTF-8''%C3%A4-%2541.html" },
		{ "foo-ä-€.html", "attachment;filename=\"foo-%C3%A4-%E2%82%AC.html\"; filename*=UTF-8''foo-%C3%A4-%E2%82%AC.html" },
		{ "A-%41.html", "attachment;filename=\"A-%2541.html\"; filename*=UTF-8''A-%2541.html" },
		{ "currency-sign=¤", "attachment;filename=\"currency-sign%3D%C2%A4\"; filename*=UTF-8''currency-sign%3D%C2%A4" },
	};

	@Test
	public void testGetSubmittedFileName() {
		for (String[] test : MULTIPART_CONTENT_DISPOSITIONS) {
			String header = test[0];
			String expectedFilename = test[1];
			String actualFilename = Servlets.getSubmittedFileName(new MockPartHeader(header));
			assertEquals(expectedFilename, actualFilename);
		}
	}

	@Test
	public void testFormatContentDisposition() {
		for (String[] test : DOWNLOAD_CONTENT_DISPOSITIONS) {
			String filename = test[0];
			String expectedHeader = test[1];
			String actualHeader = Servlets.formatContentDispositionHeader(filename, true);
			assertEquals(expectedHeader, actualHeader);
		}
	}

	private static class MockPartHeader implements Part {

		private String header;

		private MockPartHeader(String header) {
			this.header = header;
		}

		@Override
		public String getHeader(String name) {
			return header;
		}

		@Override
		public Collection<String> getHeaders(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<String> getHeaderNames() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getContentType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getSubmittedFileName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getSize() {
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(String fileName) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void delete() throws IOException {
			throw new UnsupportedOperationException();
		}

	}

	@Test
	public void testToParameterMap() {
		assertEquals(Collections.emptyMap(), Servlets.toParameterMap("="));
		assertEquals(ImmutableMap.of("foo", asList("%")), Servlets.toParameterMap("foo=%"));
		assertEquals(ImmutableMap.of("myParam", asList("123"), "anotherParam", asList("x")), Servlets.toParameterMap("myParam=123&=&anotherParam=x"));
	}

}
