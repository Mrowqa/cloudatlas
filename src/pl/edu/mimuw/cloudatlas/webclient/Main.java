/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.webclient;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import org.json.JSONObject;
import pl.edu.mimuw.cloudatlas.agent.ConfigUtils;


/**
 *
 * @author mrowqa
 */
public class Main {
	private final static String DEFAULT_REGISTRY_HOST = "localhost"; // for both agent & signer
	private final static String DEFAULT_DATA_HISTORY_LIMIT = null; // can be also a string with an interval
	private final static String DEFAULT_DATA_DOWNLOAD_INTERVAL = "5s";
	private final static String DEFAULT_ZONE_NAME = "/uw/khaki13";
	private final static int DEFAULT_HTTP_PORT = 8000;
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

		HistoricalDataStorage storage = new HistoricalDataStorage(config);
		WebClient wcl = new WebClient(storage, config);
		storage.start();
		wcl.run();
	}

	private static void parseCmdArgs(String[] args) {
		if (args.length == 0) {
			fillInDefaultConfigValues();
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
		
		if (args.length != 4 || !args[0].equals("--agent-host") || !args[2].equals("--zone")) {
			System.err.println("Usage: <me> --config-file /path/to/config/file.conf");
			System.err.println("   or: <me> --agent-host <host> --zone /my/leaf/zone");
			System.exit(1);
		}

		fillInDefaultConfigValues();
		config.put("agentRegistryHost", args[1]);
		config.put("name", args[3]);
	}

	private static void parseConfigFile(String file) throws IOException {
		JSONObject obj = ConfigUtils.getConfigObjectFromFile(file);
		String zoneName = null;
		if (obj.has("name"))
			zoneName = obj.getString("name");
		if (obj.has("webclient"))
			config = obj.getJSONObject("webclient");
		fillInDefaultConfigValues();
		if (zoneName != null)
			config.put("name", zoneName);
	}
	
	private static void fillInDefaultConfigValues() {
		if (config == null) {
			config = new JSONObject();
		}
		
		if (!config.has("name")) {
			config.put("name", DEFAULT_ZONE_NAME);
		}
		
		if (!config.has("agentRegistryHost")) {
			config.put("agentRegistryHost", DEFAULT_REGISTRY_HOST);
		}
		
		if (!config.has("signerRegistryHost")) {
			config.put("signerRegistryHost", DEFAULT_REGISTRY_HOST);
		}
		
		if (!config.has("dataHistoryLimit")) {
			config.put("dataHistoryLimit", DEFAULT_DATA_HISTORY_LIMIT);
		}
		
		if (!config.has("dataDownloadInterval")) {
			config.put("dataDownloadInterval", DEFAULT_DATA_DOWNLOAD_INTERVAL);
		}
		
		if (!config.has("httpPort")) {
			config.put("httpPort", DEFAULT_HTTP_PORT);
		}
	}
}
