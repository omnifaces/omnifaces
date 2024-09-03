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
package org.omnifaces.model.tree;

import static org.omnifaces.util.Reflection.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A base implementation of {@link TreeModel}. Implementors basically only need to implement {@link #createChildren()}
 * wherein a concrete instance of the desired underlying {@link Collection} is returned.
 *
 * @author Bauke Scholtz
 * @param <T> The type of the wrapped data of the tree node.
 * @since 1.7
 * @see ListTreeModel
 * @see SortedTreeModel
 */
public abstract class AbstractTreeModel<T> implements TreeModel<T> {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    // Properties -----------------------------------------------------------------------------------------------------

    private final ReentrantLock lock = new ReentrantLock();

    private T data;
    private AbstractTreeModel<T> parent;
    private Collection<TreeModel<T>> children;
    private List<TreeModel<T>> unmodifiableChildren = Collections.emptyList();
    private int index;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Returns a concrete (and usually empty) {@link Collection} instance which should hold the tree's children.
     * @return A concrete (and usually empty) {@link Collection} instance which should hold the tree's children.
     */
    protected abstract Collection<TreeModel<T>> createChildren();

    // Mutators -------------------------------------------------------------------------------------------------------

    @Override
    public void setData(T data) {
        this.data = data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeModel<T> addChild(T data) {
        AbstractTreeModel<T> child = instance(getClass());
        child.data = data;
        return addChildNode(child);
    }

    @Override
    public TreeModel<T> addChildNode(TreeModel<T> child) {
        if (child == null || child.getClass() != getClass()) {
            throw new IllegalArgumentException();
        }

        if (children == null) {
            children = createChildren();
        }

        ((AbstractTreeModel<T>) child).parent = this;
        ((AbstractTreeModel<T>) child).index = children.size();
        children.add(child);
        return child;
    }

    @Override
    public TreeModel<T> remove() {
        if (!isRoot()) {

            execAtomic(lock, () -> {
                parent.children.remove(this);

                // Fix the indexes of the children (that's why it needs to be synchronized).
                int newIndex = 0;
                for (TreeModel<T> child : parent.children) {
                    ((AbstractTreeModel<T>) child).index = newIndex;
                    newIndex++;
                }
            });
        }

        return parent;
    }

    // Accessors ------------------------------------------------------------------------------------------------------

    @Override
    public T getData() {
        return data;
    }

    @Override
    public TreeModel<T> getParent() {
        return parent;
    }

    @Override
    public TreeModel<T> getNextSibling() {
        return getNextSibling(parent, index + 1);
    }

    /**
     * Recursive helper method for {@link #getNextSibling()}.
     */
    private TreeModel<T> getNextSibling(TreeModel<T> parent, int index) {
        if (parent == null) {
            return null;
        }
        else if (index < parent.getChildCount()) {
            return parent.getChildren().get(index);
        }
        else {
            TreeModel<T> nextParent = parent.getNextSibling();
            return getNextSibling(nextParent, 0);
        }
    }

    @Override
    public TreeModel<T> getPreviousSibling() {
        return getPreviousSibling(parent, index - 1);
    }

    /**
     * Recursive helper method for {@link #getPreviousSibling()}.
     */
    private TreeModel<T> getPreviousSibling(TreeModel<T> parent, int index) {
        if (parent == null) {
            return null;
        }
        else if (index >= 0) {
            return parent.getChildren().get(index);
        }
        else {
            TreeModel<T> previousParent = parent.getPreviousSibling();
            return getPreviousSibling(previousParent, (previousParent != null ? previousParent.getChildCount() : 0) - 1);
        }
    }

    @Override
    public int getChildCount() {
        return children == null ? 0 : children.size();
    }

    @Override
    public List<TreeModel<T>> getChildren() {
        if (unmodifiableChildren.size() != getChildCount()) {
            unmodifiableChildren = Collections.unmodifiableList((children instanceof List)
                ? (List<TreeModel<T>>) children : new ArrayList<>(children));
        }

        return unmodifiableChildren;
    }

    @Override
    public Iterator<TreeModel<T>> iterator() {
        return getChildren().iterator();
    }

    @Override
    public int getLevel() {
        return isRoot() ? 0 : parent.getLevel() + 1;
    }

    @Override
    public String getIndex() {
        return isRoot() ? null : parent.getParentIndex() + index;
    }

    private String getParentIndex() {
        return isRoot() ? "" : getIndex() + "_";
    }

    // Checkers -------------------------------------------------------------------------------------------------------

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    @Override
    public boolean isFirst() {
        return !isRoot() && index == 0;
    }

    @Override
    public boolean isLast() {
        return !isRoot() && index + 1 == parent.getChildCount();
    }

    // Object overrides -----------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object == null || object.getClass() != getClass()) {
            return false;
        }

        return equals(this, (AbstractTreeModel<?>) object, false) && equals(getRoot(this), getRoot((AbstractTreeModel<?>) object), true);
    }

    private static AbstractTreeModel<?> getRoot(AbstractTreeModel<?> node) {
        TreeModel<?> root = node;

        while (root.getParent() != null) {
            root = root.getParent();
        }

        return (AbstractTreeModel<?>) root;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean equals(AbstractTreeModel thiz, AbstractTreeModel other, boolean recurse) {
        if (thiz == other) {
            return true;
        }

        if (!Objects.equals(thiz.data, other.data)) {
            return false;
        }

        if (recurse && thiz.children != null) {
            if (thiz.getChildCount() != other.getChildCount()) {
                return false;
            }

            Iterator<AbstractTreeModel> thisChildren = thiz.children.iterator();
            Iterator<AbstractTreeModel> otherChildren = other.children.iterator();

            while (thisChildren.hasNext() && otherChildren.hasNext()) {
                if (!equals(thisChildren.next(), otherChildren.next(), true)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, children);
    }

    @Override
    public String toString() {
        return (data == null ? "" : data) + "" + (children == null ? "" : children);
    }

    // Concurrency ----------------------------------------------------------------------------------------------------

    /**
     * A {@link FunctionalInterface} to be used with {@link #execAtomic(Lock, Action)}
     * @since 4.6
     */
    @FunctionalInterface
    public interface Action {
        void execute() throws Exception;
    }

    /**
     * Execute the passed task and return the computed result atomically using the passed lock.
     * @param lock The {@link Lock} to be used for atomic execution
     * @param task The {@link FunctionalInterface} to be executed atomically
     * @since 4.6
     */
    public static void execAtomic(Lock lock, Action task) {
        lock.lock();

        try {
            task.execute();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            lock.unlock();
        }
    }

}