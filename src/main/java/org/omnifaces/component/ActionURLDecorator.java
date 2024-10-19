package org.omnifaces.component;

import static jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI;
import static org.omnifaces.util.ComponentsLocal.getParams;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;
import static org.omnifaces.util.FacesLocal.getRequestURI;
import static org.omnifaces.util.Servlets.toQueryString;
import static org.omnifaces.util.Utils.formatURLWithQueryString;

import jakarta.faces.application.Application;
import jakarta.faces.application.ApplicationWrapper;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.application.ViewHandlerWrapper;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;

import org.omnifaces.component.input.Form;
import org.omnifaces.component.output.Link;

/**
 * Helper class used for creating a FacesContext with a decorated FacesContext -&gt; Application -&gt; ViewHandler
 * -&gt; getActionURL.
 *
 * @author Arjan Tijms
 * @since 4.5
 * @see Form
 * @see Link
 */
public class ActionURLDecorator extends FacesContextWrapper {

    private final UIComponent component;
    private final boolean useRequestURI;
    private final boolean includeRequestParams;

    public ActionURLDecorator(FacesContext wrapped, UIComponent component, boolean useRequestURI, boolean includeRequestParams) {
        super(wrapped);
        this.component = component;
        this.useRequestURI = useRequestURI;
        this.includeRequestParams = includeRequestParams;
    }

    @Override
    public Application getApplication() {
        return new ActionURLDecoratorApplication(getWrapped().getApplication());
    }

    private class ActionURLDecoratorApplication extends ApplicationWrapper {

        public ActionURLDecoratorApplication(Application wrapped) {
            super(wrapped);
        }

        @Override
        public ViewHandler getViewHandler() {
            return new ActionURLDecoratorViewHandler(getWrapped().getViewHandler());
        }

        private class ActionURLDecoratorViewHandler extends ViewHandlerWrapper {

            public ActionURLDecoratorViewHandler(ViewHandler wrapped) {
                super(wrapped);
            }

            /**
             * The actual method we're decorating in order to either include the view parameters into the
             * action URL, or include the request parameters into the action URL, or use request URI as
             * action URL. Any <code>&lt;f|o:param&gt;</code> nested in the form component will be included
             * in the query string, overriding any existing view or request parameters on same name.
             */
            @Override
            public String getActionURL(FacesContext context, String viewId) {
                var actionURL = useRequestURI && !includeRequestParams ? getActionURL(context) : getWrapped().getActionURL(context, viewId);
                var queryString = toQueryString(getParams(context, component, useRequestURI || includeRequestParams, false));
                return formatURLWithQueryString(actionURL, queryString);
            }

            private String getActionURL(FacesContext context) {
                var actionURL = getRequestAttribute(context, ERROR_REQUEST_URI) != null ? getRequestContextPath(context) : getRequestURI(context);
                return actionURL.isEmpty() ? "/" : actionURL;
            }
        }
    }
}
