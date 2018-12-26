/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Duration;
import java.util.logging.ConsoleHandler;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ZMI;
import pl.edu.mimuw.cloudatlas.model.ZMIHierarchyBuilder;


/**
 *
 * @author pawel
 */
public class Main {
	private static Duration queryDuration = Duration.ofSeconds(5);
	private static PathName targetZone;
	private static boolean testCommunicationModule = false;
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		parseArgs(args);
		if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
		try {
			Module[] modules;

			if (testCommunicationModule) {
				modules = new Module[] {
					new TimerModule(),
					new CommunicationModule(),
					new TestCommunicationModule(),
				};

				Logger logger = Logger.getLogger(CommunicationModule.class.getName());
				logger.setLevel(Level.FINER);
				ConsoleHandler handler = new ConsoleHandler();
				handler.setLevel(Level.FINER);
				logger.addHandler(handler);
			}
			else {
				modules = new Module[] {
					new RMIModule(),
					new ZMIModule(createZmi(), queryDuration),
					new TimerModule(),
					new CommunicationModule(),
				};
			}

			ModulesHandler handler = new ModulesHandler(modules);
			handler.runAll();
		} catch (Exception ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private static void parseArgs(String[] args) {
		if (args.length == 0) {
			System.out.println("Warning: using agent for handling whole ZMI instead of one node.");
			return;
		}

		if (args.length == 1 && args[0].equals("--test-communication-module")) {
			testCommunicationModule = true;
			return;
		}

		if (args.length != 4 || !args[0].equals("--sleep") || !args[2].equals("--zone")) {
			System.err.println("Usage: <me> --sleep <num>(h|m|s) --zone /my/leaf/node");
			System.err.println("   or: <me> --test-communication-module");
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
		
		targetZone = new PathName(args[3]);
	}
	
	private static ZMI createZmi() throws ParseException, UnknownHostException {
		if (targetZone == null) {
			return ZMIHierarchyBuilder.createDefaultHierarchy();
		} else {
			return ZMIHierarchyBuilder.createLeafNodeHierarchy(targetZone);
		}
	}
}
