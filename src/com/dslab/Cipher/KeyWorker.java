package com.dslab.Cipher;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.util.encoders.Hex;

public class KeyWorker {

	public static PrivateKey readPrivateKey(String pathToPrivateKey) throws FileNotFoundException {
		while (true) {
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
			KeyPair keyPair;
			try {
				keyPair = (KeyPair) in.readObject();
				return keyPair.getPrivate();
			} catch (IOException e) {
				System.out.println("Wrong password, please try again: ");
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		}
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

	public static Cipher getCipherForAlgorithm(String algo, Boolean encryption, Key key, byte[] iv)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException {
		Cipher crypt = Cipher.getInstance(algo);
		// MODE is the encryption/decryption mode
		// KEY is either a private, public or secret key
		// IV is an init vector, needed for AES
		if (encryption) {
			if (!Arrays.equals(iv, null)) {
				crypt.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
			} else {
				crypt.init(Cipher.ENCRYPT_MODE, key);
			}
		} else {
			if (!Arrays.equals(iv, null)) {
				crypt.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
			} else {
				crypt.init(Cipher.DECRYPT_MODE, key);
			}
		}
		return crypt;
	}

	public static Key readSharedSecretKey(String path, String algo) throws IOException {
		byte[] keyBytes = new byte[1024];
		FileInputStream fis = new FileInputStream(path);
		fis.read(keyBytes);
		fis.close();
		byte[] input = Hex.decode(keyBytes);
		// make sure to use the right ALGORITHM for what you want to do
		// (see text)
		return new SecretKeySpec(input, algo);
	}

	public static byte[] createHashMac(Key secretKey, String algo, String message) throws NoSuchAlgorithmException,
			InvalidKeyException {
		Mac hMac = Mac.getInstance(algo);
		hMac.init(secretKey);
		// MESSAGE is the message to sign in bytes
		hMac.update(message.getBytes());
		return hMac.doFinal();
	}

	public static Boolean verifyHash(byte[] computedHash, byte[] receivedHash) {
		return MessageDigest.isEqual(computedHash, receivedHash);
	}

}
