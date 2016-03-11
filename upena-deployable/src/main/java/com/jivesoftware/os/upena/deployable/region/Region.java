package com.jivesoftware.os.upena.deployable.region;

public interface Region<I extends PluginInput> {

    String render(String user, I input) throws Exception;
}
