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
package org.omnifaces.util;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.omnifaces.util.Components.getCurrentComponent;
import static org.omnifaces.util.Components.getCurrentForm;
import static org.omnifaces.util.FacesLocal.createResource;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;

import jakarta.faces.FacesException;
import jakarta.faces.component.UIColumn;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIData;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.PartialViewContext;

import org.omnifaces.context.OmniPartialViewContext;

/**
 * <p>
 * Collection of utility methods for the Faces API with respect to working with {@link PartialViewContext}.
 * <p>
 * The difference with {@link Ajax} is that no one method of {@link AjaxLocal} obtains the {@link FacesContext}
 * from the current thread by {@link FacesContext#getCurrentInstance()}. This job is up to the caller. This is more
 * efficient in situations where multiple utility methods needs to be called at the same time. Invoking
 * {@link FacesContext#getCurrentInstance()} is at its own an extremely cheap operation, however as it's to be obtained
 * as a {@link ThreadLocal} variable, it's during the call still blocking all other running threads for some nanoseconds
 * or so.
 *
 * @author Bauke Scholtz
 * @since 4.6
 * @see Ajax
 */
public final class AjaxLocal {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final String ERROR_NO_SCRIPT_RESOURCE =
        "";
    private static final String ERROR_NO_PARTIAL_RENDERING =
        "The current request is not an ajax request with partial rendering."
            + " Use Components#addScriptXxx() methods instead.";
    private static final String ERROR_ARGUMENTS_LENGTH =
        "The arguments length must be even. Encountered %d items.";
    private static final String ERROR_ARGUMENT_TYPE =
        "The argument name must be a String. Encountered type '%s' with value '%s'.";

    // Constructors ---------------------------------------------------------------------------------------------------

    private AjaxLocal() {
        // Hide constructor.
    }

    // Shortcuts ------------------------------------------------------------------------------------------------------

    /**
     * @see Ajax#getContext()
     */
    public static PartialViewContext getContext(FacesContext context) {
        return context.getPartialViewContext();
    }

    /**
     * @see Ajax#update(String...)
     */
    public static void update(FacesContext context, String... clientIds) {
        var renderIds = getContext(context).getRenderIds();

        for (String clientId : clientIds) {
            if (clientId.charAt(0) != '@') {
                renderIds.add(clientId);
            }
            else if ("@all".equals(clientId)) {
                updateAll(context);
            }
            else if ("@form".equals(clientId)) {
                UIComponent currentForm = getCurrentForm();

                if (currentForm != null) {
                    renderIds.add(currentForm.getClientId());
                }
            }
            else if ("@this".equals(clientId)) {
                var currentComponent = getCurrentComponent();

                if (currentComponent != null) {
                    renderIds.add(currentComponent.getClientId());
                }
            }
        }
    }

    /**
     * @see Ajax#updateAll()
     */
    public static void updateAll(FacesContext context) {
        getContext(context).setRenderAll(true);
    }

    /**
     * @see Ajax#updateRow(UIData, int)
     */
    public static void updateRow(FacesContext context, UIData table, int index) {
        if (index < 0 || table.getRowCount() < 1 || index >= table.getRowCount() || table.getChildCount() == 0) {
            return;
        }

        updateRowCells(context, table, index);
    }

    private static void updateRowCells(FacesContext context, UIData table, int index) {
        var parentId = table.getParent().getNamingContainer().getClientId(context);
        var tableId = table.getId();
        var separator = UINamingContainer.getSeparatorChar(context);
        var renderIds = getContext(context).getRenderIds();

        for (UIComponent column : table.getChildren()) {
            if (column instanceof UIColumn) {
                if (!column.isRendered()) {
                    continue;
                }

                for (UIComponent cell : column.getChildren()) {
                    if (!cell.isRendered()) {
                        continue;
                    }

                    renderIds.add(format("%s%c%s%c%d%c%s", parentId, separator, tableId, separator, index, separator, cell.getId()));
                }
            }
            else if (column instanceof UIData) { // <p:columns>.
                updateRowCells((UIData) column, renderIds, tableId, index, separator);
            }
        }
    }

    private static void updateRowCells(UIData columns, Collection<String> renderIds, String tableId, int index, char separator) {
        var columnId = columns.getId();
        var columnCount = columns.getRowCount();

        for (UIComponent cell : columns.getChildren()) {
            if (!cell.isRendered()) {
                continue;
            }

            for (var columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                renderIds.add(format("%s%c%d%c%s%c%d%c%s", tableId, separator, index, separator, columnId, separator, columnIndex, separator, cell.getId()));
            }
        }
    }

    /**
     * @see Ajax#updateColumn(UIData, int)
     */
    public static void updateColumn(FacesContext context, UIData table, int index) {
        if (index < 0 || table.getRowCount() < 1 || index > table.getChildCount()) {
            return;
        }

        var rowCount = table.getRows() == 0 ? table.getRowCount() : table.getRows();

        if (rowCount == 0) {
            return;
        }

        updateColumnCells(context, table, index, rowCount);
    }

    private static void updateColumnCells(FacesContext context, UIData table, int index, int rowCount) {
        var parentId = table.getParent().getNamingContainer().getClientId(context);
        var tableId = table.getId();
        var separator = UINamingContainer.getSeparatorChar(context);
        var renderIds = getContext(context).getRenderIds();
        var column = findColumn(table, index);

        if (column != null && column.isRendered()) {
            for (UIComponent cell : column.getChildren()) {
                if (!cell.isRendered()) {
                    continue;
                }

                var cellId = cell.getId();

                for (var rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                    renderIds.add(format("%s%c%s%c%d%c%s", parentId, separator, tableId, separator, rowIndex, separator, cellId));
                }
            }
        }
    }

    private static UIColumn findColumn(UIData table, int index) {
        var columnIndex = 0;

        for (UIComponent column : table.getChildren()) {
            if (column instanceof UIColumn && columnIndex++ == index) {
                return (UIColumn) column;
            }
        }

        return null;
    }

    /**
     * @see Ajax#load(String, String)
     */
    public static void load(FacesContext context, String libraryName, String resourceName) {
        var resource = createResource(context, libraryName, resourceName);

        if (resource == null) {
            throw new IllegalArgumentException(ERROR_NO_SCRIPT_RESOURCE);
        }

        try (var scanner = new Scanner(resource.getInputStream(), UTF_8)) {
            oncomplete(context, scanner.useDelimiter("\\A").next());
        }
        catch (IOException e) {
            throw new FacesException(e);
        }
    }

    /**
     * @see Ajax#oncomplete(String...)
     */
    public static void oncomplete(FacesContext context, String... scripts) {
        if (!isAjaxRequestWithPartialRendering(context)) {
            throw new IllegalStateException(ERROR_NO_PARTIAL_RENDERING);
        }

        var omniContext = OmniPartialViewContext.getCurrentInstance(context);

        for (String script : scripts) {
            omniContext.addCallbackScript(script);
        }
    }

    /**
     * @see Ajax#data(String, Object)
     */
    public static void data(FacesContext context, String name, Object value) {
        OmniPartialViewContext.getCurrentInstance(context).addArgument(name, value);
    }

    /**
     * @see Ajax#data(Object...)
     */
    public static void data(FacesContext context, Object... namesValues) {
        if (namesValues.length % 2 != 0) {
            throw new IllegalArgumentException(format(ERROR_ARGUMENTS_LENGTH, namesValues.length));
        }

        var omniContext = OmniPartialViewContext.getCurrentInstance(context);

        for (var i = 0; i < namesValues.length; i+= 2) {
            if (!(namesValues[i] instanceof String)) {
                var type = namesValues[i] != null ? namesValues[i].getClass().getName() : "null";
                throw new IllegalArgumentException(format(ERROR_ARGUMENT_TYPE, type, namesValues[i]));
            }

            omniContext.addArgument((String) namesValues[i], namesValues[i + 1]);
        }
    }

    /**
     * @see Ajax#data(Map)
     */
    public static void data(FacesContext context, Map<String, Object> data) {
        var omniContext = OmniPartialViewContext.getCurrentInstance(context);

        for (var entry : data.entrySet()) {
            omniContext.addArgument(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @see Ajax#isExecuted(String)
     */
    public static boolean isExecuted(FacesContext context, String clientId) {
        return getContext(context).getExecuteIds().contains(clientId);
    }
}