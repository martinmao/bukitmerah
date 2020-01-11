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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class SecureRandomGenerator implements RandomGenerator {


    public static final SecureRandomGenerator DEFAULT_INSTANCE = new SecureRandomGenerator();

    private static final int DEFAULT_RANDOM_BYTES_LENGTH = 16;

    private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Queue<SecureRandom> randoms = new ConcurrentLinkedQueue<>();

    private String secureRandomClass;

    private String secureRandomAlgorithm = SECURE_RANDOM_ALGORITHM;

    private String secureRandomProvider;


    @Override
    public byte[] nextBytes() {
        return nextBytes(DEFAULT_RANDOM_BYTES_LENGTH);
    }

    @Override
    public byte[] nextBytes(int length) {
        byte[] source = new byte[length];
        nextBytes(source);
        return source;
    }

    @Override
    public <T> T consumeRandom(RandomConsumer randomConsumer) {
        SecureRandom random = randoms.poll();
        if (random == null) {
            random = createSecureRandom();
        }
        try {
            return (T) randomConsumer.apply(random);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            randoms.add(random);
        }
    }


    protected void nextBytes(byte bytes[]) {

        SecureRandom random = randoms.poll();
        if (random == null) {
            random = createSecureRandom();
        }
        random.nextBytes(bytes);
        randoms.add(random);
    }

    private SecureRandom createSecureRandom() {

        SecureRandom result = null;

        long t1 = System.currentTimeMillis();
        if (secureRandomClass != null) {
            try {
                // Construct and seed a new random number generator
                Class<?> clazz = Class.forName(secureRandomClass);
                result = (SecureRandom) clazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        boolean error = false;
        if (result == null) {
            // No secureRandomClass or creation failed. Use SecureRandom.
            try {
                if (secureRandomProvider != null &&
                        secureRandomProvider.length() > 0) {
                    result = SecureRandom.getInstance(secureRandomAlgorithm,
                            secureRandomProvider);
                } else if (secureRandomAlgorithm != null &&
                        secureRandomAlgorithm.length() > 0) {
                    result = SecureRandom.getInstance(secureRandomAlgorithm);
                }
            } catch (NoSuchAlgorithmException e) {
                error = true;
                throw new IllegalStateException(e);
            } catch (NoSuchProviderException e) {
                error = true;
                throw new IllegalStateException(e);
            }
        }

        if (result == null && error) {
            // Invalid provider / algorithm
            try {
                result = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        if (result == null) {
            // Nothing works - use platform default
            result = new SecureRandom();
        }

        // Force seeding to take place
        result.nextInt();

        if ((System.currentTimeMillis() - t1) > 100) {
            logger.warn("Too much time to create secure-random.");
        }
        return result;
    }


    public void setSecureRandomClass(String secureRandomClass) {
        this.secureRandomClass = secureRandomClass;
    }

    public void setSecureRandomAlgorithm(String secureRandomAlgorithm) {
        this.secureRandomAlgorithm = secureRandomAlgorithm;
    }

    public void setSecureRandomProvider(String secureRandomProvider) {
        this.secureRandomProvider = secureRandomProvider;
    }

}
