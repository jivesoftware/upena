package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Service implements Stored<Service> {

    public final String name;
    public final String description;

    @JsonCreator
    public Service(@JsonProperty("name") String name,
            @JsonProperty("description") String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Service{"
                + "name=" + name
                + ", description=" + description
                + '}';
    }

    @Override
    public int compareTo(Service o) {
        return name.compareTo(o.name);
    }
}
