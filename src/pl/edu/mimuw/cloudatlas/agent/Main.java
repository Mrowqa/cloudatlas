/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Duration;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueBoolean;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueDouble;
import pl.edu.mimuw.cloudatlas.model.ValueDuration;
import pl.edu.mimuw.cloudatlas.model.ValueInt;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ValueTime;
import pl.edu.mimuw.cloudatlas.model.ZMI;
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
		parseDuration(args);
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
	
	private static void parseDuration(String[] args) {
		if (args.length < 1) {
			return;
		}
		char durationUnit = args[0].charAt(args[0].length() - 1);
		int durationValue = Integer.parseInt(args[0].substring(0, args[0].length() - 1));
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
