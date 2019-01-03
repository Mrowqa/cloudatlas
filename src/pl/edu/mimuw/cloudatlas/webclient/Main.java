/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.webclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Duration;
import org.json.JSONObject;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;
import pl.edu.mimuw.cloudatlas.agent.ConfigUtils;
import pl.edu.mimuw.cloudatlas.signer.SignerInterface;


/**
 *
 * @author mrowqa
 */
public class Main {
	private static String zoneName = "/uw/khaki13";
	private static Duration sleepDuration = Duration.ofSeconds(5);
	private static Duration dataHistoryLimit = null;
	private static JSONObject config = null;

	/**
	 * @param args the command line arguments
	 * @throws java.rmi.RemoteException
	 * @throws java.rmi.NotBoundException
	 * @throws java.lang.InterruptedException
	 */
	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException, IOException {
		try {
			parseCmdArgs(args);
		} catch (Exception ex) {
			System.err.println("Exception parsing command args: " + ex.getMessage());
			System.exit(1);
		}
		Registry registry = LocateRegistry.getRegistry("localhost");
		CloudAtlasInterface stubZmi = (CloudAtlasInterface) registry.lookup("CloudAtlas" + zoneName);
		SignerInterface stubSigner = (SignerInterface) registry.lookup("CloudAtlasSigner");

		HistoricalDataStorage storage = new HistoricalDataStorage(stubZmi, sleepDuration, dataHistoryLimit);
		WebClient wcl = new WebClient(storage, stubZmi, stubSigner, config);
		storage.start();
		wcl.run();
	}

	private static void parseCmdArgs(String[] args) {
		if (args.length == 0) {
			return;
		}

		if (args.length == 2 && args[0].equals("--config-file")) {
			try {
				parseConfigFile(args[1]);
			} catch (Exception ex) {
				System.err.println("Exception while reading configuration file: " + args[1] + 
						" " + ex.getMessage());
				System.exit(1);
			}
			return;
		}
		
		if (args.length != 2 || !args[0].equals("--sleep")) {
			System.err.println("Usage: <me> --config-file /path/to/config/file.conf");
			System.err.println("or   : <me> --sleep <num>(h|m|s)");
			System.exit(1);
		}

		sleepDuration = ConfigUtils.parseInterval(args[1]);
	}

	private static void parseConfigFile(String file) throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(file)));
		JSONObject obj = new JSONObject(content);
		if (obj.has("name"))
			zoneName = obj.getString("name");
		if (!obj.has("webclient"))
			return;
		config = obj.getJSONObject("webclient");
	}
}
