/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import pl.edu.mimuw.cloudatlas.agent.dissemination.DisseminationModule;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import org.json.JSONObject;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;
import pl.edu.mimuw.cloudatlas.model.ZMIHierarchyBuilder;
import pl.edu.mimuw.cloudatlas.model.ZMIJSONSerializer;


/**
 *
 * @author pawel
 */
public class Main {
	private static JSONObject config = null;
	private static PathName targetZone = new PathName("/uw/violet07");
	private static int socketPort = 42777;
	private static String pubKeyFilename = "public_key.der";
	private static boolean testCommunicationModule = false;
	private static ValueSet fallbackContacts = new ValueSet(TypePrimitive.CONTACT);

	/** 
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		try {
			parseCmdArgs(args);
		} catch (Exception ex) {
			System.err.println("Exception parsing command args: " + ex.getMessage());
			System.exit(1);
		}
		if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
		Module[] modules = null;
		try {
			if (testCommunicationModule) {
				modules = new Module[] {
					new TimerModule(),
					new CommunicationModule(socketPort),
					new TestCommunicationModule(socketPort),
				};

				Logger logger = Logger.getLogger(CommunicationModule.class.getName());
				logger.setLevel(Level.FINER);
				ConsoleHandler handler = new ConsoleHandler();
				handler.setLevel(Level.FINER);
				logger.addHandler(handler);
			}
			else {
				modules = new Module[] {
					new RMIModule(targetZone.getName()),
					new TimerModule(),
					new CommunicationModule(socketPort),
					ZMIModule.createModule(createZmi(), config),
					DisseminationModule.createModule(targetZone, config),
				};
			}
		} catch (Exception ex) {
			System.err.println("Error during modules creation " + ex);
			System.exit(1);
		}
		try {
			ModulesHandler handler = new ModulesHandler(modules);
			handler.runAll();
		} catch (Exception ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private static void parseCmdArgs(String[] args) throws IOException {
		if (args.length == 1 && args[0].equals("--test-communication-module")) {
			testCommunicationModule = true;
			return;
		}
		
		if (args.length != 2 || !args[0].equals("--config-file")) {
			System.err.println("Usage: <me> --config-file path/to/config/file.conf");
			System.err.println("   or: <me> --test-communication-module");
			System.exit(1);
		}
		
		parseConfigFile(args[1]);
	}
	
	private static void parseConfigFile(String file) throws IOException {
		JSONObject obj = ConfigUtils.getConfigObjectFromFile(file);
		if (obj.has("name"))
			targetZone = new PathName(obj.getString("name"));
		else
			System.err.println("Warning: Using default zone " + targetZone);
		
		if (obj.has("pubKeyFilename"))
			pubKeyFilename = obj.getString("pubKeyFilename");
		else 
			System.err.println("Warning: Using default public key: " + pubKeyFilename);
		
		if (!obj.has("agent"))
			throw new IllegalArgumentException("Agent configuration not found.");		
		config = obj.getJSONObject("agent");
		
		if (!config.has("fallbackContacts"))
			throw new IllegalArgumentException("Fallback contact set cannot be empty.");
		fallbackContacts = (ValueSet)ZMIJSONSerializer.JSONToValue(config.getJSONObject("fallbackContacts"));
		
		if (config.has("socketPort")) 
			socketPort = config.getInt("socketPort");
		
		config.put("name", targetZone.getName());
		config.put("pubKeyFilename", pubKeyFilename);
	}
	
	public static ZMI createZmi() throws ParseException, UnknownHostException {
		return ZMIHierarchyBuilder.createLeafNodeHierarchy(targetZone);
	}
}
