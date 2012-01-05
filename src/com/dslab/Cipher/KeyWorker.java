package com.dslab.Cipher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

public class KeyWorker {

	public PrivateKey readPrivateKey(String pathToPrivateKey) throws IOException {
		PEMReader in = new PEMReader(new FileReader(pathToPrivateKey), new PasswordFinder() {
			@Override
			public char[] getPassword() {
				// reads the password from standard input for decrypting the private key
				System.out.println("Enter pass phrase:");
				try {
					return new BufferedReader(new InputStreamReader(System.in)).readLine().toCharArray();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					return null;
				}
			}
		});
		KeyPair keyPair = (KeyPair) in.readObject();
		return keyPair.getPrivate();
	}

	public PublicKey readPublicKey(String pathToPublicKey) throws IOException {
		PEMReader in = new PEMReader(new FileReader(pathToPublicKey));
		return (PublicKey) in.readObject();
	}

	public byte[] createSecureRandomNumber() {
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[32];
		secureRandom.nextBytes(number);
		return number;
	}

}
