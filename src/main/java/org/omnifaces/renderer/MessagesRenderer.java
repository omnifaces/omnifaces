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
package org.omnifaces.renderer;

import static org.omnifaces.util.ComponentsLocal.forEachComponent;
import static org.omnifaces.util.Messages.createInfo;
import static org.omnifaces.util.Renderers.writeAttribute;
import static org.omnifaces.util.Renderers.writeAttributes;
import static org.omnifaces.util.Renderers.writeText;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isOneOf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.FacesMessage.Severity;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UIMessages;
import jakarta.faces.context.FacesContext;
import jakarta.faces.render.FacesRenderer;
import jakarta.faces.render.Renderer;

import org.omnifaces.component.messages.OmniMessages;

/**
 * This renderer is the default renderer of {@link OmniMessages}. It's basically copypasted from Mojarra 2.2,
 * including the fix of tooltip rendering as described in <a href="https://github.com/eclipse-ee4j/mojarra/issues/2164">
 * issue 2164</a>, and afterwards slightly rewritten, refactored and enhanced to take the new features into account.
 *
 * @author Bauke Scholtz
 * @since 1.5
 */
@FacesRenderer(componentFamily=UIMessages.COMPONENT_FAMILY, rendererType=MessagesRenderer.RENDERER_TYPE)
public class MessagesRenderer extends Renderer {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The standard renderer type. */
    public static final String RENDERER_TYPE = "org.omnifaces.Messages";

    // Private constants ----------------------------------------------------------------------------------------------

    private static final String LAYOUT_TABLE = "table";
    private static final Map<Severity, String> SEVERITY_NAMES = createSeverityNames();

    private static Map<Severity, String> createSeverityNames() {
        Map<Severity, String> severityNames = new HashMap<>();
        severityNames.put(FacesMessage.SEVERITY_INFO, "info");
        severityNames.put(FacesMessage.SEVERITY_WARN, "warn");
        severityNames.put(FacesMessage.SEVERITY_ERROR, "error");
        severityNames.put(FacesMessage.SEVERITY_FATAL, "fatal");
        return Collections.unmodifiableMap(severityNames);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Returns <code>true</code>.
     */
    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        if (!component.isRendered()) {
            return;
        }

        var omniMessages = (OmniMessages) component;
        var messages = getMessages(context, omniMessages);

        if (!isEmpty(omniMessages.getVar()) && omniMessages.getChildCount() > 0) {
            encodeMessagesRepeater(context, omniMessages, messages);
        }
        else if (messages.isEmpty()) {
            encodeEmptyMessages(context, omniMessages);
        }
        else {
            var message = omniMessages.getMessage();

            if (!isEmpty(message)) {
                messages = Arrays.asList(createInfo(message));
            }

            encodeMessages(context, omniMessages, messages, LAYOUT_TABLE.equals(omniMessages.getLayout()));
        }
    }

    /**
     * Collect all messages associated with components identified by <code>for</code> attribute and return it. An empty
     * list will be returned when there are no messages.
     * @param context The involved faces context.
     * @param component The messages component.
     * @return All messages associated with components identified by <code>for</code> attribute.
     */
    protected List<FacesMessage> getMessages(FacesContext context, OmniMessages component) {
        var forClientIds = component.getFor();

        if (forClientIds == null) {
            return component.isGlobalOnly() ? context.getMessageList(null) : context.getMessageList();
        }

        List<FacesMessage> messages = new ArrayList<>();

        for (String forClientId : forClientIds.split("\\s+")) {
            var forComponent = component.findComponent(forClientId);

            if (forComponent == null) {
                continue;
            }

            messages.addAll(context.getMessageList(forComponent.getClientId(context)));

            if (!(forComponent instanceof UIInput)) {
                forEachComponent(context).fromRoot(forComponent).ofTypes(UIInput.class)
                    .invoke(input -> messages.addAll(context.getMessageList(input.getClientId(context))));
            }
        }

        return messages;
    }

    /**
     * Encode the case when the <code>var</code> attribute is specified. This will render without any HTML markup and
     * put the current message in the request scope as identified by the <code>var</code> attribute.
     * Note: the iteration is by design completely stateless.
     * @param context The involved faces context.
     * @param component The messages component.
     * @param messages The queued faces messages.
     * @throws IOException When an I/O error occurs.
     */
    protected void encodeMessagesRepeater(FacesContext context, OmniMessages component, List<FacesMessage> messages)
        throws IOException
    {
        var var = component.getVar();
        var requestMap = context.getExternalContext().getRequestMap();
        var originalVar = requestMap.get(var);

        try {
            for (FacesMessage message : messages) {
                if (message.isRendered() && !component.isRedisplay()) {
                    continue;
                }

                requestMap.put(var, message);

                for (UIComponent child : component.getChildren()) {
                    child.encodeAll(context);
                }

                message.rendered();
            }
        }
        finally {
            if (originalVar != null) {
                requestMap.put(var, originalVar);
            }
            else {
                requestMap.remove(var);
            }
        }
    }

    /**
     * Encode the case when there are no messages. This will render a div when the ID is specified.
     * @param context The involved faces context.
     * @param component The messages component.
     * @throws IOException When an I/O error occurs.
     */
    protected void encodeEmptyMessages(FacesContext context, OmniMessages component) throws IOException {
        var id = component.getId();

        if (!isOneOf(id, null, "jakarta_faces_developmentstage_messages")) {
            var writer = context.getResponseWriter();
            writer.startElement("div", component);
            writeAttribute(writer, "id", component.getClientId(context));
            writeAttribute(writer, component, "styleClass", "class");
            writer.endElement("div");
        }
    }

    /**
     * Encode the case when the faces messages are to be rendered as either a HTML table or a HTML list.
     * @param context The involved faces context.
     * @param component The messages component.
     * @param messages The queued faces messages.
     * @param table Whether to render the messages as a HTML table or a HTML list.
     * @throws IOException When an I/O error occurs.
     */
    protected void encodeMessages
        (FacesContext context, OmniMessages component, List<FacesMessage> messages, boolean table)
            throws IOException
    {
        var writer = context.getResponseWriter();
        writer.startElement(table ? LAYOUT_TABLE : "ul", component);
        writeAttribute(writer, "id", component.getClientId(context));
        writeAttribute(writer, component, "styleClass", "class");
        writeAttributes(writer, component, "style", "title", "lang", "dir");

        for (FacesMessage message : messages) {
            if (!message.isRendered() || component.isRedisplay()) {
                encodeMessage(context, component, message, table);
                message.rendered();
            }
        }

        writer.endElement(table ? LAYOUT_TABLE : "ul");
    }

    /**
     * Encode a single faces message.
     * @param context The involved faces context.
     * @param component The messages component.
     * @param message The queued faces message.
     * @param table Whether to render the messages as a HTML table or a HTML list.
     * @throws IOException When an I/O error occurs.
     */
    protected void encodeMessage(FacesContext context, OmniMessages component, FacesMessage message, boolean table) throws IOException {
        var writer = context.getResponseWriter();
        writer.startElement(table ? "tr" : "li", component);
        var severityName = SEVERITY_NAMES.get(message.getSeverity());
        writeAttribute(writer, component, severityName + "Style", "style");
        writeAttribute(writer, component, severityName + "Class", "class", "styleClass");

        if (table) {
            writer.startElement("td", component);
        }

        var summary = coalesce(message.getSummary(), "");
        var detail = coalesce(message.getDetail(), summary);

        if (component.isTooltip() && isEmpty(component.getTitle())) {
            writeAttribute(writer, "title", detail);
        }

        if (component.isShowSummary()) {
            writeText(writer, component, summary, component.isEscape());

            if (component.isShowDetail()) {
                writer.write(" ");
            }
        }

        if (component.isShowDetail()) {
            writeText(writer, component, detail, component.isEscape());
        }

        if (table) {
            writer.endElement("td");
        }

        writer.endElement(table ? "tr" : "li");
    }

}