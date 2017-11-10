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
package org.omnifaces.test.model.tree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.omnifaces.model.tree.ListTreeModel;
import org.omnifaces.model.tree.TreeModel;

public class TestTreeModel {

	@Test
	public void testTree() {
		TreeModel<String> tree = new ListTreeModel<>();
		TreeModel<String> one = tree.addChild("One");
		TreeModel<String> two = one.addChild("Two");
		TreeModel<String> three = two.getParent().addChild("Three");
		TreeModel<String> four = three.getParent().getParent().addChild("Four");
		TreeModel<String> five = four.addChild("Five");
		assertEquals("[One[Two, Three], Four[Five]]", tree.toString());

		three.remove();
		assertEquals("[One[Two], Four[Five]]", tree.toString());

		four.remove();
		assertEquals("[One[Two]]", tree.toString());

		one.addChildNode(three);
		assertEquals("[One[Two, Three]]", tree.toString());

		TreeModel<String> copy = new ListTreeModel<>();
		copy.addChild("One").addChild("Two").getParent().addChild("Three");
		assertEquals("[One[Two, Three]]", copy.toString());
		assertTrue(tree + " equals " + copy, tree.equals(copy));

		TreeModel<String> copyOne = copy.getChildren().iterator().next();
		assertEquals("One[Two, Three]", one.toString());
		assertEquals("One[Two, Three]", copyOne.toString());
		assertTrue(one + " equals " + copyOne, one.equals(copyOne));

		copy.setData("Copy");
		assertEquals("One[Two, Three]", one.toString());
		assertEquals("One[Two, Three]", copyOne.toString());
		assertTrue(one + " equals not " + copyOne, !one.equals(copyOne));
	}

}
