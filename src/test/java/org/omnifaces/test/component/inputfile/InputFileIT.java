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
package org.omnifaces.test.component.inputfile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class InputFileIT extends OmniFacesIT {

	@FindBy(id="messages")
	private WebElement messages;

	@FindBy(id="uploadSingle:file")
	private WebElement uploadSingleFile;

	@FindBy(id="uploadSingle:submit")
	private WebElement uploadSingleSubmit;

	@FindBy(id="uploadSingleAjax:file")
	private WebElement uploadSingleAjaxFile;

	@FindBy(id="uploadSingleAjax:submit")
	private WebElement uploadSingleAjaxSubmit;

	@FindBy(id="uploadSingleAcceptAnyImage:file")
	private WebElement uploadSingleAcceptAnyImageFile;

	@FindBy(id="uploadSingleAcceptAnyImage:submit")
	private WebElement uploadSingleAcceptAnyImageSubmit;

	@FindBy(id="uploadSingleAcceptSvgImage:file")
	private WebElement uploadSingleAcceptSvgImageFile;

	@FindBy(id="uploadSingleAcceptSvgImage:submit")
	private WebElement uploadSingleAcceptSvgImageSubmit;

	@FindBy(id="uploadSingleMaxsizeClient:file")
	private WebElement uploadSingleMaxsizeClientFile;

	@FindBy(id="uploadSingleMaxsizeClient:message")
	private WebElement uploadSingleMaxsizeClientMessage;

	@FindBy(id="uploadSingleMaxsizeClient:submit")
	private WebElement uploadSingleMaxsizeClientSubmit;

	@FindBy(id="uploadSingleMaxsizeServer:file")
	private WebElement uploadSingleMaxsizeServerFile;

	@FindBy(id="uploadSingleMaxsizeServer:submit")
	private WebElement uploadSingleMaxsizeServerSubmit;

	@FindBy(id="uploadMultiple:file1")
	private WebElement uploadMultipleFile1;

	@FindBy(id="uploadMultiple:file2")
	private WebElement uploadMultipleFile2;

	@FindBy(id="uploadMultiple:submit")
	private WebElement uploadMultipleSubmit;

	@FindBy(id="uploadMultipleAjax:file1")
	private WebElement uploadMultipleAjaxFile1;

	@FindBy(id="uploadMultipleAjax:file2")
	private WebElement uploadMultipleAjaxFile2;

	@FindBy(id="uploadMultipleAjax:submit")
	private WebElement uploadMultipleAjaxSubmit;

	@FindBy(id="uploadMultipleMaxsizeClient:file1")
	private WebElement uploadMultipleMaxsizeClientFile1;

	@FindBy(id="uploadMultipleMaxsizeClient:file2")
	private WebElement uploadMultipleMaxsizeClientFile2;

	@FindBy(id="uploadMultipleMaxsizeClient:message")
	private WebElement uploadMultipleMaxsizeClientMessage;

	@FindBy(id="uploadMultipleMaxsizeClient:submit")
	private WebElement uploadMultipleMaxsizeClientSubmit;

	@FindBy(id="uploadMultipleMaxsizeServer:file1")
	private WebElement uploadMultipleMaxsizeServerFile1;

	@FindBy(id="uploadMultipleMaxsizeServer:file2")
	private WebElement uploadMultipleMaxsizeServerFile2;

	@FindBy(id="uploadMultipleMaxsizeServer:message")
	private WebElement uploadMultipleMaxsizeServerMessage;

	@FindBy(id="uploadMultipleMaxsizeServer:submit")
	private WebElement uploadMultipleMaxsizeServerSubmit;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(InputFileIT.class);
	}

	@Test
	public void uploadSingle() throws IOException {
		File txtFile = createTempFile("file", "txt", "hello world");
		uploadSingleFile.sendKeys(txtFile.getAbsolutePath());
		guardHttp(uploadSingleSubmit).click();
		assertTrue(uploadSingleFile.getText().isEmpty());
		assertEquals("uploadSingle: " + txtFile.length() + ", " + txtFile.getName(), getMessagesText());
	}

	@Test
	public void uploadSingleEmpty() {
		guardHttp(uploadSingleSubmit).click();
		assertEquals("uploadSingle: null", getMessagesText());
	}

	@Test
	public void uploadSingleAjax() throws IOException {
		File txtFile = createTempFile("file", "txt", "hello world");
		uploadSingleAjaxFile.sendKeys(txtFile.getAbsolutePath());
		guardAjaxUpload(uploadSingleAjaxSubmit, messages);
		assertEquals("uploadSingle: " + txtFile.length() + ", " + txtFile.getName(), getMessagesText());
	}

	@Test
	public void uploadSingleAcceptAnyImage() throws IOException {
		File txtFile = createTempFile("file", "txt", "hello world");
		uploadSingleAcceptAnyImageFile.sendKeys(txtFile.getAbsolutePath());
		guardHttp(uploadSingleAcceptAnyImageSubmit).click();
		assertTrue(uploadSingleAcceptAnyImageFile.getText().isEmpty());
		assertEquals("label: " + txtFile.getName() + " is not image/*", getMessagesText());

		File gifFile = createTempFile("file", "gif", "GIF89a");
		uploadSingleAcceptAnyImageFile.sendKeys(gifFile.getAbsolutePath());
		guardHttp(uploadSingleAcceptAnyImageSubmit).click();
		assertTrue(uploadSingleAcceptAnyImageFile.getText().isEmpty());
		assertEquals("uploadSingle: " + gifFile.length() + ", " + gifFile.getName(), getMessagesText());
	}

	@Test
	public void uploadSingleAcceptSvgImage() throws IOException {
		File txtFile = createTempFile("file", "txt", "hello world");
		uploadSingleAcceptSvgImageFile.sendKeys(txtFile.getAbsolutePath());
		guardHttp(uploadSingleAcceptSvgImageSubmit).click();
		assertTrue(uploadSingleAcceptSvgImageFile.getText().isEmpty());
		assertEquals("label: " + txtFile.getName() + " is not image/svg+xml", getMessagesText());

		File svgFile = createTempFile("file", "svg", "<svg/>");
		uploadSingleAcceptSvgImageFile.sendKeys(svgFile.getAbsolutePath());
		guardHttp(uploadSingleAcceptSvgImageSubmit).click();
		assertTrue(uploadSingleAcceptSvgImageFile.getText().isEmpty());
		assertEquals("uploadSingle: " + svgFile.length() + ", " + svgFile.getName(), getMessagesText());
	}

	@Test
	public void uploadSingleMaxsizeClient() throws IOException {
		File txtFile = createTempFile("file", "txt", "hello world");
		uploadSingleMaxsizeClientFile.sendKeys(txtFile.getAbsolutePath());
		triggerOnchange(uploadSingleMaxsizeClientFile, uploadSingleMaxsizeClientMessage); // TODO: this fails in phantomjs because element.files is null.
		assertTrue(uploadSingleMaxsizeClientFile.getText().isEmpty());
		String message = uploadSingleMaxsizeClientMessage.getText();
		assertTrue(message.startsWith("label: ") && message.endsWith(" larger than 10 B")); // Selenium JS engine doesn't correctly implement HTML5 File API as to obtaining file name.

		File gifFile = createTempFile("file", "gif", "GIF89a");
		uploadSingleMaxsizeClientFile.sendKeys(gifFile.getAbsolutePath());
		guardHttp(uploadSingleMaxsizeClientSubmit).click();
		assertTrue(uploadSingleMaxsizeClientFile.getText().isEmpty());
		assertEquals("uploadSingle: " + gifFile.length() + ", " + gifFile.getName(), uploadSingleMaxsizeClientMessage.getText());
	}

	@Test
	public void uploadSingleMaxsizeServer() throws IOException {
		File txtFile = createTempFile("file", "txt", "hello world");
		uploadSingleMaxsizeServerFile.sendKeys(txtFile.getAbsolutePath());
		guardHttp(uploadSingleMaxsizeServerSubmit).click();
		assertTrue(uploadSingleMaxsizeServerFile.getText().isEmpty());
		assertEquals("label: " + txtFile.getName() + " larger than 10 B", getMessagesText());

		File gifFile = createTempFile("file", "gif", "GIF89a");
		uploadSingleMaxsizeServerFile.sendKeys(gifFile.getAbsolutePath());
		guardHttp(uploadSingleMaxsizeServerSubmit).click();
		assertTrue(uploadSingleMaxsizeServerFile.getText().isEmpty());
		assertEquals("uploadSingle: " + gifFile.length() + ", " + gifFile.getName(), getMessagesText());
	}

	@Test
	public void uploadMultiple() throws IOException {
		File txtFile1 = createTempFile("file1", "txt", "hello");
		File txtFile2 = createTempFile("file2", "txt", "world");
		uploadMultipleFile1.sendKeys(txtFile1.getAbsolutePath());
		uploadMultipleFile2.sendKeys(txtFile2.getAbsolutePath()); // TODO: this hangs when phantomjs is used.
		guardHttp(uploadMultipleSubmit).click();
		assertTrue(uploadMultipleFile1.getText().isEmpty());
		assertEquals("uploadMultiple: " + txtFile1.length() + ", " + txtFile1.getName() + " uploadMultiple: " + txtFile2.length() + ", " + txtFile2.getName(), getMessagesText());
	}

	@Test
	public void uploadMultipleAjax() throws IOException {
		File txtFile1 = createTempFile("file1", "txt", "hello");
		File txtFile2 = createTempFile("file2", "txt", "world");
		uploadMultipleAjaxFile1.sendKeys(txtFile1.getAbsolutePath());
		uploadMultipleAjaxFile2.sendKeys(txtFile2.getAbsolutePath()); // TODO: this hangs when phantomjs is used.
		guardAjaxUpload(uploadMultipleAjaxSubmit, messages);
		assertEquals("uploadMultiple: " + txtFile1.length() + ", " + txtFile1.getName() + " uploadMultiple: " + txtFile2.length() + ", " + txtFile2.getName(), getMessagesText());
	}

	@Test
	public void uploadMultipleMaxsizeClient() throws IOException {
		File txtFile1 = createTempFile("file1", "txt", "hello hello");
		File txtFile2 = createTempFile("file2", "txt", "world");
		uploadMultipleMaxsizeClientFile1.sendKeys(txtFile1.getAbsolutePath());
		uploadMultipleMaxsizeClientFile2.sendKeys(txtFile2.getAbsolutePath()); // TODO: this hangs when phantomjs is used.
		triggerOnchange(uploadMultipleMaxsizeClientFile1, uploadMultipleMaxsizeClientMessage);
		assertTrue(uploadMultipleMaxsizeClientFile1.getText().isEmpty());
		String message = uploadMultipleMaxsizeClientMessage.getText();
		assertTrue(message.startsWith("label: ") && message.endsWith(" larger than 10 B")); // Selenium JS engine doesn't correctly implement HTML5 File API as to obtaining file name so we cannot assert file name but only custom label.

		txtFile1 = createTempFile("file1", "txt", "hello");
		uploadMultipleMaxsizeClientFile1.sendKeys(txtFile1.getAbsolutePath());
		guardHttp(uploadMultipleMaxsizeClientSubmit).click();
		assertTrue(uploadMultipleMaxsizeClientFile1.getText().isEmpty());
		assertTrue(uploadMultipleMaxsizeClientFile2.getText().isEmpty());
		assertEquals("uploadMultiple: " + txtFile1.length() + ", " + txtFile1.getName() + " uploadMultiple: " + txtFile2.length() + ", " + txtFile2.getName(), getMessagesText());
	}

	@Test
	public void uploadMultipleMaxsizeServer() throws IOException {
		File txtFile1 = createTempFile("file1", "txt", "hello");
		File txtFile2 = createTempFile("file2", "txt", "world world");
		uploadMultipleMaxsizeServerFile1.sendKeys(txtFile1.getAbsolutePath());
		uploadMultipleMaxsizeServerFile2.sendKeys(txtFile2.getAbsolutePath()); // TODO: this hangs when phantomjs is used.
		guardHttp(uploadMultipleMaxsizeServerSubmit).click();
		assertTrue(uploadMultipleMaxsizeServerFile1.getText().isEmpty());
		assertTrue(uploadMultipleMaxsizeServerFile2.getText().isEmpty());
		assertEquals("label: " + txtFile2.getName() + " larger than 10 B", getMessagesText());

		txtFile2 = createTempFile("file2", "txt", "world");
		uploadMultipleMaxsizeServerFile1.sendKeys(txtFile1.getAbsolutePath());
		uploadMultipleMaxsizeServerFile2.sendKeys(txtFile2.getAbsolutePath());
		guardHttp(uploadMultipleMaxsizeServerSubmit).click();
		assertTrue(uploadMultipleMaxsizeServerFile1.getText().isEmpty());
		assertTrue(uploadMultipleMaxsizeServerFile2.getText().isEmpty());
		assertEquals("uploadMultiple: " + txtFile1.length() + ", " + txtFile1.getName() + " uploadMultiple: " + txtFile2.length() + ", " + txtFile2.getName(), getMessagesText());
	}

	private File createTempFile(String name, String extension, String content) throws IOException {
		Path path = Files.createTempFile(name + "-", "." + extension);
		Files.write(path, content.getBytes(UTF_8));
		File file = path.toFile();
		file.deleteOnExit();
		return file;
	}

	private String getMessagesText() {
		return messages.getText().replaceAll("\\s+", " ");
	}

}