package com.dslab.Cipher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

public class KeyWorker {

	public static PrivateKey readPrivateKey(String pathToPrivateKey) throws IOException {
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

	public static PublicKey readPublicKey(String pathToPublicKey) throws IOException {
		PEMReader in = new PEMReader(new FileReader(pathToPublicKey));
		return (PublicKey) in.readObject();
	}

	public static byte[] createSecureRandomNumber(int bytes) {
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[bytes];
		secureRandom.nextBytes(number);
		return number;
	}

	public static SecretKey SecretKey() {

		KeyGenerator generator;
		try {
			generator = KeyGenerator.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			generator.init(32);
			return generator.generateKey();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Cipher getCipherForAlgorithm(String algo, Boolean encryption, Key key, int iv)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher crypt = Cipher.getInstance(algo);
		// MODE is the encryption/decryption mode
		// KEY is either a private, public or secret key
		// IV is an init vector, needed for AES
		if (encryption) {
			crypt.init(Cipher.ENCRYPT_MODE, key);
		} else {
			crypt.init(Cipher.DECRYPT_MODE, key);
		}
		return crypt;
	}
}
