/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.test.component.inputfile;

import static org.omnifaces.util.Messages.addGlobalInfo;
import static org.omnifaces.util.Servlets.getSubmittedFileName;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;

@Named
@RequestScoped
public class InputFileITBean {

	private Part file;
	private List<Part> files;

	public void uploadSingle() {
		addGlobalInfo("uploadSingle: " + (file == null ? "null" : (file.getSize() + ", " + getSubmittedFileName(file))));
	}

	public void uploadMultiple() {
		for (Part file : files) {
			addGlobalInfo(" uploadMultiple: " + file.getSize() + ", " + getSubmittedFileName(file));
		}
	}

	public Part getFile() {
		return file;
	}

	public void setFile(Part file) {
		this.file = file;
	}

	public List<Part> getFiles() {
		return files;
	}

	public void setFiles(List<Part> files) {
		this.files = files;
	}

}