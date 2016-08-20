package org.omnifaces.test.exceptionhandler;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.junit.Assert.assertTrue;
import static org.omnifaces.test.OmniFacesIT.ArchiveBuilder.buildWebArchive;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withFullAjaxExceptionHandler;
import static org.omnifaces.test.OmniFacesIT.WebXml.withErrorPage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@RunWith(Arquillian.class)
public class FullAjaxExceptionHandlerIT extends OmniFacesIT {

	@FindBy(id="exception")
	private WebElement exception;

	@FindBy(id="form:throwDuringInvokeApplication")
	private WebElement throwDuringInvokeApplication;

	@FindBy(id="form:throwDuringUpdateModelValues")
	private WebElement throwDuringUpdateModelValues;

	@FindBy(id="form:throwDuringRenderResponse")
	private WebElement throwDuringRenderResponse;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(FullAjaxExceptionHandlerIT.class)
			.withFacesConfig(withFullAjaxExceptionHandler)
			.withWebXml(withErrorPage)
			.createDeployment();
	}

	@Test
	public void throwDuringInvokeApplication() {
		guardAjax(throwDuringInvokeApplication).click();
		assertTrue(exception.getText().contains("throwDuringInvokeApplication"));
	}

	@Test
	public void throwDuringUpdateModelValues() {
		guardAjax(throwDuringUpdateModelValues).click();
		assertTrue(exception.getText().contains("throwDuringUpdateModelValues"));
	}

	@Test
	public void throwDuringRenderResponse() {
		guardAjax(throwDuringRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
	}

}