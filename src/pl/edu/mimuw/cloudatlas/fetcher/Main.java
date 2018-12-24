/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.fetcher;

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

	private static Duration sleepDuration = Duration.ofSeconds(5);
	private static String targetZone = "/uw/violet07";

	/**
	 * @param args the command line arguments
	 * @throws java.rmi.RemoteException
	 * @throws java.rmi.NotBoundException
	 * @throws java.lang.InterruptedException
	 */
	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException {
		Registry registry = LocateRegistry.getRegistry("localhost");
		CloudAtlasInterface stub = (CloudAtlasInterface) registry.lookup("CloudAtlas");

		parseCmdArgs(args);
		Fetcher f = new Fetcher(sleepDuration, stub, targetZone);
		f.run(); // it is just fine, no need for f.start()
	}

	private static void parseCmdArgs(String[] args) {
		if (args.length == 0) {
			System.out.println("Warning: using fetcher with default zone: " + targetZone);
			return;
		}

		if (args.length != 4 || !args[0].equals("--sleep") || !args[2].equals("--zone")) {
			System.err.println("Usage: <me> --sleep <num>(h|m|s) --zone /my/leaf/node");
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

		targetZone = args[3];
	}
}
