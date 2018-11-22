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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;

/**
 *
 * @author mrowqa
 */
public class WebClient {
	final HistoricalDataStorage dataStorage;
	private final static int httpPort = 8000;
	
	public WebClient(HistoricalDataStorage dataStorage) {
		this.dataStorage = dataStorage;
	}
	
	public void run() throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(httpPort), 0);		
		server.createContext("/history", new HistoryHandler());
		server.createContext("/static", new GetStaticFileHandler());
		server.createContext("/", new RedirectHandler("/static/index.html"));
		server.setExecutor(null); // creates a default executor
		server.start();
		System.out.println("Started webclient at http://localhost:" + httpPort + "/");
		
		// content types:
		// https://stackoverflow.com/questions/23714383/what-are-all-the-possible-values-for-http-content-type-header
		// text/css    
		// text/html    
		// text/javascript (obsolete) 
	}
	
	class RedirectHandler implements HttpHandler {
		String location;
		
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
	
	class HistoryHandler implements HttpHandler {
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
			
			byte [] response = dataStorage.getHistoricalData(limit).getBytes();
			t.sendResponseHeaders(200, response.length);
			OutputStream os = t.getResponseBody();
			os.write(response);
			os.close();
		}
	}
	
	// https://www.rgagnon.com/javadetails/java-have-a-simple-http-server.html
	static class GetStaticFileHandler implements HttpHandler {		
		@Override
		public void handle(HttpExchange t) throws IOException {
			if (!t.getRequestMethod().equals("GET")) {
				return;
			}
			
			//Headers h = t.getResponseHeaders();
			//h.add("Content-Type", this.contentType);

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
	public static Map<String, String> queryToMap(String query) {
		Map<String, String> result = new HashMap<>();
		if (query != null) {
			for (String param : query.split("&")) {
				String pair[] = param.split("=");
				if (pair.length > 1) {
					result.put(pair[0], pair[1]);
				}
				else {
					result.put(pair[0], "");
				}
			}
		}
		return result;
	}
}
