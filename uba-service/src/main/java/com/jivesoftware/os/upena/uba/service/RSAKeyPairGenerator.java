package com.jivesoftware.os.upena.uba.service;

import com.google.common.io.Files;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.apache.commons.codec.binary.Base64;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 *
 * @author jonathan.colt
 */
public class RSAKeyPairGenerator {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    void create(String alias, String password, File keystore, File publicKeyFile) throws Exception {

        LOG.info("Creating {} {} {}", alias, keystore, publicKeyFile);

        CertAndKeyGen keyGen = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
        keyGen.generate(1024);
        X509Certificate rootCertificate = keyGen.getSelfCertificate(new X500Name("CN=ROOT"), (long) 365 * 24 * 60 * 60);
        rootCertificate = createSignedCertificate(rootCertificate, rootCertificate, keyGen.getPrivateKey());
        X509Certificate[] chain = {rootCertificate};

        storeKeyAndCertificateChain(alias, password.toCharArray(), keystore, keyGen.getPrivateKey(), chain);

        Files.write(Base64.encodeBase64String(keyGen.getPublicKey().getEncoded()).getBytes(StandardCharsets.UTF_8), publicKeyFile);
    }

    private X509Certificate createSignedCertificate(X509Certificate cetrificate,
        X509Certificate issuerCertificate,
        PrivateKey issuerPrivateKey) throws Exception {

        Principal issuer = issuerCertificate.getSubjectDN();
        String issuerSigAlg = issuerCertificate.getSigAlgName();

        byte[] inCertBytes = cetrificate.getTBSCertificate();
        X509CertInfo info = new X509CertInfo(inCertBytes);
        info.set(X509CertInfo.ISSUER, issuer);

        //No need to add the BasicContraint for leaf cert
        if (!cetrificate.getSubjectDN().getName().equals("CN=TOP")) {
            CertificateExtensions exts = new CertificateExtensions();
            BasicConstraintsExtension bce = new BasicConstraintsExtension(true, -1);
            exts.set(BasicConstraintsExtension.NAME, new BasicConstraintsExtension(false, bce.getExtensionValue()));
            info.set(X509CertInfo.EXTENSIONS, exts);
        }

        X509CertImpl outCert = new X509CertImpl(info);
        outCert.sign(issuerPrivateKey, issuerSigAlg);

        return outCert;
    }

    private void storeKeyAndCertificateChain(String alias, char[] password, File keystore, Key key, X509Certificate[] chain) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null, null);
        keyStore.setKeyEntry(alias, key, password, chain);
        keyStore.store(new FileOutputStream(keystore), password);

    }

    String getPublicKey(String alias, String password, File keystoreFile, File publicKeyFile) throws Exception {
        if (keystoreFile == null || !keystoreFile.exists() || publicKeyFile == null || !publicKeyFile.exists()) {
            return null;
        }

        LOG.info("getPublicKey {} {} {}", alias, keystoreFile, publicKeyFile);

        String privateKey = getPrivateKey(alias, password, keystoreFile);
        if (privateKey != null) {
            return Files.toString(publicKeyFile, StandardCharsets.UTF_8);
        }
        return null;
    }

    String getPrivateKey(String alias, String password, File keystoreFile) throws Exception {

        if (keystoreFile == null || !keystoreFile.exists()) {
            return null;
        }

        LOG.info("getPrivateKey {} {}", alias, keystoreFile);

        //Reload the keystore
        char[] passwordChars = password.toCharArray();
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(new FileInputStream(keystoreFile), passwordChars);

        Key key = keyStore.getKey(alias, passwordChars);
        if (key != null) {
            return Base64.encodeBase64String(key.getEncoded());
        }
        return null;
    }

}
