/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.database.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.firebase.database.snapshot.Node;
import com.google.firebase.database.snapshot.NodeUtilities;
import com.google.firebase.database.utilities.Clock;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ServerValuesTest {

  private static final Clock CLOCK = () -> 42L;

  private static Map<String, Object> serverValues() {
    return ServerValues.generateServerValues(CLOCK);
  }

  private static Map<String, Object> valueWithServerValue(Object serverValue) {
    Map<String, Object> value = new HashMap<>();
    value.put(ServerValues.NAME_SUBKEY_SERVERVALUE, serverValue);
    return value;
  }

  @Test
  public void resolveDeferredValueResolvesTimestamp() {
    Map<String, Object> value = valueWithServerValue("timestamp");
    assertEquals(42L, ServerValues.resolveDeferredValue(value, serverValues()));
  }

  @Test
  public void resolveDeferredValueLeavesCompoundServerValueAlone() {
    Map<String, Object> increment = new HashMap<>();
    increment.put("increment", 5L);

    Map<String, Object> value = valueWithServerValue(increment);
    assertEquals(value, ServerValues.resolveDeferredValue(value, serverValues()));
  }

  @Test
  public void resolveDeferredValueLeavesUnknownServerValueAlone() {
    Map<String, Object> value = valueWithServerValue("somethingElse");
    assertEquals(value, ServerValues.resolveDeferredValue(value, serverValues()));
  }

  @Test
  public void resolveDeferredValueSnapshotWithCompoundServerValue() {
    Map<String, Object> increment = new HashMap<>();
    increment.put("increment", 5L);

    Node node = NodeUtilities.NodeFromJSON(valueWithServerValue(increment));
    Node resolved = ServerValues.resolveDeferredValueSnapshot(node, serverValues());

    assertFalse(resolved.isEmpty());
    assertEquals(node, resolved);
  }

  @Test
  public void resolveDeferredValueSnapshotWithTimestamp() {
    Node node = NodeUtilities.NodeFromJSON(valueWithServerValue("timestamp"));
    Node resolved = ServerValues.resolveDeferredValueSnapshot(node, serverValues());

    assertTrue(resolved.isLeafNode());
    assertEquals(42L, resolved.getValue());
  }

  @Test
  public void resolveDeferredValueSnapshotWalksChildren() {
    Map<String, Object> value = new HashMap<>();
    value.put("updatedAt", valueWithServerValue("timestamp"));
    value.put("keep", "as-is");

    Node node = NodeUtilities.NodeFromJSON(value);
    Node resolved = ServerValues.resolveDeferredValueSnapshot(node, serverValues());

    assertEquals(42L, resolved.getChild(new Path("updatedAt")).getValue());
    assertEquals("as-is", resolved.getChild(new Path("keep")).getValue());
  }
}
