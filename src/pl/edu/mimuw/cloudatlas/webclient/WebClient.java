/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.webclient;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONObject;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ZMIJSONSerializer;
import pl.edu.mimuw.cloudatlas.signer.QueryOperation;
import pl.edu.mimuw.cloudatlas.signer.SignerInterface;

/**
 *
 * @author mrowqa
 */
public class WebClient {
	private final CloudAtlasInterface zmiRmi;
	private final SignerInterface signerRmi;
	private final HistoricalDataStorage dataStorage;
	private final int httpPort;
	private final JSONObject config;
	
	public WebClient(HistoricalDataStorage dataStorage, JSONObject config) throws RemoteException, NotBoundException {
		this.dataStorage = dataStorage;
		this.config = config;
		this.httpPort = config.getInt("httpPort");
		
		Registry agentRegistry = LocateRegistry.getRegistry(config.getString("agentRegistryHost"));
		this.zmiRmi = (CloudAtlasInterface) agentRegistry.lookup("CloudAtlas" + config.getString("name"));
		Registry signerRegistry = LocateRegistry.getRegistry(config.getString("signerRegistryHost"));
		this.signerRmi = (SignerInterface) signerRegistry.lookup("CloudAtlasSigner");
	}
	
	public void run() throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(httpPort), 0);		
		server.createContext("/history", new HistoryHandler());
		server.createContext("/static", new GetStaticFileHandler());
		server.createContext("/query/install", new InstallQueryHandler());
		server.createContext("/query/uninstall", new UninstallQueryHandler());
		server.createContext("/query/get", new GetAllQueriesHandler());
		server.createContext("/zones/get", new GetZonesHandler());
		server.createContext("/zones/attributes/get", new GetZoneAttributesHandler());
		server.createContext("/zones/attributes/set", new SetZoneAttributesHandler());
		server.createContext("/fallback-contacts/get", new GetFallbackContactsHandler());
		server.createContext("/fallback-contacts/set", new SetFallbackContactsHandler());
		server.createContext("/", new RedirectHandler("/static/index.html"));
		server.setExecutor(null); // creates a default executor
		server.start();
		System.out.println("Started webclient at http://localhost:" + httpPort + "/");
	}
	
	private class RedirectHandler implements HttpHandler {
		private String location;
		
		public RedirectHandler(String location) {
			this.location = location;
		}
		
		@Override
		public void handle(HttpExchange t) throws IOException {			
			Headers h = t.getResponseHeaders();
			h.add("Location", this.location);
			t.sendResponseHeaders(302, 0);
			t.getResponseBody().close();
		}
	}
	
	private class HistoryHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("GET")) {
				return;
			}
			
			Map<String, String> params = WebClient.queryToMap(t.getRequestURI().getQuery());
			Integer limit = null;
			if (params.get("limit") != null) {
				try {
					limit = Integer.parseInt(params.get("limit"));
				}
				catch (NumberFormatException ex) {}
			}
			
			WebClient.sendResponse(t, 200, dataStorage.getHistoricalData(limit));
		}
	}
	
	private class GetZonesHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("GET")) {
				return;
			}
			
			String rmiResult;
			int statusCode = 200;
			try {
				rmiResult = ZMIJSONSerializer.valueToJSONString(zmiRmi.getZones());
			}
			catch (Exception ex) {
				rmiResult = "Error:\n" + exceptionToString(ex);
				statusCode = 400;
			}
			
			WebClient.sendResponse(t, statusCode, rmiResult);
		}
	}
	
	private class GetZoneAttributesHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("GET")) {
				return;
			}
			
			String request = new Scanner(t.getRequestURI().getQuery()).nextLine();
			Map<String, String> params = WebClient.queryToMap(request);
			
			String rmiResult;
			try {
				ValueString zoneName = new ValueString(params.get("zone-name"));
				rmiResult = ZMIJSONSerializer.attributesMapToJSONString(zmiRmi.getZoneAttributes(zoneName));
			}
			catch (Exception ex) {
				rmiResult = "Error:\n" + exceptionToString(ex);
			}
			
			WebClient.sendResponse(t, 200, rmiResult);
		}
	}
	
	private class SetZoneAttributesHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("POST")) {
				return;
			}
			
			String request = new Scanner(t.getRequestBody()).nextLine();
			Map<String, String> params = WebClient.queryToMap(request);
			
			String rmiResult = "OK";
			try {
				ValueString zoneName = new ValueString(params.get("zone-name"));
				AttributesMap attrs = ZMIJSONSerializer.JSONStringToAttributesMap(params.get("zone-attrs"));
				zmiRmi.setZoneAttributes(zoneName, attrs);
			}
			catch (Exception ex) {
				rmiResult = "Error:\n" + exceptionToString(ex);
			}
			
			WebClient.sendResponse(t, 200, rmiResult);
		}
	}
	
	private class GetFallbackContactsHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("GET")) {
				return;
			}
			
			String rmiResult;
			try {
				rmiResult = ZMIJSONSerializer.valueToJSONString(zmiRmi.getFallbackContacts());
			}
			catch (Exception ex) {
				rmiResult = "Error:\n" + exceptionToString(ex);
			}
			
			WebClient.sendResponse(t, 200, rmiResult);
		}
	}
	
	private class SetFallbackContactsHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("POST")) {
				return;
			}
			
			String request = new Scanner(t.getRequestBody()).nextLine();
			Map<String, String> params = WebClient.queryToMap(request);
			
			String rmiResult = "OK";
			try {
				ValueSet contacts = (ValueSet) ZMIJSONSerializer.JSONStringToValue(params.get("contacts"));
				zmiRmi.setFallbackContacts(contacts);
			}
			catch (Exception ex) {
				rmiResult = "Error:\n" + exceptionToString(ex);
			}
			
			WebClient.sendResponse(t, 200, rmiResult);
		}
	}
	
	private class InstallQueryHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("POST")) {
				return;
			}
			
			String request = new Scanner(t.getRequestBody()).nextLine();
			Map<String, String> params = WebClient.queryToMap(request);
			String queryName = "&" + params.get("query-name");
			String queryText = params.get("query-value");

			String rmiResult = "OK";
			try {
				QueryOperation queryOp = QueryOperation.newQueryInstall(queryName, queryText);
				String signature = signerRmi.signQueryOperation(queryOp);

				List<Value> queryNamesRawList = Arrays.asList(new Value[] { new ValueString(queryName) });
				ValueList queryNames = new ValueList(queryNamesRawList, TypePrimitive.STRING);
				List<Value> queriesRawList = Arrays.asList(new Value[] { new ValueString(queryText) });
				ValueList queries = new ValueList(queriesRawList, TypePrimitive.STRING);
				List<Value> signaturesRawList = Arrays.asList(new Value[] { new ValueString(signature) });
				ValueList signatures = new ValueList(signaturesRawList, TypePrimitive.STRING);

				zmiRmi.installQueries(queryNames, queries, signatures);
			}
			catch (Exception ex) {
				rmiResult = "Error:\n" + exceptionToString(ex);
			}

			// ok, we are ready to send the response.
			WebClient.sendResponse(t, 200, rmiResult);
		}
	}
	
	private class UninstallQueryHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("POST")) {
				return;
			}
			
			String request = new Scanner(t.getRequestBody()).nextLine();
			Map<String, String> params = WebClient.queryToMap(request);
			String queryName = "&" + params.get("query-name");

			String rmiResult = "OK";
			try {
				QueryOperation queryOp = QueryOperation.newQueryUninstall(queryName);
				String signature = signerRmi.signQueryOperation(queryOp);

				List<Value> queryNamesRawList = Arrays.asList(new Value[] { new ValueString(queryName) });
				ValueList queryNames = new ValueList(queryNamesRawList, TypePrimitive.STRING);
				List<Value> signaturesRawList = Arrays.asList(new Value[] { new ValueString(signature) });
				ValueList signatures = new ValueList(signaturesRawList, TypePrimitive.STRING);

				zmiRmi.uninstallQueries(queryNames, signatures);
			}
			catch (Exception ex) {
				rmiResult = "Error:\n" + exceptionToString(ex);
			}

			// ok, we are ready to send the response.
			WebClient.sendResponse(t, 200, rmiResult);
		}
	}
	
	private class GetAllQueriesHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("GET")) {
				return;
			}
			
			String rmiResult;
			try {
				rmiResult = ZMIJSONSerializer.allQueriesToJSONString(zmiRmi.getAllQueries());
			}
			catch (Exception ex) {
				rmiResult = "Error:\n" + exceptionToString(ex);
			}
			
			WebClient.sendResponse(t, 200, rmiResult);
		}
	}
	
	// https://www.rgagnon.com/javadetails/java-have-a-simple-http-server.html
	private static class GetStaticFileHandler implements HttpHandler {		
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("GET")) {
				return;
			}

			String filepath = "www/" + t.getRequestURI().getPath().substring("/static/".length());
			File file = new File(filepath);
			byte [] bytearray  = new byte [(int)file.length()];
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bis.read(bytearray, 0, bytearray.length);

			// ok, we are ready to send the response.
			t.sendResponseHeaders(200, file.length());
			OutputStream os = t.getResponseBody();
			os.write(bytearray, 0, bytearray.length);
			os.close();
		}
	}
	
	// https://www.rgagnon.com/javadetails/java-get-url-parameters-using-jdk-http-server.html
	/**
	* returns the url parameters in a map
	* @param query
	* @return map
	*/
	private static Map<String, String> queryToMap(String query) {
		Map<String, String> result = new HashMap<>();
		if (query != null) {
			for (String param : query.split("&")) {
				String pair[] = param.split("=");
				try {
					if (pair.length > 1) {
						result.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
					}
					else {
						result.put(URLDecoder.decode(pair[0], "UTF-8"), "");
					}
				}
				catch (UnsupportedEncodingException ex) {}
			}
		}
		return result;
	}
	
	private static String exceptionToString(Exception ex) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		ex.printStackTrace(new PrintStream(out));
		return out.toString();
	}
	
	private static void sendResponse(HttpExchange t, int statusCode, String response) throws IOException  {
		byte [] responseBytes = response.getBytes();
		t.sendResponseHeaders(statusCode, responseBytes.length);
		OutputStream os = t.getResponseBody();
		os.write(responseBytes);
		os.close();
	}
}
