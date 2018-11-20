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
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;

/**
 *
 * @author mrowqa
 */
public class WebClient {
	private CloudAtlasInterface rmi;
	private Duration sleepDuration;
	private static int httpPort = 8000;
	
	public WebClient(CloudAtlasInterface rmi, Duration sleepDuration) {
		this.rmi = rmi;
		this.sleepDuration = sleepDuration;
	}
	
	public void run() throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(httpPort), 0);		
		server.createContext("/test", new MyHandler());
		server.createContext("/", new GetStaticFileHandler("www/index.html", "text/html"));
		server.setExecutor(null); // creates a default executor
		server.start();
		
		// content types:
		// https://stackoverflow.com/questions/23714383/what-are-all-the-possible-values-for-http-content-type-header
		// text/css    
		// text/html    
		// text/javascript (obsolete) 
	}
	
	static class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			byte [] response = "Welcome Real's HowTo test page".getBytes();
			t.sendResponseHeaders(200, response.length);
			OutputStream os = t.getResponseBody();
			os.write(response);
			os.close();
		}
	}
	
	// https://www.rgagnon.com/javadetails/java-have-a-simple-http-server.html
	class GetStaticFileHandler implements HttpHandler {
		String filePath;
		String contentType;
		
		public GetStaticFileHandler(String filePath, String contentType) {
			this.filePath = filePath;
			this.contentType = contentType;
		}
		
		public void handle(HttpExchange t) throws IOException {
			Headers h = t.getResponseHeaders();
			h.add("Content-Type", this.contentType);

			File file = new File(this.filePath);
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
}
