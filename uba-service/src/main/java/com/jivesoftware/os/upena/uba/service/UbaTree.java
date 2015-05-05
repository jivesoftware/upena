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
package com.jivesoftware.os.upena.uba.service;

import java.io.File;
import java.util.ArrayList;

public class UbaTree {

    private final File root;
    private final String[] treeLevels;

    public UbaTree(File root, String[] treeLevels) {
        if (root == null) {
            throw new IllegalArgumentException("root of the conductor tree cannot be null.");
        }
        this.root = root;
        this.treeLevels = treeLevels;
    }

    public File getRoot() {
        return root;
    }

    public static interface ConductorPathCallback {

        void conductorPath(NameAndKey[] path);
    }

    public void build(ConductorPathCallback orchestraPathCallback) {
        walkTree(new ArrayList<NameAndKey>(), treeLevels, 0, root, orchestraPathCallback);
    }

    void walkTree(ArrayList<NameAndKey> path, String[] levelNames, int depth, File directory, ConductorPathCallback orchestraPathCallback) {
        File[] listFiles = directory.listFiles();
        if (listFiles != null) {
            for (File folder : listFiles) {
                NameAndKey nameAndKey = buildNameAndKey(folder);
                if (nameAndKey == null) {
                    continue;
                }
                path.add(nameAndKey);
                if (depth + 1 == levelNames.length) {
                    orchestraPathCallback.conductorPath(path.toArray(new NameAndKey[path.size()]));
                } else {
                    walkTree(path, levelNames, depth + 1, folder, orchestraPathCallback);
                }
                path.remove(path.size() - 1);
            }
        }
    }

    NameAndKey buildNameAndKey(File file) {
        if (!file.isDirectory()) {
            return null;
        }
        String name = file.getName();
        int i = name.indexOf('_');
        if (i == -1) {
            return null;
        }
        return new NameAndKey(name.substring(0, i), name.substring(i + 1));
    }
}
