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
package com.jivesoftware.os.upena.amza.shared;

public interface RowScanable {

    <E extends Exception> void rowScan(RowScan<E> rowScan) throws E;

    /**

    @param <E>
    @param from inclusive
    @param to exclusize
    @param rowScan
    @throws E
    */
    <E extends Exception> void rangeScan(RowIndexKey from, RowIndexKey to, RowScan<E> rowScan) throws E;

}
