package com.jivesoftware.os.upena.deployable.okta.client.models.links;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = LinksUnionDeserializer.class)
@JsonSubTypes({
        @JsonSubTypes.Type(Link.class),
        @JsonSubTypes.Type(Links.class)
})
public interface LinksUnion { }

