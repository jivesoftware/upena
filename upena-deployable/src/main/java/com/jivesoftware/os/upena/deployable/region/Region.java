package com.jivesoftware.os.upena.deployable.region;

public interface Region<I> {

    String render(String user, I input);
}
