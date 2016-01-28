package org.omnifaces.taghandler;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.validator.Validator;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import org.omnifaces.util.Components;
import org.omnifaces.util.Events;
import org.omnifaces.util.Faces;


/**
 * A tag handler to skip validation.
 * 
 * @author Michele Mariotti
 */
public class SkipValidators extends TagHandler
{
    public static final String SKIP_VALIDATORS = "org.omnifaces.SKIP_VALIDATORS";

    public SkipValidators(TagConfig config)
    {
        super(config);
    }

    @Override
    public void apply(final FaceletContext context, final UIComponent parent) throws IOException
    {
        if(ComponentHandler.isNew(parent))
        {
            parent.subscribeToEvent(PostAddToViewEvent.class, new PostAddToViewEventListener());

            Events.subscribeToViewEvent(PreValidateEvent.class, new PreValidateEventListener());
            Events.subscribeToViewEvent(PostValidateEvent.class, new PostValidateEventListener());
        }
    }

    /**
     * Check if the given component has been invoked during the current request and if so, set a flag to skip validators.
     */
    public static class PostAddToViewEventListener implements ComponentSystemEventListener
    {
        @Override
        public void processEvent(ComponentSystemEvent event) throws AbortProcessingException
        {
            UIComponent component = event.getComponent();

            if(Components.hasInvokedSubmit(component))
            {
                Faces.setContextAttribute(SKIP_VALIDATORS, true);
            }
        }
    }

    /**
     * Remove validators and save them to be restored later.
     */
    public static class PreValidateEventListener implements SystemEventListener
    {
        @Override
        public void processEvent(SystemEvent event) throws AbortProcessingException
        {
            UIInput input = (UIInput) event.getSource();

            boolean skipValidators = Faces.getContextAttribute(SKIP_VALIDATORS) == Boolean.TRUE;

            if(skipValidators)
            {
                Faces.setContextAttribute(input.getClientId() + ".required", input.isRequired());
                input.setRequired(false);

                Validator[] validators = input.getValidators();
                if(validators != null)
                {
                    Faces.setContextAttribute(input.getClientId() + ".validators", validators);
                    for(Validator validator : validators)
                    {
                        input.removeValidator(validator);
                    }
                }
            }
        }

        @Override
        public boolean isListenerForSource(Object source)
        {
            return source instanceof UIInput;
        }
    }

    /**
     * Restore previously saved validators.
     */
    public static class PostValidateEventListener implements SystemEventListener
    {
        @Override
        public void processEvent(SystemEvent event) throws AbortProcessingException
        {
            UIInput input = (UIInput) event.getSource();

            boolean skipValidators = Faces.getContextAttribute(SKIP_VALIDATORS) == Boolean.TRUE;

            if(skipValidators)
            {
                boolean required = Faces.getContextAttribute(input.getClientId() + ".required") == Boolean.TRUE;
                input.setRequired(required);

                Validator[] validators = Faces.getContextAttribute(input.getClientId() + ".validators");
                if(validators != null)
                {
                    for(Validator validator : validators)
                    {
                        input.addValidator(validator);
                    }
                }
            }
        }

        @Override
        public boolean isListenerForSource(Object source)
        {
            return source instanceof UIInput;
        }
    }
}
