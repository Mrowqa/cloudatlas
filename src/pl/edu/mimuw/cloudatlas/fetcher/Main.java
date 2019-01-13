/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.fetcher;

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


/**
 *
 * @author mrowqa
 */
public class Main {
	public final static String RMI_HOST = "localhost";
	private static Duration sleepDuration = Duration.ofSeconds(5);
	private static String targetZone = "/uw/violet07";

	/**
	 * @param args the command line arguments
	 * @throws java.rmi.RemoteException
	 * @throws java.rmi.NotBoundException
	 * @throws java.lang.InterruptedException
	 */
	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException {
		try {
			parseCmdArgs(args);
		} catch (Exception ex) {
			System.err.println("Exception parsing command args: " + ex.getMessage());
			System.exit(1);
		}
		
		Fetcher f = new Fetcher(sleepDuration, RMI_HOST, targetZone);
		f.run(); // it is just fine, no need for f.start()
	}

	private static void parseCmdArgs(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Warning: using fetcher with default zone: " + targetZone);
			return;
		}

		if (args.length == 2 && args[0].equals("--config-file")) {
			parseConfigFile(args[1]);
			return;
		}
		
		if (args.length != 4 || !args[0].equals("--sleep") || !args[2].equals("--zone")) {
			System.err.println("Usage: <me> --config-file path/to/config/file.conf");
			System.err.println("   or: <me> --sleep <num>(h|m|s) --zone /my/leaf/node");
			System.exit(1);
		}

		sleepDuration = ConfigUtils.parseInterval(args[1]);
		targetZone = args[3];
	}
	
	private static void parseConfigFile(String file) throws IOException {
		JSONObject obj = ConfigUtils.getConfigObjectFromFile(file);
		if (obj.has("name"))
			targetZone = obj.getString("name");
		if (!obj.has("fetcher"))
			return;
		JSONObject config = obj.getJSONObject("fetcher");
		if (config.has("sleepDuration"))
			sleepDuration = ConfigUtils.parseInterval(config.getString("sleepDuration"));
	}
}
