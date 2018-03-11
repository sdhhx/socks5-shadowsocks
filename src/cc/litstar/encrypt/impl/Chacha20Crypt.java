/*
 * Copyright (c) 2015, Blake
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The name of the author may not be used to endorse or promote
 * products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cc.litstar.encrypt.impl;

/**
 * Chacha20 cipher implementation
 */
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.ChaChaEngine;
import org.bouncycastle.crypto.engines.ChaCha7539Engine;

import cc.litstar.encrypt.CryptBase;

import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Chacha20Crypt extends CryptBase {
	public final static String CIPHER_CHACHA20 = "chacha20";
	public final static String CIPHER_CHACHA20_IETF = "chacha20-ietf";

	public static Map<String, String> getCiphers() {
		Map<String, String> ciphers = new HashMap<>();
		ciphers.put(CIPHER_CHACHA20, Chacha20Crypt.class.getName());
		ciphers.put(CIPHER_CHACHA20_IETF, Chacha20Crypt.class.getName());
		return ciphers;
	}

	public Chacha20Crypt(String name, String password) {
		super(name, password);
	}

	@Override
	protected StreamCipher getCipher(boolean isEncrypted) throws InvalidAlgorithmParameterException {
		if (_name.equals(CIPHER_CHACHA20)) {
			return new ChaChaEngine();
		}
		else if (_name.equals(CIPHER_CHACHA20_IETF)) {
			return new ChaCha7539Engine();
		}
		return null;
	}

	@Override
	protected SecretKey getKey() {
		return new SecretKeySpec(_ssKey.getEncoded(), "AES");

	}

	@Override
	protected void _encrypt(byte[] data, ByteArrayOutputStream stream) {
		int noBytesProcessed;
		byte[] buffer = new byte[data.length];

		noBytesProcessed = encCipher.processBytes(data, 0, data.length, buffer, 0);
		stream.write(buffer, 0, noBytesProcessed);
	}

	@Override
	protected void _decrypt(byte[] data, ByteArrayOutputStream stream) {
		int BytesProcessedNum;
		byte[] buffer = new byte[data.length];
		BytesProcessedNum = decCipher.processBytes(data, 0, data.length, buffer, 0);
		stream.write(buffer, 0, BytesProcessedNum);

	}

	@Override
	public int getKeyLength() {
		if (_name.equals(CIPHER_CHACHA20) || _name.equals(CIPHER_CHACHA20_IETF)) {
			return 32;
		}
		return 0;
	}

	@Override
	public int getIVLength() {
		if (_name.equals(CIPHER_CHACHA20)) {
			return 8;
		}
		else if (_name.equals(CIPHER_CHACHA20_IETF)) {
			return 12;
		}
		return 0;
	}
}