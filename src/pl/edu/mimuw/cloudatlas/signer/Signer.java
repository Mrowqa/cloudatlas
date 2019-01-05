/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.signer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import pl.edu.mimuw.cloudatlas.agent.ZMIModule;

/**
 *
 * @author mrowqa
 */
public class Signer implements SignerInterface {
	public enum Mode {
		SIGNER, SIGN_VERIFIER,
	}
	
    private final static String DIGEST_ALGORITHM = "SHA-256";
    private final static String ENCRYPTION_ALGORITHM = "RSA";
	
	private final PublicKey pubKey;
	private final PrivateKey privKey;
	private final MessageDigest digestGenerator;
	private final Cipher signCipher;
	
	private final HashSet<String> signedQueriesNames = new HashSet<>();
	private final HashSet<String> signedAttributesCreatedByQuery = new HashSet<>(); // SELECT 42 AS xd; <-- "xd" is the attribute here
	
	
	public Signer(Mode mode, String keyFilename) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, NoSuchPaddingException {
		byte[] keyBytes = Files.readAllBytes(Paths.get(keyFilename));
		KeyFactory kf = KeyFactory.getInstance(ENCRYPTION_ALGORITHM);
		
		if (mode == Mode.SIGNER) {
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			privKey = kf.generatePrivate(spec);
			pubKey = null;
		}
		else {
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			pubKey = kf.generatePublic(spec);
			privKey = null;
		}

		digestGenerator = MessageDigest.getInstance(DIGEST_ALGORITHM);
		signCipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
	}

	@Override
	public synchronized String signQueryOperation(QueryOperation query) throws RemoteException {
		assert privKey != null;
		
		Set<String> attributes = null;
		if (query.getOperation() == QueryOperation.Operation.QUERY_INSTALL) {
			if (signedQueriesNames.contains(query.getName())) {
				// because of nature of distributed systems, even if a query has been uninstalled,
				// we can't be sure it is not installed somewhere, so it's safer to allow
				// sign query with given name just once
				throw new RemoteException("Query installation with this name has been already signed");
			}
			String errorMsg = ZMIModule.validateQuery(query.getName(), query.getText());
			if (errorMsg != null) {
				throw new RemoteException("Invalid query: " + errorMsg);
			}
			
			try {
				attributes = ZMIModule.extractAttributesCreatedByQuery(query.getText());
			}
			catch (Exception ex) {
				throw new RemoteException("Interpreter error", ex);
			}
			for (String attribute : attributes) {
				if (signedAttributesCreatedByQuery.contains(attribute)) {
					throw new RemoteException("Query tries to overwrite attribute \"" +
							attribute + "\" which is created by other already signed query.");
				}
			}
		}
		else {
			if (!signedQueriesNames.contains(query.getName())) {
				throw new RemoteException("Only installed queries can be uninstalled");
			}
		}
		
		try {
			byte[] serialized = serializeQueryOperation(query);
			byte[] digest = digestGenerator.digest(serialized);
			signCipher.init(Cipher.ENCRYPT_MODE, privKey);
			byte[] signature = signCipher.doFinal(digest);
			if (query.getOperation() == QueryOperation.Operation.QUERY_INSTALL) {
				signedQueriesNames.add(query.getName());
				signedAttributesCreatedByQuery.addAll(attributes);
			}
			return Base64.getEncoder().encodeToString(signature);
		}
		catch (Exception ex) {
			// for security reasons, we don't want to expose the occurred exception
			throw new RemoteException("Query signing failed.", ex);
		}
	}
	
	public boolean verifyQueryOperationSignature(QueryOperation query, String signature) throws RemoteException {
		assert pubKey != null;
		
		try {
			byte[] serialized = serializeQueryOperation(query);
			byte[] digest = digestGenerator.digest(serialized);
			byte[] signatureBytes = Base64.getDecoder().decode(signature);
			signCipher.init(Cipher.DECRYPT_MODE, pubKey);
			byte[] originalDigest = signCipher.doFinal(signatureBytes);
			return Arrays.equals(digest, originalDigest);
		}
		catch (Exception ex) {
			// for security reasons, we don't want to expose the occurred exception
			throw new RemoteException("Query signature validation failed.");
		}
	}

	private byte[] serializeQueryOperation(QueryOperation query) throws IOException {
		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		ObjectOutputStream out2 = new ObjectOutputStream(out1);
		out2.writeObject(query);
		out2.close();
		out1.close();

		return out1.toByteArray();
	}
}
