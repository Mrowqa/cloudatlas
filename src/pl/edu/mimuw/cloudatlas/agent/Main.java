/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Duration;
import pl.edu.mimuw.cloudatlas.model.ZMIHierarchyBuilder;


/**
 *
 * @author pawel
 */
public class Main {
	private static Duration queryDuration = Duration.ofMinutes(5);
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		parseArgs(args);
		if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
		try {
			CloudAtlasAgent agent = new CloudAtlasAgent(ZMIHierarchyBuilder.createDefaultHierarchy());
			agent.startQueryExecutor(queryDuration);
			CloudAtlasInterface stub = (CloudAtlasInterface) UnicastRemoteObject.exportObject(agent, 0);
			Registry registry = LocateRegistry.getRegistry();
            registry.rebind("CloudAtlas", stub);
			System.out.println("CloudAtlas Agent bound");
		} catch (Exception ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private static void parseArgs(String[] args) {
		if (args.length == 0) {
			return;
		}
		if (args.length != 2 || !args[0].equals("--sleep")) {
			System.err.println("Usage: <me> --sleep <num>(h|m|s)");
			System.exit(1);
		}
		char durationUnit = args[1].charAt(args[1].length() - 1);
		int durationValue = Integer.parseInt(args[1].substring(0, args[1].length() - 1));
		switch(durationUnit) {
			case 'h': 
				queryDuration = Duration.ofHours(durationValue); 
				break;
			case 'm':
				queryDuration = Duration.ofMinutes(durationValue); 
				break;
			case 's':
				queryDuration = Duration.ofSeconds(durationValue); 
				break;
			default:
				System.err.println("Invalid duration unit, got " + durationUnit + " allowed h|m|s.");
				System.exit(1);
		}
	}
}
