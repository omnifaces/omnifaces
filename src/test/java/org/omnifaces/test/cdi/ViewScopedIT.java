package org.omnifaces.test.cdi;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@RunWith(Arquillian.class)
public class ViewScopedIT {

	@Drone
	private WebDriver browser;

	@ArquillianResource
	private URL contextPath;

	@FindBy(id="bean")
	private WebElement bean;

	@FindBy(id="messages")
	private WebElement messages;

	@FindBy(id="refresh")
	private WebElement refresh;

	@FindBy(id="non-ajax:submit")
	private WebElement nonAjaxSubmit;

	@FindBy(id="non-ajax:navigate")
	private WebElement nonAjaxNavigate;

	@FindBy(id="non-ajax:rebuild")
	private WebElement nonAjaxRebuild;

	@FindBy(id="ajax:submit")
	private WebElement ajaxSubmit;

	@FindBy(id="ajax:navigate")
	private WebElement ajaxNavigate;

	@FindBy(id="ajax:rebuild")
	private WebElement ajaxRebuild;

	@Deployment(testable = false)
	public static Archive<?> createDeployment() {
		return create(WebArchive.class, "ViewScopedIT.war")
			.addClass(ViewScopedITBean.class)
			.addAsWebResource("cdi/ViewScopedIT.xhtml")
			.addAsWebInfResource(INSTANCE, "beans.xml")
			.setWebXML("web.xml")
			.addAsLibrary(new File("target/" + System.getProperty("finalName") + ".jar"));
	}

	@Test
	public void test(String formId) {
		browser.get(contextPath + "ViewScopedIT.xhtml");
		assertEquals("init", messages.getText());
		String previousBean = bean.getText();

		guardHttp(refresh).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());

		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", messages.getText());

		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(nonAjaxRebuild).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("rebuild destroy init", messages.getText());

		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(refresh).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals(messages.getText(), "unload init");

		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", messages.getText());

		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardAjax(ajaxRebuild).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("rebuild destroy init", messages.getText());

		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(refresh).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());
	}

}