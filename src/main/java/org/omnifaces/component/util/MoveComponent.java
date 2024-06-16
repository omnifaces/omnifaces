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
package org.omnifaces.component.util;

import static java.lang.String.format;
import static org.omnifaces.component.util.MoveComponent.Destination.ADD_LAST;
import static org.omnifaces.component.util.MoveComponent.PropertyKeys.behaviorDefaultEvent;
import static org.omnifaces.component.util.MoveComponent.PropertyKeys.behaviorEvents;
import static org.omnifaces.component.util.MoveComponent.PropertyKeys.destination;
import static org.omnifaces.component.util.MoveComponent.PropertyKeys.facet;
import static org.omnifaces.util.Components.findComponentRelatively;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Faces.isPostback;
import static org.omnifaces.util.Utils.csvToList;
import static org.omnifaces.util.Utils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.event.PostAddToViewEvent;
import jakarta.faces.event.PreRenderViewEvent;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;

import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:moveComponent&gt;</code> component is a utility component via which components, facets and behaviors can be moved at runtime
 * to a target component in various ways. This allows for simple programmatic composition of components using a declarative page author
 * centric approach.
 *
 * <p>
 * The destination of a move operation is specified in terms of a location that's relative to a given target component. The following
 * shows a list of supported destinations:
 *
 * <ul>
 *         <li> <code>BEFORE</code>    - Component is moved right before target component, i.e. as a sibling with an index that's 1 position lower.
 *        <li> <code>ADD_FIRST</code> - Component is added as the first child of the target component, any other children will have their index increased by 1.
 *        <li> <code>ADD_LAST</code>  - Component is added as the last child of the target component, any other children will stay at their original location.
 *        <li> <code>FACET</code>     - Component will be moved to the facet section of the target component under the name denoted by "facet".
 *        <li> <code>BEHAVIOR</code>  - A Behavior will be moved to the behavior section of the target component.
 *        <li> <code>AFTER</code>     - Component is moved right after target component, i.e. as a sibling with an index that's 1 position higher.
 * </ul>
 *
 * @since 2.0
 * @author Arjan Tijms
 * @see UtilFamily
 */
@FacesComponent(MoveComponent.COMPONENT_TYPE)
public class MoveComponent extends UtilFamily implements SystemEventListener, ClientBehaviorHolder {

    /** The component type, which is {@value org.omnifaces.component.util.MoveComponent#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.util.MoveComponent";

    private static final String ERROR_COMPONENT_NOT_FOUND =
        "A component with ID '%s' as specified by the 'for' attribute of the MoveComponent with Id '%s' could not be found.";

    /** The default scope, which is "facelet". */
    public static final String DEFAULT_SCOPE = "facelet";

    private final State state = new State(getStateHelper());

    enum PropertyKeys {
        /* for */ facet, destination, behaviorDefaultEvent, behaviorEvents
    }

    /**
     * Destination of component to be moved by {@link MoveComponent}.
     */
    public enum Destination {

        /**
         * Component is moved right before target component, i.e. as a sibling with an index that's 1 position lower.
         */
        BEFORE,

        /**
         * Component is added as the first child of the target component, any other children will have their index increased by 1.
         */
        ADD_FIRST,

        /**
         * Component is added as the last child of the target component, any other children will stay at their original location.
         */
        ADD_LAST,

        /**
         * Component will be moved to the facet section of the target component under the name denoted by "facet".
         */
        FACET,

        /**
         * A Behavior will be moved to the behavior section of the target component.
         */
        BEHAVIOR,

        /**
         * Component is moved right after target component, i.e. as a sibling with an index that's 1 position higher.
         */
        AFTER
    }

    private String attachedEventName;
    private ClientBehavior attachedBehavior;

    // Used to fool over-eager tag handlers that check in advance whether a given component indeed
    // supports the event for which a behavior is attached.
    private List<String> containsTrueList = new ArrayList<>() {
        private static final long serialVersionUID = 1L;
        @Override
        public boolean contains(Object o) {
            return true;
        }
    };

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Constructs the component.
     */
    public MoveComponent() {
        subscribeToViewEvent(isPostback() ? PostAddToViewEvent.class : PreRenderViewEvent.class, this);
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return source instanceof UIViewRoot;
    }

    @Override
    public void processEvent(SystemEvent event) {
        if (event instanceof PreRenderViewEvent || event instanceof PostAddToViewEvent) {
            doProcess();
        }
    }

    @Override
    public void addClientBehavior(String eventName, ClientBehavior behavior) {
        attachedEventName = eventName;
        attachedBehavior = behavior;
    }

    @Override
    public String getDefaultEventName() {
        return getBehaviorDefaultEvent();
    }

    @Override
    public Collection<String> getEventNames() {
        if (isEmpty(getBehaviorEvents())) {
            return containsTrueList;
        }

        return csvToList(getBehaviorEvents());
    }

    private void doProcess() {
        var forValue = getFor();

        if (!isEmpty(forValue)) {
            var component = findComponentRelatively(this, forValue);

            if (component == null) {
                component = findComponent(forValue);
            }

            if (component == null) {
                throw new IllegalArgumentException(format(ERROR_COMPONENT_NOT_FOUND, forValue, getId()));
            }

            var children = getChildrenInNewList();

            switch (getDestination()) {
                case BEFORE:
                    moveBefore(component, children);
                    break;
                case ADD_FIRST:
                    moveAddFirst(component, children);
                    break;
                case ADD_LAST:
                    moveAddLast(component, children);
                    break;
                case FACET:
                    moveFacet(component, children);
                    break;
                case BEHAVIOR:
                    moveBehavior(component, attachedEventName, attachedBehavior);
                    break;
                case AFTER:
                    moveAfter(component, children);
                    break;
            }
        }
    }

    private static void moveBefore(UIComponent component, List<UIComponent> children) {
        // Find the index the target component has among its siblings, this directly
        // becomes the target index of where to insert all the components that are
        // to be moved.
        var targetIndex = component.getParent().getChildren().indexOf(component);

        for (var i = 0; i < children.size(); i++) {
            var childComponent = children.get(i);
            component.getParent().getChildren().add(targetIndex + i, childComponent);
            // Setting a component's ID to its own ID is necessary for MyFaces, who doesn't update the client-id
            // for the new location of the component in the tree. This action causes the client id to be nulled
            // and recreated when referenced.
            childComponent.setId(childComponent.getId());
        }
    }

    private static void moveAddFirst(UIComponent component, List<UIComponent> children) {
        for (var i = 0; i < children.size(); i++) {
            var childComponent = children.get(i);
            component.getChildren().add(i, childComponent);
            childComponent.setId(childComponent.getId());
        }
    }

    private static void moveAddLast(UIComponent component, List<UIComponent> children) {
        for (var childComponent : children) {
            component.getChildren().add(childComponent);
            childComponent.setId(childComponent.getId());
        }
    }

    private void moveFacet(UIComponent component, List<UIComponent> children) {
        for (var childComponent : children) {
            component.getFacets().put(getFacet(), childComponent);
            childComponent.setId(childComponent.getId());
        }
    }

    private static void moveBehavior(UIComponent component, String event, ClientBehavior behavior) {
        if (component instanceof ClientBehaviorHolder clientBehaviorHolder) {
            var behaviors = clientBehaviorHolder.getClientBehaviors().get(event);

            if (behaviors == null || !behaviors.contains(behavior)) { // Guard against adding ourselves twice
                clientBehaviorHolder.addClientBehavior(event, behavior);
            }
        }
    }

    private static void moveAfter(UIComponent component, List<UIComponent> children) {
        // Find the index the target component has among its siblings
        var targetComponentIndex = component.getParent().getChildren().indexOf(component);

        // The insertion point will be AFTER the target component
        var targetIndex = targetComponentIndex + 1;

        // If the target component was not the last sibling, add all the components that are to be moved
        // to the position after the target component, continuously shifting the subsequent siblings
        // to the right, otherwise they will be added as the last siblings
        for (var i = 0; i < children.size(); i++) {
            var childComponent = children.get(i);
            component.getParent().getChildren().add(targetIndex + i, childComponent);
            childComponent.setId(childComponent.getId());
        }
    }

    private List<UIComponent> getChildrenInNewList() {
        return new ArrayList<>(getChildren());
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns ID of the target component for which the component moving will be done.
     * @return ID of the target component for which the component moving will be done.
     */
      public String getFor() {
        return state.get("for");
    }

      /**
       * Sets ID of the target component for which the component moving will be done.
       * @param forValue ID of the target component for which the component moving will be done.
       */
    public void setFor(String forValue) {
        state.put("for", forValue);
    }

    /**
     * Returns the destination relative to the target component where the source component(s) are moved to. Default is <code>ADD_LAST</code>.
     * @return The destination relative to the target component where the source component(s) are moved to.
     */
    public Destination getDestination() {
        return state.get(destination, ADD_LAST);
    }

    /**
     * Sets the destination relative to the target component where the source component(s) are moved to. Valid values are
     * <ul>
     * <li> <code>BEFORE</code>    - Component is moved right before target component, i.e. as a sibling with an index that's 1 position lower
     * <li> <code>ADD_FIRST</code> - Component is added as the first child of the target component, any other children will have their index increased by 1
     * <li> <code>ADD_LAST</code>  - Component is added as the last child of the target component, any other children will stay at their original location
     * <li> <code>FACET</code>     - Component will be moved to the facet section of the target component under the name denoted by "facet"
     * <li> <code>BEHAVIOR</code>  - A Behavior will be moved to the behavior section of the target component
     * <li> <code>AFTER</code>     - Component is moved right after target component, i.e. as a sibling with an index that's 1 position higher
     * </ul>
     * @param destinationValue The destination relative to the target component where the source component(s) are moved to.
     */
    public void setDestination(Destination destinationValue) {
        state.put(destination, destinationValue);
    }

    /**
     * Returns in case the <code>destination</code> is set to FACET, the name of the facet in the target component to which the components should be moved.
     * @return In case the <code>destination</code> is set to FACET, the name of the facet in the target component to which the components should be moved.
     */
    public String getFacet() {
        return state.get(facet);
    }

    /**
     * Sets in case the <code>destination</code> is set to FACET, the name of the facet in the target component to which the components should be moved.
     * @param facetValue In case the <code>destination</code> is set to FACET, the name of the facet in the target component to which the components should be moved.
     */
    public void setFacet(String facetValue) {
        state.put(facet, facetValue);
    }

    /**
     * Returns in case the <code>destination</code> is set to BEHAVIOR, the name of the default event that the <b>target</b> component is 'supposed' to have.
     * @return In case the <code>destination</code> is set to BEHAVIOR, the name of the default event that the <b>target</b> component is 'supposed' to have.
     */
    public String getBehaviorDefaultEvent() {
        return state.get(behaviorDefaultEvent, "");
    }

    /**
     * Sets in case the <code>destination</code> is set to BEHAVIOR, the name of the default event that the <b>target</b> component is 'supposed' to have.
     * This normally does not need to be set, but might be needed for some over-eager tag handlers associated with a behavior that in advance try
     * to check whether the behavior event matches with what the component supports.
     * @param behaviorDefaultEventValue In case the <code>destination</code> is set to BEHAVIOR, the name of the default event that the <b>target</b> component is 'supposed' to have.
     */
    public void setBehaviorDefaultEvent(String behaviorDefaultEventValue) {
        state.put(behaviorDefaultEvent, behaviorDefaultEventValue);
    }

    /**
     * Returns in case the <code>destination</code> is set to BEHAVIOR, the comma separated list events that the <b>target</b> component is 'supposed' to support.
     * @return In case the <code>destination</code> is set to BEHAVIOR, the comma separated list events that the <b>target</b> component is 'supposed' to support.
     */
    public String getBehaviorEvents() {
        return state.get(behaviorEvents);
    }

    /**
     * Sets in case the <code>destination</code> is set to BEHAVIOR, the comma separated list events that the <b>target</b> component is 'supposed' to support.
     * This normally does not need to be set, but might be needed for some over-eager tag handlers associated with a behavior that in advance try
     * to check whether the behavior event matches with what the component supports.
     * @param behaviorEventsValue In case the <code>destination</code> is set to BEHAVIOR, the comma separated list events that the <b>target</b> component is 'supposed' to support.
     */
    public void setBehaviorEvents(String behaviorEventsValue) {
        state.put(behaviorEvents, behaviorEventsValue);
    }

}