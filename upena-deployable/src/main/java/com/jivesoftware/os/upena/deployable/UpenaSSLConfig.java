package com.jivesoftware.os.upena.deployable;

import com.jivesoftware.os.routing.bird.http.client.OAuthSigner;

/**
 *
 * @author jonathan.colt
 */
public class UpenaSSLConfig {

    public final boolean sslEnable;
    public final boolean allowSelfSignedCerts;
    public final OAuthSigner signer;

    public UpenaSSLConfig(boolean sslEnable, boolean allowSelfSignedCerts, OAuthSigner signer) {
        this.sslEnable = sslEnable;
        this.allowSelfSignedCerts = allowSelfSignedCerts;
        this.signer = signer;
    }

}
