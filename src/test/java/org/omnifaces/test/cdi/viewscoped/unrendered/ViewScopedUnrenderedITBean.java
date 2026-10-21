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
package org.omnifaces.test.cdi.viewscoped.unrendered;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.omnifaces.util.Faces.sendFile;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

/**
 * Is rendered by its view, so that it must stay.
 */
@Named
@ViewScoped
public class ViewScopedUnrenderedITBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private ViewScopedUnrenderedITProbe probe;

	private final String id = UUID.randomUUID().toString();
	private int count;

	@PostConstruct
	public void init() {
		probe.create("main");
	}

	@PreDestroy
	public void destroy() {
		probe.destroy("main");
	}

	public void increment() {
		count++;
	}

	public void download() throws IOException {
		sendFile("download".getBytes(UTF_8), "download.txt", true);
	}

	public String getId() {
		return id;
	}

	public int getCount() {
		return count;
	}

}
