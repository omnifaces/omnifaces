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
package org.omnifaces.test.util;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.util.ResourcePaths.addLeadingSlashIfNecessary;
import static org.omnifaces.util.ResourcePaths.addTrailingSlashIfNecessary;
import static org.omnifaces.util.ResourcePaths.concat;
import static org.omnifaces.util.ResourcePaths.filterExtension;
import static org.omnifaces.util.ResourcePaths.getExtension;
import static org.omnifaces.util.ResourcePaths.getParent;
import static org.omnifaces.util.ResourcePaths.isDirectory;
import static org.omnifaces.util.ResourcePaths.isExtensionless;
import static org.omnifaces.util.ResourcePaths.isRoot;
import static org.omnifaces.util.ResourcePaths.stripExtension;
import static org.omnifaces.util.ResourcePaths.stripPrefixPath;
import static org.omnifaces.util.ResourcePaths.stripTrailingSlash;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ResourcePathsTest {

    @Test
    void testIsDirectory() {
        assertTrue(isDirectory("/"));
        assertTrue(isDirectory("/foo/"));
        assertTrue(isDirectory("/foo/bar/"));
        assertTrue(isDirectory("foo/"));
        assertFalse(isDirectory("/foo"));
        assertFalse(isDirectory("/foo/bar"));
        assertFalse(isDirectory("foo.xhtml"));
        assertFalse(isDirectory(""));
    }

    @Test
    void testStripPrefixPath() {
        assertEquals("/bar", stripPrefixPath("/foo/", "/foo/bar"));
        assertEquals("/bar/baz", stripPrefixPath("/foo/", "/foo/bar/baz"));
        assertEquals("/", stripPrefixPath("/foo/", "/foo/"));
        assertEquals("/other/bar", stripPrefixPath("/foo/", "/other/bar"));
        assertEquals("", stripPrefixPath("/foo/", ""));
    }

    @Test
    void testStripTrailingSlash() {
        assertEquals("/foo", stripTrailingSlash("/foo"));
        assertEquals("/foo", stripTrailingSlash("/foo/"));
        assertEquals("/foo", stripTrailingSlash("/foo//"));
        assertEquals("/foo", stripTrailingSlash("/foo///"));
        assertEquals("/foo/bar", stripTrailingSlash("/foo/bar/"));
        assertEquals("", stripTrailingSlash("/"));
        assertEquals("", stripTrailingSlash(""));
    }

    @Test
    void testStripExtension() {
        assertEquals("index", stripExtension("index.xhtml"));
        assertEquals("/foo/index", stripExtension("/foo/index.xhtml"));
        assertEquals("archive.tar", stripExtension("archive.tar.gz"));
        assertEquals("noext", stripExtension("noext"));
        assertEquals("/foo/noext", stripExtension("/foo/noext"));
        assertEquals("/foo.bar/baz", stripExtension("/foo.bar/baz"));
    }

    @Test
    void testGetExtension() {
        assertEquals(".xhtml", getExtension("index.xhtml"));
        assertEquals(".xhtml", getExtension("/foo/index.xhtml"));
        assertEquals(".gz", getExtension("archive.tar.gz"));
        assertNull(getExtension("noext"));
        assertNull(getExtension("/foo/noext"));
        assertNull(getExtension("/foo.bar/baz"));
    }

    @Test
    void testIsExtensionless() {
        assertTrue(isExtensionless("noext"));
        assertTrue(isExtensionless("/foo/noext"));
        assertTrue(isExtensionless("/foo.bar/baz"));
        assertTrue(isExtensionless("/"));
        assertTrue(isExtensionless(""));
        assertTrue(isExtensionless("/foo.bar/"));
        assertFalse(isExtensionless("index.xhtml"));
        assertFalse(isExtensionless("/foo.bar/baz.xhtml"));
        assertFalse(isExtensionless("/foo/index.xhtml"));
        assertFalse(isExtensionless("archive.tar.gz"));
    }

    @Test
    void testFilterExtension() {
        Set<String> input = new HashSet<>(asList("/a", "/b.xhtml", "/foo/c", "/foo/d.xhtml", "/foo.bar/baz"));
        Set<String> expected = new HashSet<>(asList("/a", "/foo/c", "/foo.bar/baz"));
        assertEquals(expected, filterExtension(input));

        assertTrue(filterExtension(new HashSet<>()).isEmpty());
        assertTrue(filterExtension(new HashSet<>(asList("a.xhtml", "b.xhtml"))).isEmpty());
    }

    @Test
    void testIsRoot() {
        assertTrue(isRoot("/"));
        assertFalse(isRoot(""));
        assertFalse(isRoot("//"));
        assertFalse(isRoot("/foo"));
        assertFalse(isRoot("/foo/"));
    }

    @Test
    void testAddLeadingSlashIfNecessary() {
        assertEquals("/foo", addLeadingSlashIfNecessary("foo"));
        assertEquals("/foo", addLeadingSlashIfNecessary("/foo"));
        assertEquals("/foo/bar", addLeadingSlashIfNecessary("foo/bar"));
        assertEquals("/foo/bar", addLeadingSlashIfNecessary("/foo/bar"));
        assertEquals("/", addLeadingSlashIfNecessary("/"));
        assertEquals("/", addLeadingSlashIfNecessary(""));
    }

    @Test
    void testAddTrailingSlashIfNecessary() {
        assertEquals("foo/", addTrailingSlashIfNecessary("foo"));
        assertEquals("foo/", addTrailingSlashIfNecessary("foo/"));
        assertEquals("/foo/bar/", addTrailingSlashIfNecessary("/foo/bar"));
        assertEquals("/foo/bar/", addTrailingSlashIfNecessary("/foo/bar/"));
        assertEquals("/", addTrailingSlashIfNecessary("/"));
        assertEquals("/", addTrailingSlashIfNecessary(""));
    }

    @Test
    void testConcat() {
        assertEquals("foo/bar", concat("foo", "bar"));
        assertEquals("foo/bar/baz", concat("foo", "bar", "baz"));
        assertEquals("/foo/bar", concat("", "foo", "bar"));
        assertEquals("", concat());
        assertEquals("foo", concat("foo"));
    }

    @Test
    void testGetParent() {
        // Typical URL paths — must work identically on Windows and Linux.
        assertEquals("/member", getParent("/member/cbc"));
        assertEquals("/a/b", getParent("/a/b/c"));
        assertEquals("/foo", getParent("/foo/bar.xhtml"));

        // Single-segment absolute paths, root and empty: all return empty string (no meaningful parent).
        assertEquals("", getParent("/member"));
        assertEquals("", getParent("/foo"));
        assertEquals("", getParent("/"));
        assertEquals("", getParent(""));

        // Relative paths (no leading slash).
        assertEquals("foo", getParent("foo/bar"));
        assertEquals("", getParent("foo"));

        // Trailing slash: last segment is empty, so parent is the path without the trailing slash.
        assertEquals("/foo/bar", getParent("/foo/bar/"));
    }

}
