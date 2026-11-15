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
package org.omnifaces.cdi;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.util.List;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.junit.jupiter.api.Test;

/**
 * A bean storage is serialized into the HTTP session, and with <code>&#64;ViewScoped(saveInViewState = true)</code>
 * into the Faces view state. Only its beans are worth serializing. Its locks guard only the instance in which they are
 * created, so a deserialized bean storage must create its own.
 */
class BeanStorageSerializationTest {

    @Test
    void serializedFieldsAreOnlyTheBeans() {
        var fields = stream(ObjectStreamClass.lookup(BeanStorage.class).getFields()).map(ObjectStreamField::getName).collect(toList());

        assertEquals(List.of("beans"), fields);
    }

    @Test
    void deserializedBeanStorageIsFullyUsable() throws Exception {
        var existingBean = new StringBean("existing");
        var newBean = new StringBean("new");
        var beanStorage = new BeanStorage(1);
        beanStorage.getBean(existingBean, null);

        var deserialized = deserialize(serialize(beanStorage));

        assertEquals("existing", deserialized.getBean(existingBean), "existing bean is kept");
        assertEquals("new", deserialized.getBean(newBean, null), "new bean is created");
        assertTrue(deserialized.acquire(), "acquirable");
        deserialized.release();
    }

    private static byte[] serialize(BeanStorage beanStorage) throws Exception {
        var bytes = new ByteArrayOutputStream();

        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(beanStorage);
        }

        return bytes.toByteArray();
    }

    private static BeanStorage deserialize(byte[] bytes) throws Exception {
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (BeanStorage) input.readObject();
        }
    }

    private static class StringBean implements Contextual<String>, PassivationCapable {

        private final String value;

        StringBean(String value) {
            this.value = value;
        }

        @Override
        public String getId() {
            return value;
        }

        @Override
        public String create(CreationalContext<String> context) {
            return value;
        }

        @Override
        public void destroy(String instance, CreationalContext<String> context) {
            // NOOP.
        }
    }

}
