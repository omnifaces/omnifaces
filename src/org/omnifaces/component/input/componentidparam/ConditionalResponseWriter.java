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
package org.omnifaces.component.input.componentidparam;

import static org.omnifaces.util.Utils.isOneOf;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.context.ResponseWriterWrapper;

import org.omnifaces.component.input.ComponentIdParam;

/**
 * ResponseWriter intended to work in conjunction with the {@link ComponentIdParam} component.
 * <p>
 * This allows rendering to proceed to the output if the current component matches any of the given ids, otherwise simply does not send anything to
 * the output.
 * 
 * @since 1.1
 * @author Arjan Tijms
 * 
 */
public class ConditionalResponseWriter extends ResponseWriterWrapper {

    private final ResponseWriter responseWriter;
    private final FacesContext facesContext;
    private final List<String> componentIds;
    private final List<String> clientIds;

    private UIComponent lastComponent;
    private boolean lastRendered;

    public ConditionalResponseWriter(ResponseWriter responseWriter, FacesContext facesContext, List<String> componentIds, List<String> clientIds) {
        this.responseWriter = responseWriter;
        this.facesContext = facesContext;
        this.componentIds = componentIds;
        this.clientIds = clientIds;
    }

    // ResponseWriter overrides that do some kind of writing

    @Override
    public void endCDATA() throws IOException {
        if (isForRenderedComponent()) {
            super.endCDATA();
        }
    }

    @Override
    public void endElement(String name) throws IOException {
        if (isForRenderedComponent()) {
            super.endElement(name);
        }
    }

    @Override
    public void endDocument() throws IOException {
        if (isForRenderedComponent()) {
            super.endDocument();
        }
    }

    @Override
    public void startCDATA() throws IOException {
        if (isForRenderedComponent()) {
            super.startCDATA();
        }
    }

    @Override
    public void startDocument() throws IOException {
        if (isForRenderedComponent()) {
            super.startDocument();
        }
    }

    @Override
    public void startElement(String name, UIComponent component) throws IOException {
        if (isForRenderedComponent()) {
            super.startElement(name, component);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (isForRenderedComponent()) {
            super.write(cbuf, off, len);
        }
    }

    @Override
    public void writeAttribute(String name, Object value, String property) throws IOException {
        if (isForRenderedComponent()) {
            super.writeAttribute(name, value, property);
        }
    }

    @Override
    public void writeComment(Object comment) throws IOException {
        if (isForRenderedComponent()) {
            super.writeComment(comment);
        }
    }

    @Override
    public void writeText(char[] text, int off, int len) throws IOException {
        if (isForRenderedComponent()) {
            super.writeText(text, off, len);
        }
    }

    @Override
    public void writeText(Object text, String property) throws IOException {
        if (isForRenderedComponent()) {
            super.writeText(text, property);
        }
    }

    @Override
    public void writeText(Object text, UIComponent component, String property) throws IOException {
        if (isForRenderedComponent()) {
            super.writeText(text, component, property);
        }
    }

    @Override
    public void writeURIAttribute(String name, Object value, String property) throws IOException {
        if (isForRenderedComponent()) {
            super.writeURIAttribute(name, value, property);
        }
    }

    // Writer overrides

    @Override
    public Writer append(char c) throws IOException {
        if (isForRenderedComponent()) {
            return super.append(c);
        }

        return this;
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        if (isForRenderedComponent()) {
            return super.append(csq);
        }

        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        if (isForRenderedComponent()) {
            return super.append(csq, start, end);
        }

        return this;
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        // TODO Auto-generated method stub
        super.write(cbuf);
    }

    @Override
    public void write(int c) throws IOException {
        if (isForRenderedComponent()) {
            super.write(c);
        }
    }

    @Override
    public void write(String str) throws IOException {
        if (isForRenderedComponent()) {
            super.write(str);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        if (isForRenderedComponent()) {
            super.write(str, off, len);
        }
    }

    private boolean isForRenderedComponent() {
        UIComponent currentComponent = UIComponent.getCurrentComponent(facesContext);

        // Typically a single component writes multiple times to the response writer in quick succession.
        // Shortcut id matching by doing a cheaper check on the outcome of the last processed component.
        if (lastComponent == currentComponent) {
            return lastRendered;
        }

        lastComponent = currentComponent;

        return lastRendered = isOneOf(currentComponent.getId(), componentIds) || isOneOf(currentComponent.getClientId(), clientIds);
    }

    @Override
    public ResponseWriter getWrapped() {
        return responseWriter;
    }

}
