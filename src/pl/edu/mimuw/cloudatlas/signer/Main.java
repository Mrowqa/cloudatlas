/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.signer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mrowqa
 */
public class Main {
	private static String privKeyFilename = "private_key.der";
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		parseArgs(args);
		if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
		try {
			Signer signer = new Signer(Signer.Mode.Signer, privKeyFilename);
			
			SignerInterface stub = (SignerInterface) UnicastRemoteObject.exportObject(signer, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("CloudAtlasSigner", stub);
			System.out.println("Signer RMI interface bound.");
		} catch (Exception ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private static void parseArgs(String[] args) {
		if (args.length == 0) {
			System.out.println("Warning: using default private key: " + privKeyFilename);
			return;
		}

		if (args.length != 2 || !args[0].equals("--private-key")) {
			System.err.println("Usage: <me> --private-key path/to/private_key.der");
			System.exit(1);
		}
		
		privKeyFilename = args[1];
	}
}
