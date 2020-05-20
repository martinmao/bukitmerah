package org.scleropages.core.util; /**
 * 
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.shiro.codec.Base64;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.hash.Md2Hash;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.crypto.hash.Sha1Hash;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.crypto.hash.Sha384Hash;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.springframework.util.Assert;

/**
 * 
 * Message org.scleropages.core.util.Digests(Hash) algorithm Utility class based Apache Shiro.
 * 
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 * 
 */
public abstract class Digests {

	// ===================supported hash digest=====================//

	public static final String SHA1 = Sha1Hash.ALGORITHM_NAME;
	public static final String SHA256 = Sha256Hash.ALGORITHM_NAME;
	public static final String SHA384 = Sha384Hash.ALGORITHM_NAME;
	public static final String SHA512 = Sha512Hash.ALGORITHM_NAME;
	public static final String MD2 = Md2Hash.ALGORITHM_NAME;
	public static final String MD5 = Md5Hash.ALGORITHM_NAME;

	public static final String BASE64 = "BASE64";

	public static final String HEX = "HEX";

	public static final int DEFAULT_HASH_ITERATIONS = 1;

	// ===================helper methods=====================//

	public static final String getHashDigest(String source) {

		Assert.hasText(source, "hash digest must not null.");

		if (SHA1.equalsIgnoreCase(source))
			return SHA1;
		if (SHA256.equalsIgnoreCase(source))
			return SHA256;
		if (SHA384.equalsIgnoreCase(source))
			return SHA384;
		if (SHA512.equalsIgnoreCase(source))
			return SHA512;
		if (MD2.equalsIgnoreCase(source))
			return MD2;
		if (MD5.equalsIgnoreCase(source))
			return MD5;

		throw new IllegalArgumentException("unsupport hash digest. ");
	}

	public static byte[] plainTextToHashDigest(String hashDigest, String plainText, Object salt,
			Integer hashIterations) {
		hashDigest = getHashDigest(hashDigest);
		if (SHA1.equals(hashDigest))
			return plainTextToSHA1(plainText, salt, hashIterations);
		if (SHA256.equals(hashDigest))
			return plainTextToSHA256(plainText, salt, hashIterations);
		if (SHA384.equals(hashDigest))
			return plainTextToSHA384(plainText, salt, hashIterations);
		if (SHA512.equals(hashDigest))
			return plainTextToSHA512(plainText, salt, hashIterations);
		if (MD2.equals(hashDigest))
			return plainTextToMD2(plainText, salt, hashIterations);
		return plainTextToMD5(plainText, salt, hashIterations);
	}

	protected static void assertParams(String plainText, Object salt, Integer hashIterations) {
		Assert.notNull(plainText, "plainText must not null.");
		Assert.notNull(salt, "salt must not null.");
	}

	public static final byte[] plainTextToSHA1(String plainText, Object salt, Integer hashIterations) {
		assertParams(plainText, salt, hashIterations);
		return new Sha1Hash(plainText, salt, null == hashIterations ? DEFAULT_HASH_ITERATIONS : hashIterations)
				.getBytes();
	}

	public static final byte[] plainTextToSHA1(String plainText) {
		Assert.notNull(plainText, "plainText must not null.");
		return new Sha1Hash(plainText).getBytes();
	}

	public static final byte[] plainTextToSHA256(String plainText, Object salt, Integer hashIterations) {
		assertParams(plainText, salt, hashIterations);
		return new Sha256Hash(plainText, salt, null == hashIterations ? DEFAULT_HASH_ITERATIONS : hashIterations)
				.getBytes();
	}

	public static final byte[] plainTextToSHA384(String plainText, Object salt, Integer hashIterations) {
		assertParams(plainText, salt, hashIterations);
		return new Sha384Hash(plainText, salt, null == hashIterations ? DEFAULT_HASH_ITERATIONS : hashIterations)
				.getBytes();
	}

	public static final byte[] plainTextToSHA512(String plainText, Object salt, Integer hashIterations) {
		assertParams(plainText, salt, hashIterations);
		return new Sha512Hash(plainText, salt, null == hashIterations ? DEFAULT_HASH_ITERATIONS : hashIterations)
				.getBytes();
	}

	public static final byte[] plainTextToMD2(String plainText, Object salt, Integer hashIterations) {
		assertParams(plainText, salt, hashIterations);
		return new Md2Hash(plainText, salt, null == hashIterations ? DEFAULT_HASH_ITERATIONS : hashIterations)
				.getBytes();
	}

	public static final byte[] plainTextToMD5(String plainText, Object salt, Integer hashIterations) {
		assertParams(plainText, salt, hashIterations);
		return new Md5Hash(plainText, salt, null == hashIterations ? DEFAULT_HASH_ITERATIONS : hashIterations)
				.getBytes();
	}

	public static String getEncode(String encode) {
		if (BASE64.equalsIgnoreCase(encode))
			return BASE64;
		else if (HEX.equalsIgnoreCase(encode))
			return HEX;
		throw new IllegalArgumentException("not supported encode: " + encode);
	}

	public static final byte[] decode(String encode, String text) {
		if (BASE64.equalsIgnoreCase(encode))
			return decodeBase64(text);
		else if (HEX.equalsIgnoreCase(encode))
			return decodeHex(text);
		throw new IllegalArgumentException("not supported encode: " + encode);
	}

	public static final String encode(String encode, byte[] bytes) {
		if (BASE64.equalsIgnoreCase(encode))
			return encodeBase64(bytes);
		else if (HEX.equalsIgnoreCase(encode))
			return encodeHex(bytes);
		throw new IllegalArgumentException("not supported encode: " + encode);
	}

	public static final byte[] decodeBase64(String base64) {
		return Base64.decode(base64);
	}

	public static final String encodeBase64(byte[] bytes) {
		return Base64.encodeToString(bytes);
	}

	public static final byte[] decodeHex(String text) {
		return Hex.decode(text);
	}

	public static final String encodeHex(byte[] bytes) {
		return Hex.encodeToString(bytes);
	}
}
