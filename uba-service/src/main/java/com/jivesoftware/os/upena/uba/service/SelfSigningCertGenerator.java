package com.jivesoftware.os.upena.uba.service;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
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
public class SelfSigningCertGenerator {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public boolean validate(String alias, String password, File keystoreFile) throws Exception {

        LOG.info("Validating {} {}", alias, keystoreFile);

        //Reload the keystore
        char[] passwordChars = password.toCharArray();
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(new FileInputStream(keystoreFile), passwordChars);

        Key key = keyStore.getKey(alias, passwordChars);

        if (key instanceof PrivateKey) {
            Certificate[] certs = keyStore.getCertificateChain(alias);
            for (Certificate cert : certs) {
                return true;
            }
        }
        return false;
    }

    public void create(String alias, String password, File keystoreFile) throws Exception {

        LOG.info("Creating {} {}", alias, keystoreFile);
        //Generate ROOT certificate
        CertAndKeyGen keyGen = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
        keyGen.generate(1024);
        PrivateKey rootPrivateKey = keyGen.getPrivateKey();

        X509Certificate rootCertificate = keyGen.getSelfCertificate(new X500Name("CN=ROOT"), (long) 365 * 24 * 60 * 60);

        rootCertificate = createSignedCertificate(rootCertificate, rootCertificate, rootPrivateKey);

        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = rootCertificate;

        char[] passwordChars = password.toCharArray();
        String keystore = keystoreFile.getAbsolutePath();

        //Store the certificate chain
        storeKeyAndCertificateChain(alias, passwordChars, keystore, rootPrivateKey, chain);
    }

    private void storeKeyAndCertificateChain(String alias, char[] password, String keystore, Key key, X509Certificate[] chain) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null, null);

        keyStore.setKeyEntry(alias, key, password, chain);
        keyStore.store(new FileOutputStream(keystore), password);
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
}
