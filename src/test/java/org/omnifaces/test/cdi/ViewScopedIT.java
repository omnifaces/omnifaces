package org.omnifaces.test.cdi;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.omnifaces.test.OmniFacesIT.ArchiveBuilder.createWebArchive;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@RunWith(Arquillian.class)
public class ViewScopedIT extends OmniFacesIT {

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

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(ViewScopedIT.class);
	}

	@Test @InSequence(1)
	public void nonAjax() {
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

		guardHttp(refresh).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals(messages.getText(), "unload init");
	}

	@Test @InSequence(2)
	public void ajax() {
		assertEquals("unload init", messages.getText());
		String previousBean = bean.getText();

		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", messages.getText());

		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(refresh).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());
	}

}