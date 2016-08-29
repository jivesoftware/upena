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
package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProvider;
import com.jivesoftware.os.upena.service.UpenaTable.UpenaKeyProvider;
import com.jivesoftware.os.upena.shared.LB;
import com.jivesoftware.os.upena.shared.LBKey;

public class LBKeyProvider implements UpenaKeyProvider<LBKey, LB> {

    private final OrderIdProvider idProvider;

    public LBKeyProvider(OrderIdProvider idProvider) {
        this.idProvider = idProvider;
    }


    @Override
    public LBKey getNodeKey(UpenaTable<LBKey, LB> table, LB value) {
        String k = Long.toString(Math.abs(idProvider.nextId()));
        return new LBKey(k);
    }
}
