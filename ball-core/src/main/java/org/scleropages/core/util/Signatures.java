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
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * utility class provide common operations for java(jca,jce) security signature
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class Signatures {


    public static final String ALGORITHM_MD5_WITH_RSA = "MD5withRSA";
    public static final String ALGORITHM_SHA1_WITH_RSA = "SHA1withRSA";
    public static final String ALGORITHM_SHA256_WITH_RSA = "SHA256withRSA";
    public static final String ALGORITHM_SHA384_WITH_RSA = "SHA384withRSA";
    public static final String ALGORITHM_SHA512_WITH_RSA = "SHA512withRSA";

    public static final String ALGORITHM_HMAC_MD5 = "HmacMD5";
    public static final String ALGORITHM_HMAC_SHA1 = "HmacSHA1";
    public static final String ALGORITHM_HMAC_SHA256 = "HmacSHA256";
    public static final String ALGORITHM_HMAC_SHA384 = "HmacSHA384";
    public static final String ALGORITHM_HMAC_SHA512 = "HmacSHA512";

    public static final String ALGORITHM_SHA1_WITH_DSA = "SHA1withDSA";
    public static final String ALGORITHM_SHA256_WITH_DSA = "SHA256withDSA";

    public static final String ALGORITHM_SHA1_WITH_ECDSA = "SHA1withECDSA";
    public static final String ALGORITHM_SHA256_WITH_ECDSA = "SHA256withECDSA";
    public static final String ALGORITHM_SHA384_WITH_ECDSA = "SHA384withECDSA";
    public static final String ALGORITHM_SHA512_WITH_ECDSA = "SHA512withECDSA";


    private static final Map<String, Queue<SignatureProvider>> signatureProviders = Maps.newConcurrentMap();


    public interface Signer extends Consumer<SignatureProvider> {

        @Override
        void accept(SignatureProvider signatureProvider);
    }


    /**
     * perform sign or verify with specify algorithm and {@link Signer}<br>
     * {@link Signer} conceptually similar {@link Signature} provide a high level interface to process signature sign and verify.
     *
     * @param algorithm
     * @param signer
     */
    public static void doInSign(String algorithm, Signer signer) {
        Queue<SignatureProvider> providerQueue = Signatures.signatureProviders.computeIfAbsent(algorithm, s -> new ConcurrentLinkedQueue<>());
        SignatureProvider signatureProvider = providerQueue.poll();
        if (null == signatureProvider)
            try {
                if (isHmac(algorithm)) {
                    Mac mac = Mac.getInstance(algorithm);
                    signatureProvider = new HmacSign(mac);
                } else {
                    Signature signature = Signature.getInstance(algorithm);
                    signatureProvider = new DefaultSign(signature);
                }
                signer.accept(signatureProvider);
                providerQueue.add(signatureProvider);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException(e);
            }
    }

    private static boolean isHmac(String algorithm) {
        return StringUtils.startsWith(algorithm, "Hmac");
    }


    /**
     * a common signature operations for any implementations({@link Signature},{@link Mac}.... )
     */
    public interface SignatureProvider {


        void initSign(Key key);

        void initVerify(Key key);

        /**
         * Updates the data to be signed or verified, using the specified
         * array of bytes.
         *
         * @param update
         * @throws SignatureException
         */
        void update(byte[] update) throws SignatureException;

        /**
         * Returns the signature bytes of all the data updated.
         * The format of the signature depends on the underlying
         * signature scheme.
         *
         * @return
         * @throws SignatureException
         */
        byte[] sign() throws SignatureException;

        /**
         * Verifies the passed-in signature.
         *
         * @param verify
         * @return
         * @throws SignatureException
         */
        boolean isValid(byte[] verify) throws SignatureException;

        /**
         * Returns native {@link Signature} (rsa,dsa)or {@link Mac} (mac)
         *
         * @param <T>
         * @return
         */
        <T> T nativeSignature();
    }


    /**
     * provide RSA,DSA... signatures
     */
    private static class DefaultSign implements SignatureProvider {

        private final Signature signature;

        private DefaultSign(Signature signature) {
            this.signature = signature;
        }

        @Override
        public void initSign(Key key) {
            try {
                signature.initSign((PrivateKey) key);
            } catch (InvalidKeyException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public void initVerify(Key key) {
            try {
                signature.initVerify((PublicKey) key);
            } catch (InvalidKeyException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public void update(byte[] update) throws SignatureException {
            signature.update(update);
        }

        @Override
        public byte[] sign() throws SignatureException {
            return signature.sign();
        }

        @Override
        public boolean isValid(byte[] verify) throws SignatureException {
            return signature.verify(verify);
        }

        @Override
        public <T> T nativeSignature() {
            return (T) signature;
        }

    }

    /**
     * Hmac implementation.
     */
    private static class HmacSign implements SignatureProvider {

        private final Mac mac;

        private HmacSign(Mac mac) {
            this.mac = mac;
        }

        @Override
        public void initSign(Key key) {
            try {
                mac.init(key);
            } catch (InvalidKeyException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public void initVerify(Key key) {
            try {
                mac.init(key);
            } catch (InvalidKeyException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public void update(byte[] update) throws SignatureException {
            mac.update(update);
        }

        @Override
        public byte[] sign() throws SignatureException {
            return mac.doFinal();
        }

        @Override
        public boolean isValid(byte[] verify) throws SignatureException {
            byte[] sign = mac.doFinal();
            if (sign == verify) return true;
            if (sign == null || verify == null) {
                return false;
            }
            if (sign.length != verify.length) {
                return false;
            }

            int result = 0;
            for (int i = 0; i < sign.length; i++) {
                result |= sign[i] ^ verify[i];
            }
            return result == 0;
        }

        @Override
        public <T> T nativeSignature() {
            return (T) mac;
        }
    }
}
