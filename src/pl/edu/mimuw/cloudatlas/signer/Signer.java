/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.signer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import pl.edu.mimuw.cloudatlas.agent.ZMIModule;

/**
 *
 * @author mrowqa
 */
public class Signer implements SignerInterface {
	public enum Mode {
		Signer, SignVerifier,
	}
	
    private final static String DIGEST_ALGORITHM = "SHA-256";
    private final static String ENCRYPTION_ALGORITHM = "RSA";
	
	private final PublicKey pubKey;
	private final PrivateKey privKey;
	
	private final HashMap<String, QueryOperation> signedQueries = new HashMap<>();
	
	
	public Signer(Mode mode, String keyFilename) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		byte[] keyBytes = Files.readAllBytes(Paths.get(keyFilename));
		KeyFactory kf = KeyFactory.getInstance(ENCRYPTION_ALGORITHM);
		
		if (mode == Mode.Signer) {
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			privKey = kf.generatePrivate(spec);
			pubKey = null;
		}
		else {
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			pubKey = kf.generatePublic(spec);
			privKey = null;
		}
	}

	@Override
	public byte[] signQueryOperation(QueryOperation query) throws RemoteException {
		assert privKey != null;
		
		if (query.getOperation() == QueryOperation.Operation.QUERY_INSTALL) {
			if (signedQueries.containsKey(query.getName())) {
				// because of nature of distributed systems, even if a query has been uninstalled,
				// we can't be sure it is not installed somewhere, so it's safer to allow
				// sign query with given name just once
				throw new RemoteException("Query installation with this name has been already signed");
			}
			String errorMsg = ZMIModule.validateQuery(query.getName(), query.getText());
			if (errorMsg != null) {
				throw new RemoteException("Invalid query: " + errorMsg);
			}
		}
		else {
			if (!signedQueries.containsKey(query.getName())) {
				throw new RemoteException("Only installed queries can be uninstalled");
			}
		}
		
		// sign and ret
		
		return null; // todo
	}
	
	public boolean checkQueryOperationSignature(QueryOperation query, byte[] signature) {
		assert pubKey != null;
		
		return false; // todo
	}
}
// todo: client signs queries;
// todo: query install verifies signature
