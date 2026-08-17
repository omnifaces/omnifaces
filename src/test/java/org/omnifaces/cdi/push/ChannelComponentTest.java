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
package org.omnifaces.cdi.push;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifies that the channel validation messages name the offending tag and value instead of emitting the raw format specifiers.
 */
class ChannelComponentTest {

    private static class TestChannel extends ChannelComponent {
        // NOOP.
    }

    @Test
    void invalidChannelNameIsReportedWithTagNameAndValue() {
        var component = new TestChannel();
        var thrown = assertThrows(IllegalArgumentException.class, () -> component.validateChannel(null, "invalid name!"));
        var message = thrown.getMessage();

        assertAll(
            () -> assertTrue(message.contains("o:testchannel"), message),
            () -> assertTrue(message.contains("invalid name!"), message),
            () -> assertFalse(message.contains("%s"), message)
        );
    }

}
