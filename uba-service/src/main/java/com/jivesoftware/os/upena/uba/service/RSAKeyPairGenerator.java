package com.jivesoftware.os.upena.uba.service;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author jonathan.colt
 */
public class RSAKeyPairGenerator {

    void create(String alias, String password, File keystore, File publicKeyFile) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        storeKeyAndCertificateChain(alias, password.toCharArray(), keystore, keyPair.getPrivate(), new X509Certificate[0]);

        Files.write(Base64.encodeBase64String(keyPair.getPublic().getEncoded()).getBytes(StandardCharsets.UTF_8), publicKeyFile);
    }

    private void storeKeyAndCertificateChain(String alias, char[] password, File keystore, Key key, X509Certificate[] chain) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null, null);
        keyStore.setKeyEntry(alias, key, password, chain);
        keyStore.store(new FileOutputStream(keystore), password);

    }

    String getPublicKey(String alias, String password, File keystoreFile, File publicKeyFile) throws Exception {
        String privateKey = getPrivateKey(alias, password, keystoreFile);
        if (privateKey != null) {
            return Files.toString(publicKeyFile, StandardCharsets.UTF_8);
        }
        return null;
    }

    String getPrivateKey(String alias, String password, File keystoreFile) throws Exception {
        //Reload the keystore
        char[] passwordChars = password.toCharArray();
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(new FileInputStream(keystoreFile), passwordChars);

        Key key = keyStore.getKey(alias + "-public", passwordChars);
        if (key != null) {
            return Base64.encodeBase64String(key.getEncoded());
        }
        return null;
    }

}
