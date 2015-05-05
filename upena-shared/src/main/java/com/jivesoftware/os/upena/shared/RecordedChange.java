package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author jonathan.colt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordedChange {

    public final String who;
    public final String what;
    public final long when;
    public final String where;
    public final String why;
    public final String how;

    @JsonCreator
    public RecordedChange(@JsonProperty("who") String who,
        @JsonProperty("what") String what,
        @JsonProperty("when") long when,
        @JsonProperty("where") String where,
        @JsonProperty("why") String why,
        @JsonProperty("how") String how) {
        this.who = who;
        this.what = what;
        this.when = when;
        this.where = where;
        this.why = why;
        this.how = how;
    }

}
