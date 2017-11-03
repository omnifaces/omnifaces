/*
 * Copyright 2017 OmniFaces
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
package org.omnifaces.component.output;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.el.ValueExpression;
//import javax.faces.FacesException;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.FacesMessage;
import javax.faces.application.NavigationCase;
import javax.faces.application.ProjectStage;
import javax.faces.application.ViewHandler;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlOutcomeTargetButton;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionListener;
import javax.faces.event.PreRenderComponentEvent;
import javax.faces.flow.FlowHandler;
import javax.faces.lifecycle.ClientWindow;
import javax.servlet.ServletContext;

import static org.omnifaces.util.Renderers.writeIdAttributeIfNecessary;
import static org.omnifaces.util.Servlets.getApplicationAttribute;
import static org.omnifaces.util.Utils.encodeURI;
import static org.omnifaces.util.Utils.encodeURL;

/**
 * The <code>&lt;o:button&gt;</code> is a component that extends the standard <code>&lt;h:button&gt;</code> component to support 
 * <code>MultiViews</code> feature of {@link org.omnifaces.facesviews.FacesViews} by rendering the supplied <code>&lt;o:pathParam&gt;</code>
 * parameters as path parameters and not as query parameters as otherwise will be produced by standard <code>&lt;o:param&gt;</code> and 
 * <code>&lt;f:param&gt;</code>, or if <code>&lt;o:pathParam&gt;</code> tags are nested in a standard <code>&lt;h:button&gt;</code>.
 * <p>
 * To achieve this goal <code>&lt;o:pathParam&gt;</code> tags must be specified as children of <code>&lt;o:button&gt;</code>. See {@link PathParam}
 * for an overview of configuration of path parameters. Any <code>&lt;o:pathParam&gt;</code> tag will be rendered as a part of the URL path of 
 * this component, like <i>/path-parameter-evaluated-value</i>, if the following conditions are met:
 * <ol>
 * <li>
 * <code>outcome</code> attribute of the current <code>&lt;o:button&gt;</code> component specifies a valid MultiView page, 
 * see {@link org.omnifaces.facesviews.FacesViews} for a proper way of configuring <code>MultiViews</code> feature.
 * </li>
 * <li>
 * <code>basic</code> attribute of the current component must evaluate to false (or left as default).
 * </li>
 * <li>
 * <code>basic</code> attribute of <code>&lt;o:pathParam&gt;</code> tag must evaluate to false (or left as default).
 * </li>
 * </ol>
 * <p>
 * The ordering of path parameters will be defined according to the <code>index</code> attribute of <code>&lt;o:pathParam&gt;</code> tags. 
 * In case of collisions the tags with equal indices, either specified and unspecified, will be rendered in the order they were defined. 
 * The location of paths with unspecified indices will be derived from <code>location</code> attribute of this component, see description below. 
 * Location and indices only specify relative context of rendering, or before-after relation. Negative indices are also supported.
 * <p>
 * This component also accepts standard parameters, <code>&lt;o:param&gt;</code> and <code>&lt;f:param&gt;</code>, and will render
 * them as query parameters, as it has always traditionally been done by {@link UIOutcomeTarget} components like <code>&lt;h:link&gt;</code> and 
 * <code>&lt;h:button&gt;</code>. <code>&lt;o:pathParam&gt;</code> tags can also be rendered as query parameters if their <code>basic</code>
 * attributes evaluate to true. It can be helpful if an application has to decide upon way of rendering at runtime, by checking 
 * <code>#{someCondition}</code>.
 * <p>
 * The component specifies three additional non-required attributes to control and fine-tune the generated path from path parameters:
 * <ul>
 * <li>
 * <code>location</code>, non-required attribute that defines the location where <code>&lt;o:pathParam&gt;</code> tags with non-specified 
 * <code>index</code> attributes relative to the ones with the specified <code>index</code> attribute will be rendered: after all path 
 * parameters for attribute value "end", before all path parameters for attribute value "beginning" or at a specified location for attribute 
 * integer value, i.e. for components where <code>Integer.valueOf(getLocation())</code> doesn't throw <code>NumberFormatException</code>. 
 * Default value of this attribute is "end". All other values will be treated as "end" as well.
 * </li>
 * <li>
 * <code>force</code>, non-required attribute that defines behaviour when <code>outcome</code> attribute doesn't specify a valid MultiView page:
 * if the attribute value is true then if any <code>&lt;o:pathParam&gt;</code> with <code>basic</code> attribute evaluating to false are present 
 * the component will be rendered as disabled and generate a {@link FacesMessage} that the outcome is not a valid MultiView page, if the 
 * attribute value is false, any <code>&lt;o:pathParam&gt;</code> tag will be treated as simple <code>&lt;o:param&gt;</code> thus producing 
 * query parameters for all parameters with a specified non-empty name. Default value is true.
 * </li>
 * <li>
 * <code>basic</code>, non-required attribute that is used to turn off dealing with <code>&lt;o:pathParam&gt;</code> tags as path parameters.
 * If this property evaluates to true, all present <code>&lt;o:pathParam&gt;</code> tags will be treated as <code>&lt;o:param&gt;</code> tags,
 * thus overriding all preset behaviour of the component and its children. If this property evaluates to false, the rendering will proceed
 * in a standard fashion, described above. This attribute is helpful when an application needs to turn off defined behaviour basing on
 * some condition. Default value is false.
 * </li>
 * </ul>
 * <p>
 * There are four additional properties to the ones defined in <code>&lt;h:button&gt;</code> to account for extra features: 
 * <ul>
 * <li>
 * <code>disabled</code>, non-required attribute that serves as a flag to disable the button. Default value is false.
 * </li>
 * <li>
 * <code>target</code>, non-required attribute that defines the name of a frame where the resource of this component will be displayed. Default value is "_self".
 * </li>
 * <li>
 * <code>fragment</code>, non-required attribute that attaches hash-separated fragment at the end of the URL, if present and not empty.
 * </li>
 * <li>
 * <code>escape</code>, non-required attribute that defines whether or not to escape the value of this component. Default value is true.
 * Beware of potential XSS attack holes when redisplaying user-controlled input with <code>escape="false"</code>.
 * </li>
 * </ul>
 * <p>
 * This component is useful when application must provide links to the MultiView-configured pages with parameters rendered as part of the 
 * path and not a part of the query string, as it is traditionally done. The sending page defines a <code>&lt;o:button&gt;</code> with 
 * <code>&lt;o:pathParam&gt;</code>s, thus generating URL with path parameters, and the receiving page consumes these path parameters in 
 * CDI beans by means of {@link org.omnifaces.cdi.Param} with a specified <code>pathIndex</code> property.
 * <p>
 * There are some options, described above, for tuning the generated URL of this component in various circumstances. This component 
 * can also be used to generate standard buttons for non-MultiView pages as well as buttons for MultiView pages with standard appearance.
 * <p>
 * The component provides a set of predefined CSS classes to support defining styles for all used components of this type in ones place. Additionally,
 * user-defined CSS classes can be added via the traditional <code>styleClass</code> attribute for the generated <code>button</code> HTML element and 
 * via the <code>image</code> attribute for the nested <code>span</code> HTML element, where the image has to be referenced via the CSS 
 * <code>background-image</code> property. In case the <code>image</code> attribute is absent, no image span will be rendered. Also note that it 
 * is the application responsibility to create right CSS classes for proper appearance of the button. Text of the button will be rendered in the 
 * <code>span</code> HTML element, next to the image element, if there is one. If no <code>image</code> attribute is specified, the text 
 * element will be rendered in any case, either with the supplied component value, or with the default text, if no <code>value</code> attribute was 
 * declared. The value of the component will be rendered as the button text. No declared children will be rendered to HTML output.
 * These classes are:
 * <ul>
 * <li>"of-button" for all generated active <code>button</code> HTML elements and "of-button-disabled" for all disabled ones.</li>
 * <li>"of-button-image" for all generated <code>span</code> HTML elements for referencing the image of the button.</li>
 * <li>"of-button-text" for all generated <code>span</code> HTML elements for holding the text of the button.</li>
 * </ul>
 * <p>
 * Basic usage examples follow. They assume that {@link org.omnifaces.facesviews.FacesViews} is turned on and <code>MultiViews</code>
 * configuration is present for paths "/users/*", "/path/*" and "/blog/*":
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.FACES_VIEWS_SCAN_PATHS&lt;/param-name&gt;
 *     &lt;param-value&gt;/*.xhtml, /user/*, /path/*, /blog/*&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * For additional information on how to configure FacesViews and use the MutiViews feature see {@link org.omnifaces.facesviews.FacesViews}.
 * <p>
 * In the following example the component generates a button to a view user detail page by employing implicit <code>@FacesConverter(forClass)</code>
 * converter.
 * <pre>
 * &lt;o:button value="User details" outcome="user"&gt;
 *     &lt;o:pathParam&gt; value="#{bean.user}" /&gt;
 * &lt;/o:button&gt;
 * </pre>
 * with
 * <pre>
 *{@literal @}FacesConverter(forClass = User.class) public class UserConverter implements Converter {
 *     public Object getAsObject(FacesContext context, UIComponent component, String value) {
 *         if (value == null) { return null; } return new User(value);
 *     }
 *     public String getAsString(FacesContext context, UIComponent component, Object value) {
 *         if(value == null) { return ""; }
 *         if(value instanceof User) { User user = (User)value; return user.getUsername(); }
 *         throw new ConverterException(value + " not a User");
 *     }
 * }
 *{@literal @}Named @RequestScoped public class Bean { private User user; ... }
 * public class User { private String username; ... }
 * </pre>
 * The generated URL referenced by the button could look like <code>/context-path/user/username</code>.
 * <p>
 * The following button shows a way of tuning the rendering of path parameters.
 * <pre>
 * &lt;o:button value="Button" outcome="path" location="beginning"&gt;
 *     &lt;o:pathParam&gt; value="first" index="1"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; value="no-index"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; name="basic" value="basic" basic="true"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; value="third" index="3"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; value="no-index-second" disable="#{someCondition}"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; value="#{bean.name}" converter="#{bean}" index="2"&lt;/o:pathParam&gt;
 * &lt;/o:button&gt;
 * </pre>
 * The generated URL referenced by the button could look like <code>/context-path/path/no-index/no-index-second/first/bean-name/third?basic=basic</code>.
 * <p>
 * Note that if the outcome of the button above pointed at a standard view id, say, <code>index</code> then the construct will
 * be rendered as a disabled button (impossible to render path parameters at a standard outcome). Still, configuring
 * &lt;o:button ... force="false" /&gt; will render an active button with all of the parameter rendered as query parameters (for 
 * parameters with non-empty name). Also note that standard parameters are perfectly legal:
 * <pre>
 * &lt;o:button ...&gt;
 *     &lt;o:pathParam&gt; ...&lt;/o:pathParam&gt;
 *     &lt;o:param&gt; name="omniparam" value="omniparam"&lt;/o:param&gt;
 *     &lt;f:param&gt; name="fparam" value="fparam"&lt;/f:param&gt;
 * &lt;/o:button&gt;
 * </pre>
 * Another option to turn off rendering of path parameters altogether is to set a condition for the <code>basic</code> attribute:
 * <pre>
 * &lt;o:button value="Button" outcome="path" basic="#{someCondition}"&gt;
 *     &lt;o:pathParam&gt; name="first" value="first"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; name="second" value="second"&lt;/o:pathParam&gt;
 * &lt;/o:button&gt;
 * </pre>
 * If <code>#{someCondition}</code> evaluates to false, the URL referenced by the button will be rendered as <code>/context-path/path/first/second</code>,
 * and if it evaluates to true, the URL referenced by the button will be rendered as <code>/context-path/path?first=first&second=second</code>.
 * <p>
 * The significant benefit of using the <code>&lt;o:button&gt;</code> component is its ability to integrate with JSF infrastructure.
 * The <code>outcome</code> attribute will be internally checked by a {@link NavigationHandler} and {@link ViewHandler} 
 * and in case of negative result the component will be rendered as disabled and a message will be printed. On the one hand this 
 * will reduce the number of hand-written errors and on the other hand will incorporate all user-created artifacts, like custom
 * <code>ViewHandlerWrapper</code> classes with overridden <code>getActionURL(...)</code> methods to reflect and generate valid 
 * and up-to-date URLs for output components.
 * <p>
 * The following component, for example, will be rendered as disabled: <code>&lt;o:button outcome="non-existing-path" .../&gt;</code>.
 * <p>
 * The following component will render a button with a prefix, if <code>getActionURL(...)</code> were overridden accordingly to prefix 
 * all paths of a special subset of views to "/special": <code>&lt;o:button outcome="special-path-for-view-handler" .../&gt;</code>. 
 * The generated URL could look like <code>/context-path/special/special-path-for-view-handler/...</code>.
 * <p>
 * The last example renders URLs basing on available information. For example, we can have a mapping on "/blog" with year, month and 
 * day specified. The following button could be used to point at a valid blog post overview:
 * <pre>
 * &lt;o:button value="Blog" outcome="blog"&gt;
 *     &lt;o:pathParam&gt; name="year" value="#{userSettings.year}"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; name="month" value="#{userSettings.month}"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; name="day" value="#{userSettings.day}"&lt;/o:pathParam&gt;
 * &lt;/o:button&gt;
 * </pre>
 * Of course, some additional logic must be incorporated to check parameters, but the construct above could be used to render buttons 
 * with referenced URLs of type <code>/context-path/blog/2017</code>, <code>/context-path/blog/2017/october</code> and 
 * <code>/context-path/blog/2017/october/1</code>, which could be rather fruitful under some circumstances.
 * 
 * @author Sergey Kuntsel
 * @since 3.0
 * @see Link
 * @see PathParam
 */
@FacesComponent(Button.COMPONENT_TYPE)
public class Button extends HtmlOutcomeTargetButton {
    //TODO refactor in favour of a Renderer or create a Utility class to solve common tasks.

    // Constants
    public static final String COMPONENT_TYPE = "org.omnifaces.component.output.Button";
    public static final String COMPONENT_FAMILY = "org.omnifaces.component.output";

    // TODO externalize messages and add i18n support
    private static final String ERROR_NAVIGATION_CASE_NOT_FOUND =
            "Navigation case couldn't be resolved for the outcome: %s.";
    private static final String ERROR_MULTIVIEW_NOT_CONFIGURED =
            "MultiView was not configured for the outcome: %s, but PathParams were defined for it.";
    private static final String NO_NAVIGATION_CASE = Button.class.getName() + "_NO_NAVIGATION_CASE";
    private static final String NO_MULTIVIEW_CONFIG = Button.class.getName() + "_NO_MULTIVIEW_CONFIG";

    private static final Logger logger = Logger.getLogger(Button.class.getName());
    protected static final Map<String, String> ATTRIBUTE_NAMES = getButtonAttributes();

    // Constructor
    public Button() {
        setRendererType(null);
    }

    // Properties
    private enum PropertyKeys {
        disabled, // flag for disabling button, default false
        target, // frame where the outcome will be displayed, default value "_self"
        fragment, // attach hash-separated fragment at the end of the URL, default null
        escape, // escape content if explicitly needed, default true
        location, // location to render path params with unspecified index: at the beginning ("beginning"), at the end ("end") or at the specified index (any Integer value)
                  //default value "end", any other string and error while converting to Integer equals to "end"
        force, // if true: when PathParams defined for non-applicable view generate error message, if false: treat PathParams as UIParameters, default true
        basic; // if true: all PathParams will be treated as UIParameters, thus effectively disabling path parameter behaviour basing on some condition, if false: standard behaviour, default false
    }

    public Boolean getDisabled() { return (Boolean)getStateHelper().eval(PropertyKeys.disabled, Boolean.FALSE); }
    public void setDisabled(Boolean disabled) { getStateHelper().put(PropertyKeys.disabled, disabled); }
    public String getTarget() { return (String)getStateHelper().eval(PropertyKeys.target, "_self"); }
    public void setTarget(String target) { getStateHelper().put(PropertyKeys.target, target); }
    public String getFragment() { return (String)getStateHelper().eval(PropertyKeys.fragment); }
    public void setFragment(String fragment) { getStateHelper().put(PropertyKeys.fragment, fragment); }
    public Boolean getEscape() { return (Boolean)getStateHelper().eval(PropertyKeys.escape, Boolean.TRUE); }
    public void setEscape(Boolean escape) { getStateHelper().put(PropertyKeys.escape, escape); }
    public String getLocation() { return (String)getStateHelper().eval(PropertyKeys.location, "end"); }
    public void setLocation(String location) { getStateHelper().put(PropertyKeys.location, location); }
    public Boolean getForce() { return (Boolean)getStateHelper().eval(PropertyKeys.force, Boolean.TRUE); }
    public void setForce(Boolean escape) { getStateHelper().put(PropertyKeys.force, escape); }
    public Boolean getBasic() { return (Boolean)getStateHelper().eval(PropertyKeys.basic, Boolean.FALSE); }
    public void setBasic(Boolean basic) { getStateHelper().put(PropertyKeys.basic, basic); }

    // Superclass overrides
    @Override
    public String getFamily() { return COMPONENT_FAMILY; }

    @Override
    public boolean getRendersChildren() { return true; }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        // do nothing, so button doesn't accept any children
    }
    
    // Encoding behaviour
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (context == null) { throw new NullPointerException(); }
        pushComponentToEL(context, null);
        if (!isRendered()) { return; }
        context.getApplication().publishEvent(context,PreRenderComponentEvent.class, this);

        boolean disabled = getDisabled();
        NavigationCase navigationCase = null;
        boolean navigationCaseNotFound = false;
        boolean isMultiViewId = false;
        boolean multiViewIdNotConfigured = false;
        if (!disabled) {
            navigationCase = getNavigationCase(context);
            if(navigationCase == null) {
                navigationCaseNotFound = true;
                context.getAttributes().put(NO_NAVIGATION_CASE, true);
                //throw new FacesException(String.format(ERROR_NAVIGATION_CASE_NOT_FOUND, getOutcome()));
            } else {
                isMultiViewId = isMultiViewsEnabled(context, navigationCase);
                multiViewIdNotConfigured = !isMultiViewId && hasDeclaredActivePathParams();
                if (multiViewIdNotConfigured) {
                    context.getAttributes().put(NO_MULTIVIEW_CONFIG, true);
                    //throw new FacesException(String.format(ERROR_MULTIVIEW_NOT_CONFIGURED, getOutcome()));
                }
            }
        }

        boolean effectivelyDisabled = disabled || navigationCase == null || multiViewIdNotConfigured;
        
        ResponseWriter writer = context.getResponseWriter();
        
        writer.startElement("button", this);
        writeIdAttributeIfNecessary(writer, this);
        writer.writeAttribute("name", getClientId(), "name");
        writer.writeAttribute("type", "button", null);

        String styleClass = generateStyleClass(effectivelyDisabled);
        writer.writeAttribute("class", styleClass, "styleClass");

        if (effectivelyDisabled) {
            writer.writeAttribute("disabled", "disabled", "disabled");
        }

        String href = (navigationCase != null && !multiViewIdNotConfigured) ? createTargetURL(context, navigationCase, isMultiViewId) : null;
        String onclick = generateOnclick(href);
        if(!"".equals(onclick)) {
            writer.writeAttribute("onclick", onclick, "click");
        }

        writeButtonAttributes(writer);
        writePassthroughAttrs(context, writer);

        //image is treated as a CSS class
        String image = getImage();
        if (image != null) {
            image = image.trim();
            if (!image.equals("")) {
                writer.startElement("span", null);
                writer.writeAttribute("class", "of-button-image " + image, null);
                writer.endElement("span");
            }
        }

        if(!(image != null && getValue() == null)) {
            writer.startElement("span", null);
            writer.writeAttribute("class", "of-button-text", null);
            writeValue(writer);
            writer.endElement("span");
        }

        if (!context.isProjectStage(ProjectStage.Production)) {
            if (navigationCaseNotFound) {
                String message = String.format(ERROR_NAVIGATION_CASE_NOT_FOUND, getOutcome());
                // FacesMessage was already added via navigation handler
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "{0} Component id: {1}.", new Object[]{message, getId()});
                }
            }
            if (multiViewIdNotConfigured) {
                String message = String.format(ERROR_MULTIVIEW_NOT_CONFIGURED, getOutcome());
                addFacesWarnMessage(context, message);
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "{0} Component id: {1}.", new Object[]{message, getId()});
                }
            }
        }
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        if (context == null) { throw new NullPointerException(); }
        if (!isRendered()) { popComponentFromEL(context); return; }
        ResponseWriter writer = context.getResponseWriter();
        
        boolean noNavigationCase = context.getAttributes().remove(NO_NAVIGATION_CASE) != null;
        boolean noMultiViewConfig = context.getAttributes().remove(NO_MULTIVIEW_CONFIG) != null;
        writer.endElement("button");
        
        if (!context.isProjectStage(ProjectStage.Production)) {
            String message = null;
            if (noNavigationCase) {
                message = String.format(ERROR_NAVIGATION_CASE_NOT_FOUND, getOutcome());
            }
            if (noMultiViewConfig) {
                message = String.format(ERROR_MULTIVIEW_NOT_CONFIGURED, getOutcome());
            }
            if (message != null) {
                writer.startElement("span", null);
                writer.writeAttribute("style", "margin-left: 3px; color: #DC143C", null);
                writer.write(message);
                writer.endElement("span");            
            }
        }
        
        popComponentFromEL(context);
    }

    // Protected methods
    /**
     * Generate onclick for the current button.
     * 
     * @param href href value to incorporate in onclick.
     * @return generated onclick.
     */
    protected String generateOnclick(String href) {
        StringBuilder onclick = new StringBuilder();
        String onclickAttribute = getOnclick();
        if (onclickAttribute != null) {
            onclickAttribute = onclickAttribute.trim();
            if (!onclickAttribute.equals("")) {
                onclick.append(onclickAttribute).append(onclickAttribute.endsWith(";") ? " " : "; ");
            }
        }
        if (href != null) {
            onclick.append("window.open('").append(href.trim()).append("', '")
                    .append(getTarget()).append("');");
        }
        return onclick.toString();
    }
    
    /**
     * Generate style class for the current button. Any active button has "of-button" class,
     * any disabled button has "of-button-disabled" class. User-defined classes are 
     * appended as well.
     * 
     * @param disabled effectively disabled flag of the button.
     * @return generated style class.
     */
    protected String generateStyleClass(boolean disabled) {
        StringBuilder styleClass = new StringBuilder(disabled ? "of-button-disabled" : "of-button");
        String styleClassAttribute = getStyleClass();
        if (styleClassAttribute != null) {
            styleClassAttribute = styleClassAttribute.trim();
            if (!styleClassAttribute.equals("")) {
                styleClass.append(" ").append(styleClassAttribute);
            }
        }
        return styleClass.toString();
    }

    /**
     * Try to resolve a navigation case for the outcome of the current
     * <code>Button</code> component.
     *
     * @param context <code>FacesContext</code> instance.
     * @return <code>NaigationCase</code> for the outcome of the current
     * <code>Button</code> component or <code>null</code> if navigation case
     * couldn't be resolved.
     */
    protected NavigationCase getNavigationCase(FacesContext context) {
        ConfigurableNavigationHandler navigationHandler = (ConfigurableNavigationHandler) context.getApplication().getNavigationHandler();
        String outcome = getOutcome();
        if (outcome == null) {
            outcome = context.getViewRoot().getViewId();
        }
        NavigationCase navigationCase;
        String toFlowDocumentId = (String)getAttributes().get(ActionListener.TO_FLOW_DOCUMENT_ID_ATTR_NAME);
        if (toFlowDocumentId != null) {
            navigationCase = navigationHandler.getNavigationCase(context, null, outcome, toFlowDocumentId);
        } else {
            navigationCase = navigationHandler.getNavigationCase(context, null, outcome);
        }
        return navigationCase;
    }

    /**
     * Check if at least one <code>PathParam</code> was declared as a child
     * of current component and is active or whether path parameter rendering, 
     * including strict one, was turned off.
     *
     * @return <code>true</code> if at least one <code>PathParam</code> has
     * to be rendered as a path parameter and <code>false</code> otherwise.
     */
    protected boolean hasDeclaredActivePathParams() {
        return (getBasic() || !getForce() || getChildCount() == 0) ? false : 
                getChildren().stream().anyMatch(Button::isActivePathParameter);
    }
    
    /**
     * Define if <code>MultiView</code> was configured for the target view id of
     * the <code>NavigationCase</code>.
     *
     * @param context <code>FacesContext</code> instance.
     * @param navigationCase <code>NavigationCase</code> for which presence of
     * <code>MultiView</code> configuration has to be determined.
     * @return <code>true</code> if <code>MultiView</code> was configured for
     * the target view id of the <code>NavigationCase</code> and
     * <code>false</code> otherwise.
     */
    protected boolean isMultiViewsEnabled(FacesContext context, NavigationCase navigationCase) {
        //TODO integrate better with Faces Views
        //A fix of this method would be: (1) to change visibility of method
        //FacesViews#isMultiViewsEnabled(ServletContext servletContext, String resource)
        //from package access to public and (2) modify the code to:
        //String resource = navigationCase.getToViewId(context);
        //resource = resource.substring(0, resource.lastIndexOf("."));
        //return FacesViews#isMultiViewsEnabled(context.getExternalContext().getContext(), resource);
        String resource = navigationCase.getToViewId(context);
        resource = resource.substring(0, resource.lastIndexOf("."));
        Set<String> multiviewsPaths = getApplicationAttribute((ServletContext) context.getExternalContext().getContext(), "org.omnifaces.facesviews.multiviews_paths");
        if (multiviewsPaths != null) {
            String path = resource + "/";
            for (String multiviewsPath : multiviewsPaths) {
                if (path.startsWith(multiviewsPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Writes defined attributes of the current component with the specified writer.
     * 
     * @param writer <code>ResponseWriter</code> to write component attributes with.
     * @throws RuntimeException that wraps <code>IOException</code>
     */
    protected void writeButtonAttributes(ResponseWriter writer) {
        Map<String, Object> attrs = getAttributes();//must call get on this map to evaluate property
        ATTRIBUTE_NAMES.entrySet().stream()
                .forEach(consumerRethrower((attr) -> {
                    Object attrVal = attrs.get(attr.getKey());
                    String value = attrVal == null ? null : attrVal.toString();
                    if(value != null) writer.writeAttribute(attr.getKey(), value, attr.getValue());
                }));
    }

    /**
     * Writes defined pass through attributes of the current component with the specified writer.
     * 
     * @param context <code>FacesContext</code> instance.
     * @param writer <code>ResponseWriter</code> to write pass through attributes with.
     * @throws RuntimeException that wraps <code>IOException</code>
     */
    protected void writePassthroughAttrs(FacesContext context, ResponseWriter writer) {
        Map<String, Object> passthroughAttrs = getPassThroughAttributes(false);
        if (passthroughAttrs != null && !passthroughAttrs.isEmpty()) {
            passthroughAttrs.entrySet().stream()
                    .forEach(consumerRethrower((attr) -> {
                        Object attrVal = attr.getValue();
                        Object attrValEv = attrVal instanceof ValueExpression ? ((ValueExpression)attrVal).getValue(context.getELContext()) : attrVal;
                        String value = attrValEv == null ? null : attrValEv.toString();
                        if(value != null) writer.writeAttribute(attr.getKey(), value, null);
                    }));
        }
    }

    /**
     * Writes the value of this component with <code>ResponseWriter</code>,
     * escaping content by default and not escaping it if escape attribute was
     * explicitly set to false.
     * 
     * @param writer <code>ResponseWriter</code> to write component attributes with.
     * @throws IOException 
     */
    protected void writeValue(ResponseWriter writer) throws IOException {
        Object value = getValue();
        if (value != null) {
            if (getEscape()) {
                writer.writeText(value, "value");
            } else {
                writer.write(value.toString());
            }
        } else {
            writer.write("Button");
        }
    }

    /**
     * Generate bookmarkable URL for the <code>NavigationCase</code> with query and 
     * path parameters gathered from nested <code>UIParameter</code> instances.
     * <code>ViewHandler::getBookmarkableURL</code> method is used to generate standard
     * bookmarkable URL and path fragment generated from <code>PathParam</code>
     * instances is added to that URL.
     * 
     * @param context <code>FacesContext</code> instance.
     * @param navigationCase <code>NavigationCase</code> where to create URL for.
     * @param isMultiViewId whether the destination view is a valid multi view id.
     * @return bookmarkable URL for the current component.
     */
    protected String createTargetURL(FacesContext context, NavigationCase navigationCase, boolean isMultiViewId) {
        Map<Integer, List<String>> pathParams = new LinkedHashMap<>();
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        fillDeclaredParams(pathParams, queryParams, isMultiViewId);
        addNavigationCaseParameters(navigationCase, queryParams);
        
        String href = getBaseBookmarkableURL(context, navigationCase, queryParams);
        
        if (!pathParams.isEmpty()) {
            String pathAddOn = generatePathFromParameters(pathParams);
            int querySeparator = href.indexOf("?");
            int index = querySeparator == -1 ? href.length() : querySeparator;
            href = new StringBuilder(href).insert(index, pathAddOn).toString();
        }
        if (getFragment() != null) {
            href += "#" + encodeURL(getFragment());
        }
        
        return href;
    }

    // Private methods
    /**
     * Fill specified maps with values of nested <code>UIParameter</code> values.
     * 
     * @param pathParams map of found path parameter values.
     * @param queryParams map of found query parameter values.
     * @param isMultiViewId whether the destination view is a valid multi view id.
     */
    private void fillDeclaredParams(final Map<Integer, List<String>> pathParams, final Map<String, List<String>> queryParams, boolean isMultiViewId) {
        if (getChildCount() > 0) {
            if(!getBasic()) {
                List<PathParam> pathParamsRaw = new ArrayList<>();
                boolean treatAsSimpleParams = !isMultiViewId && !getForce();

                for (UIComponent kid : getChildren()) {
                    boolean handled = false;

                    if (kid instanceof PathParam) {
                        PathParam param = (PathParam) kid;
                        if(!treatAsSimpleParams && !param.isDisable() && !param.getBasic()) {
                            pathParamsRaw.add(param);
                            handled = true;
                        }
                    }

                    if (kid instanceof UIParameter && !handled) {
                        UIParameter uiParam = (UIParameter) kid;
                        if (!uiParam.isDisable()) {
                            String name = uiParam.getName();
                            Object value = uiParam.getValue();
                            if (name != null) {
                                name = name.trim();
                                List<String> values = queryParams.get(name);
                                if (values == null) {
                                    values = new ArrayList<>();
                                    queryParams.put(name, values);
                                }
                                values.add(value instanceof String ? (String)value : value.toString());
                            }
                        }
                    }

                }

                if(!pathParamsRaw.isEmpty()) {
                    Integer properIndex = 0;
                    if(pathParamsRaw.stream().anyMatch(param -> param.getIndex() == null)) {
                        String emptyPositionString = getLocation();
                        Set<Integer> vals = pathParamsRaw.stream()
                                .filter(param -> param.getIndex() != null)
                                .map(param -> param.getIndex())
                                .distinct().collect(Collectors.toSet());
                        if ("end".equalsIgnoreCase(emptyPositionString)) {
                            Optional<Integer> index = vals.stream().max(Integer::compare);
                            properIndex = index.isPresent() ? index.get() + 1 : 0;
                        } else if ("beginning".equalsIgnoreCase(emptyPositionString)) {
                            Optional<Integer> index = vals.stream().min(Integer::compare);
                            properIndex = index.isPresent() ? index.get() - 1 : 0;
                        } else {
                            try {
                                properIndex = Integer.valueOf(emptyPositionString);
                            } catch (NumberFormatException nfe) {
                                Optional<Integer> index = vals.stream().max(Integer::compare);
                                properIndex = index.isPresent() ? index.get() + 1 : 0;
                            }
                        }
                    }

                    final Integer ind = properIndex == null ? 0 : properIndex;
                    Map<Integer, List<String>> pathParamsMap = pathParamsRaw.stream()
                            .collect(Collectors.groupingBy(param -> { Integer index = param.getIndex(); return index == null ? ind : index; }, 
                                    HashMap::new, 
                                    Collectors.mapping(param -> { Object val = param.getValue(); return val == null ? null : val.toString(); },
                                            Collectors.toCollection(ArrayList::new))));

                    pathParams.putAll(pathParamsMap);
                }
            } else {
                for (UIComponent kid : getChildren()) {
                    if (kid instanceof UIParameter) {
                        UIParameter uiParam = (UIParameter) kid;
                        if (!uiParam.isDisable()) {
                            String name = uiParam.getName();
                            Object value = uiParam.getValue();
                            if (name != null) {
                                name = name.trim();
                                List<String> values = queryParams.get(name);
                                if (values == null) {
                                    values = new ArrayList<>();
                                    queryParams.put(name, values);
                                }
                                values.add(value instanceof String ? (String)value : value.toString());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Add found navigation case parameters to a current query parameters map.
     * 
     * @param navigationCase <code>NavigationCase</code> where to look for navigation parameters.
     * @param queryParams map with query parameters to add found navigation parameters to.
     */
    private void addNavigationCaseParameters(NavigationCase navigationCase, Map<String, List<String>> queryParams) {
        Map<String, List<String>> navigationParameters = navigationCase.getParameters();
        if (navigationParameters != null && !navigationParameters.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : navigationParameters.entrySet()) {
                String name = entry.getKey();
                //do not overwrite explicitly defined parameters
                if (!queryParams.containsKey(name)) {
                    List<String> values = entry.getValue();
                    if (values.size() == 1) {
                        String value = values.get(0);
                        String sanitized = null != value && 2 < value.length() ? value.trim() : "";
                        if (sanitized.contains("#{") || sanitized.contains("${")) {
                            FacesContext context = FacesContext.getCurrentInstance();
                            value = context.getApplication().evaluateExpressionGet(context, value, String.class);
                            queryParams.put(name, Arrays.asList(value));
                        } else {
                            queryParams.put(name, values);
                        }
                    } else {
                        queryParams.put(name, values);
                    }
                }
            }
        }
        String toFlowDocumentId = navigationCase.getToFlowDocumentId();
        if (toFlowDocumentId != null) {
            List<String> flowDocumentIdValues = new ArrayList<>();
            flowDocumentIdValues.add(toFlowDocumentId);
            queryParams.put(FlowHandler.TO_FLOW_DOCUMENT_ID_REQUEST_PARAM_NAME, flowDocumentIdValues);
            if (!FlowHandler.NULL_FLOW.equals(toFlowDocumentId)) {
                List<String> flowIdValues = new ArrayList<>();
                flowIdValues.add(navigationCase.getFromOutcome());
                queryParams.put(FlowHandler.FLOW_ID_REQUEST_PARAM_NAME, flowIdValues);
            }
        }
    }
    
    /**
     * Generate bookmarkable URL for the <code>NavigationCase</code> with supplied 
     * query parameters by a call to <code>ViewHandler::getBookmarkableURL</code> method.
     * 
     * @param context <code>FacesContext</code> instance. 
     * @param navigationCase current <code>NavigationCase</code>.
     * @param params query parameters to be added to the generated URL.
     * @return generated bookmarkable URL with query parameters.
     */
    private String getBaseBookmarkableURL(FacesContext context, NavigationCase navigationCase, Map<String, List<String>> params) {
        String href;
        String toViewId = navigationCase.getToViewId(context);
        boolean isIncludeViewParams = isIncludeViewParams() || navigationCase.isIncludeViewParams();

        boolean clientWindowRenderingEnabled = false;
        ClientWindow clientWindow = null;
        try {
            if (isDisableClientWindow()) {
                clientWindow = context.getExternalContext().getClientWindow();
                if (clientWindow != null) {
                    clientWindowRenderingEnabled = clientWindow.isClientWindowRenderModeEnabled(context);
                    if (clientWindowRenderingEnabled) {
                        clientWindow.disableClientWindowRenderMode(context);
                    }
                }
            }
            ViewHandler viewHandler = context.getApplication().getViewHandler();
            href = viewHandler.getBookmarkableURL(context, toViewId, params, isIncludeViewParams);
        } finally {
            if (clientWindowRenderingEnabled && clientWindow != null) {
                clientWindow.enableClientWindowRenderMode(context);
            }
        }
        return href;
    }
    
    /**
     * Generate URI-encoded path from supplied path parameters.
     * 
     * @param pathParams path parameters to extract values from.
     * @return URI-encoded path value.
     */
    private String generatePathFromParameters(Map<Integer, List<String>> pathParams) {
        List<List<String>> params = pathParams.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        String path = params.stream()
                .flatMap(List::stream)
                .filter(g -> g != null && !"".equals(g))
                .map(g -> encodeURI(g))
                .collect(Collectors.joining("/"));
        return "".equals(path) ? "" : "/" + path;
    }
    
    // Helpers
    /**
     * Collect all attributes of the component that should be rendered.
     * 
     * @return unmodifiable map that contains all attributes of the component
     * that should be rendered.
     */
    private static Map<String, String> getButtonAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("accesskey", "accesskey");
        attrs.put("alt", "alt");
        attrs.put("dir", "dir");
        attrs.put("lang", "lang");
        attrs.put("onblur", "blur");
        attrs.put("onclick", "click");
        attrs.put("ondblclick", "dblclick");
        attrs.put("onfocus", "focus");
        attrs.put("onkeydown", "keydown");
        attrs.put("onkeypress", "keypress");
        attrs.put("onkeyup", "keyup");
        attrs.put("onmousedown", "mousedown");
        attrs.put("onmousemove", "mousemove");
        attrs.put("onmouseout", "mouseout");
        attrs.put("onmouseover", "mouseover");
        attrs.put("onmouseup", "mouseup");
        attrs.put("role", "role");
        attrs.put("style", "style");
        attrs.put("tabindex", "tabindex");
        attrs.put("title", "title");
        return Collections.unmodifiableMap(attrs);
    }

    /**
     * Functional interface that declares <code>Consumer</code> method that 
     * throws <code>Exception</code>.
     * 
     * @param <T> input type.
     * @param <E> exception type.
     */
    @FunctionalInterface
    public interface ConsumerChecked<T, E extends Exception> {
        void accept(T t) throws E;
    }

    /**
     * Rethrow a checked <code>Exception</code> by wrapping it in a 
     * <code>RuntimeException</code>.
     * 
     * @param <T> input type.
     * @param <E> exception type.
     * @param consumerChecked consumerChecked instance.
     * @return wrapped <code>Consumer</code> functional interface.
     */
    public static <T, E extends Exception> Consumer<T> consumerRethrower(ConsumerChecked<T, E> consumerChecked) {
        return i -> {
            try {
                consumerChecked.accept(i);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /**
     * Check if a component is an active path parameter.
     * 
     * @param component UIComponent to check.
     * @return <code>true</code> if parameter is an active path parameter and
     * <code>false</code> otherwise.
     */
    public static boolean isActivePathParameter(UIComponent component) {
        return component instanceof PathParam && !((PathParam) component).isDisable() && !((PathParam) component).getBasic(); // rendered not taken into account
    }

    /**
     * Adds a <code>FacesMessage</code> with severity <code>SEVERITY_WARN</code>
     * to the <code>FacesContext</code>.
     * 
     * @param context <code>FacesContext</code> to add message to.
     * @param message <code>FacesMessage</code> to be added.
     */
    public static void addFacesWarnMessage(FacesContext context, String message) {
        FacesMessage msg = new FacesMessage(message);
        msg.setSeverity(FacesMessage.SEVERITY_WARN);
        context.addMessage(null, msg);
    }
    
}
