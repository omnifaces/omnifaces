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
package org.omnifaces.test.taghandler.validateuniquecolumn;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named
@RequestScoped
public class ValidateUniqueColumnITBean {

    private List<Item> allVisibleItems;
    private List<Item> paginatedItems;
    private List<Item> partiallyExecutedItems;
    private List<Item> itemsBesideOtherInput;
    private List<Item> nonAjaxItems;
    private List<Item> inputAjaxItems;
    private List<Item> formExecutedItems;
    private String otherInput;

    @PostConstruct
    public void init() {
        allVisibleItems = createItems("A", "B", "C");
        paginatedItems = createItems("A", "B", "C", "D");
        partiallyExecutedItems = createItems("A", "B", "AC");
        itemsBesideOtherInput = createItems("A", "B", "C");
        nonAjaxItems = createItems("A", "B", "C");
        inputAjaxItems = createItems("A", "B", "AC");
        formExecutedItems = createItems("A", "B", "AC");
    }

    private static List<Item> createItems(String... values) {
        List<Item> items = new ArrayList<>(values.length);

        for (String value : values) {
            items.add(new Item(value));
        }

        return items;
    }

    public void action() {
        // NOOP
    }

    public List<Item> getAllVisibleItems() {
        return allVisibleItems;
    }

    public List<Item> getPaginatedItems() {
        return paginatedItems;
    }

    public List<Item> getPartiallyExecutedItems() {
        return partiallyExecutedItems;
    }

    public List<Item> getItemsBesideOtherInput() {
        return itemsBesideOtherInput;
    }

    public List<Item> getNonAjaxItems() {
        return nonAjaxItems;
    }

    public List<Item> getFormExecutedItems() {
        return formExecutedItems;
    }

    public List<Item> getInputAjaxItems() {
        return inputAjaxItems;
    }

    public String getOtherInput() {
        return otherInput;
    }

    public void setOtherInput(String otherInput) {
        this.otherInput = otherInput;
    }

    public static class Item {

        private String value;

        public Item(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

}
