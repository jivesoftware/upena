package com.jivesoftware.os.upena.uba.service;

public class NameAndKey {

    public final String name;
    public final String key;

    public NameAndKey(String name, String key) {
        this.name = name;
        this.key = key;
    }

    public String toStringForm() {
        return name + "_" + key;
    }

    @Override
    public String toString() {
        return toStringForm();
    }
}
