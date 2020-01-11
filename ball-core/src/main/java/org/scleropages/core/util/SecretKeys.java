/**
 * Copyright 2001-2005 The Apache Software Foundation.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scleropages.core.util;

import com.google.common.collect.Maps;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * utility class provide common operations for java(x) security keys
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class SecretKeys {


    public static final String ALGORITHM_KEY_AES = "AES";
    public static final String ALGORITHM_KEY_DES = "DES";
    public static final String ALGORITHM_KEY_DES_EDE = "DESede";


    public static final String ALGORITHM_KEY_HMAC_MD5 = "HmacMD5";
    public static final String ALGORITHM_KEY_HMAC_SHA1 = "HmacSHA1";
    public static final String ALGORITHM_KEY_HMAC_SHA256 = "HmacSHA256";
    public static final String ALGORITHM_KEY_HMAC_SHA384 = "HmacSHA384";
    public static final String ALGORITHM_KEY_HMAC_SHA512 = "HmacSHA512";



    public static final String ALGORITHM_KEYPAIR_EC = "EC";
    public static final String ALGORITHM_KEYPAIR_DH = "DH";
    public static final String ALGORITHM_KEYPAIR_RSA = "RSA";
    public static final String ALGORITHM_KEYPAIR_DSA = "DSA";


    private static final Map<String, Queue<KeyGenerator>> keyGenerators = Maps.newConcurrentMap();

    private static final Map<String, Queue<KeyPairGenerator>> keyPairGenerators = Maps.newConcurrentMap();


    public interface KeyGeneratorInitializer extends Consumer<KeyGenerator> {
    }

    public interface KeyPairGeneratorInitializer extends Consumer<KeyPairGenerator> {
    }

    /**
     * generate a random symmetric key by given algorithm
     *
     * @param algorithm
     * @return
     */
    public static SecretKey generateRandomKey(String algorithm, KeyGeneratorInitializer initializer) {
        Queue<KeyGenerator> keyQueue = SecretKeys.keyGenerators.computeIfAbsent(algorithm, s -> new ConcurrentLinkedQueue<>());
        KeyGenerator keyGenerator = keyQueue.poll();
        if (null == keyGenerator) {
            try {
                keyGenerator = KeyGenerator.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                keyGenerators.remove(algorithm);
                throw new IllegalArgumentException(e);
            }
        }
        if (null != initializer)
            initializer.accept(keyGenerator);
        SecretKey secretKey = keyGenerator.generateKey();
        keyQueue.add(keyGenerator);
        return secretKey;
    }

    /**
     * generate a random key pair by given algorithm
     *
     * @param algorithm
     * @return
     */
    public static KeyPair generateRandomKeyPair(String algorithm, KeyPairGeneratorInitializer initializer) {
        Queue<KeyPairGenerator> keyPairQueue = SecretKeys.keyPairGenerators.computeIfAbsent(algorithm, s -> new ConcurrentLinkedQueue<>());
        KeyPairGenerator keyPairGenerator = keyPairQueue.poll();
        if (null == keyPairGenerator) {
            try {
                keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                keyPairGenerators.remove(algorithm);
                throw new IllegalArgumentException(e);
            }
        }
        if (null != initializer)
            initializer.accept(keyPairGenerator);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        keyPairQueue.add(keyPairGenerator);
        return keyPair;
    }


    /**
     * generate symmetric key from {@link KeySpec}(key material)
     *
     * @param keySpec
     * @param algorithm
     * @return
     */
    public static SecretKey generateKey(KeySpec keySpec, String algorithm) {
        if (keySpec instanceof SecretKeySpec)
            return (SecretKeySpec) keySpec;
        try {
            return SecretKeyFactory.getInstance(algorithm).generateSecret(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * generate private key from {@link KeySpec}(key material)
     *
     * @param keySpec
     * @param algorithm
     * @return
     */
    public static PrivateKey generatePrivateKey(String algorithm, KeySpec keySpec) {
        try {
            return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * generate public key from {@link KeySpec}(key material)
     *
     * @param keySpec
     * @param algorithm
     * @return
     */
    public static PublicKey generatePublicKey(String algorithm, KeySpec keySpec) {
        try {
            return KeyFactory.getInstance(algorithm).generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * generate key from byte array.<br>
     * NOTE: this method will not check if raw is valid for specify algorithm.
     *
     * @param algorithm
     * @param raw
     * @return
     */
    public static SecretKey generateKey(String algorithm, byte[] raw) {
        return generateKey(new SecretKeySpec(raw, algorithm), algorithm);
    }

    /**
     * generate ASN.1 PKCS#8 encoded private key
     *
     * @param algorithm
     * @param raw
     * @return
     */
    public static PrivateKey generatePKCS8PrivateKey(String algorithm, byte[] raw) {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(raw);
        return generatePrivateKey(algorithm, pkcs8EncodedKeySpec);
    }

    /**
     * generate ASN.1 X.509 encoded public key
     *
     * @param algorithm
     * @param raw
     * @return
     */
    public static PublicKey generateX509PublicKey(String algorithm, byte[] raw) {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(raw);
        return generatePublicKey(algorithm, x509EncodedKeySpec);
    }


    /**
     * Load a {@link sun.security.provider.JavaKeyStore} (java default key store type) key store (.jks).
     *
     * @param in
     * @param password
     * @return
     */
    public static KeyStore loadJks(InputStream in, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(in, password.toCharArray());
            return keyStore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * Load a Public-Key Cryptography Standards(pkcs#12) format (.p12,.pfx)<br>
     * <p>
     * NOTE:
     * https://bugs.openjdk.java.net/browse/JDK-8157404 fixed in jdk13
     *
     * @param in
     * @param password
     * @return
     */
    public static KeyStore loadPkcs12(InputStream in, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int numRead;
            while ((numRead = in.read(buf)) >= 0) {
                baos.write(buf, 0, numRead);
            }
            baos.close();
            // Don't close is. That remains the callers responsibility.
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            keyStore.load(bais, password.toCharArray());
            return keyStore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalArgumentException(e);
        }
    }


    public static void main(String[] args)  {

        for (Provider p : Security.getProviders()) {
            System.out.println("Provider:" + p.getName() + ":" + p.getVersion());
            for (Provider.Service s : p.getServices()) {
                System.out.println("\t Type:" + s.getType() + "，Algorithm：" + s.getAlgorithm());
            }
            System.out.println("--------------------------");
        }
    }
}
