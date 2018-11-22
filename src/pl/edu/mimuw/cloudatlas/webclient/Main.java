/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.webclient;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Duration;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;


/**
 *
 * @author mrowqa
 */
public class Main {

	private static Duration sleepDuration = Duration.ofMinutes(5);
	private static Duration dataHistoryLimit = null;

	/**
	 * @param args the command line arguments
	 * @throws java.rmi.RemoteException
	 * @throws java.rmi.NotBoundException
	 * @throws java.lang.InterruptedException
	 */
	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException, IOException {
		Registry registry = LocateRegistry.getRegistry("localhost");
		CloudAtlasInterface stub = (CloudAtlasInterface) registry.lookup("CloudAtlas");

		parseCmdArgs(args);
		HistoricalDataStorage storage = new HistoricalDataStorage(stub, sleepDuration, dataHistoryLimit);
		WebClient wcl = new WebClient(storage);
		storage.start();
		wcl.run();
	}

	private static void parseCmdArgs(String[] args) {
		if (args.length == 0) {
			return;
		}

		if (args.length != 2 || !args[0].equals("--sleep")) {
			System.err.println("Usage: <me> --sleep <num>(h|m|s)");
			System.exit(1);
		}

		char sleepUnit = args[1].charAt(args[1].length() - 1);
		int sleepValue = Integer.parseInt(args[1].substring(0, args[1].length() - 1));
		switch (sleepUnit) {
			case 'h': { sleepDuration = Duration.ofHours(sleepValue); break; }
			case 'm': { sleepDuration = Duration.ofMinutes(sleepValue); break; }
			case 's': { sleepDuration = Duration.ofSeconds(sleepValue); break; }
			default: {
				System.err.println("Invalid time unit: " + sleepUnit);
				System.exit(1);
			}
		}
	}
}
