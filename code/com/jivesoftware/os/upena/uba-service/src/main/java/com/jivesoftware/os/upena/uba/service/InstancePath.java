package com.jivesoftware.os.upena.uba.service;

import java.io.File;
import java.util.Arrays;

public class InstancePath {

    private final File root;
    private final NameAndKey[] path;

    public InstancePath(File root, NameAndKey[] path) {
        this.root = root;
        this.path = path;
    }

    public String toHumanReadableName() {
        StringBuilder key = new StringBuilder();
        for (NameAndKey p : path) {
            if (key.length() > 0) {
                key.append(" ");
            }
            key.append(p.name);
        }
        return key.toString();
    }

    File script(String scriptName) {
        return new File(path(path, -1), "bin/" + scriptName);
    }

    File serviceRoot() {
        return path(path, -1);
    }

    File artifactRoot() {
        return path(path, path.length - 1);
    }

    File artifactFile(String ext) {
        return new File(path(path, path.length - 1), path[path.length - 1].key + ext);
    }

    File path(NameAndKey[] path, int length) {
        if (length == -1 || length > path.length) {
            length = path.length;
        }
        StringBuilder fsPath = new StringBuilder();
        for (int i = 0; i < length; i++) {
            NameAndKey p = path[i];
            if (fsPath.length() > 0) {
                fsPath.append(File.separator);
            }
            fsPath.append(p.toStringForm());
        }
        File folder = new File(root, fsPath.toString());
        folder.mkdirs();
        return folder;
    }

    @Override
    public String toString() {
        return "InstancePath{" + "root=" + root + ", path=" + Arrays.deepToString(path) + '}';
    }
}