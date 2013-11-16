package com.jivesoftware.os.upena.routing.shared;

class TimestampedClient<C> {

    private final long timestamp;
    private final C client;

    public TimestampedClient(long timestamp, C client) {
        this.timestamp = timestamp;
        this.client = client;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public C getClient() {
        return client;
    }
}
