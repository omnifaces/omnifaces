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
package org.omnifaces.test.component.tree;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.omnifaces.model.tree.ListTreeModel;
import org.omnifaces.model.tree.TreeModel;

@Named
@ViewScoped
public class TreeITBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private TreeModel<Node> model;

    @PostConstruct
    public void init() {
        model = new ListTreeModel<>();
        var a = model.addChild(new Node("A"));
        a.addChild(new Node("A1"));
        a.addChild(new Node("A2"));
        var b = model.addChild(new Node("B"));
        b.addChild(new Node("B1"));
    }

    public TreeModel<Node> getModel() {
        return model;
    }

    public static class Node implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;
        private String value;

        public Node(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

}
