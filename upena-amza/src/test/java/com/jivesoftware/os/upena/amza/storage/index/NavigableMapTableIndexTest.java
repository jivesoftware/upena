/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.amza.storage.index;

import com.jivesoftware.os.upena.amza.shared.MemoryRowsIndex;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.RowIndexValue;
import com.jivesoftware.os.upena.amza.storage.filer.UIO;
import java.util.AbstractMap;
import java.util.Collections;
import org.testng.annotations.Test;

public class NavigableMapTableIndexTest {

    @Test(enabled = false)
    public void loadTest() throws Exception {
        System.out.println("InitialHeap:" + Runtime.getRuntime().totalMemory());

        MemoryRowsIndex instance = new MemoryRowsIndex();

        for (long i = 0; i < 200000000; i++) {
            instance.put(Collections.singletonList(new AbstractMap.SimpleEntry<>(
                new RowIndexKey(UIO.longBytes(i)),
                new RowIndexValue(UIO.longBytes(i), i, false))));
            if (i % 1000000 == 0) {
                System.out.println("Size:" + i + " Heap:" + Runtime.getRuntime().totalMemory());
                instance.commit();
            }
        }
        instance.commit();

        System.out.println("Heap:" + Runtime.getRuntime().totalMemory());
        // TODO review the generated test code and remove the default call to fail.
    }
}
