/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.webclient;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;
import pl.edu.mimuw.cloudatlas.agent.ConfigUtils;
import pl.edu.mimuw.cloudatlas.fetcher.Fetcher;
import pl.edu.mimuw.cloudatlas.model.ZMI;
import pl.edu.mimuw.cloudatlas.model.ZMIJSONSerializer;
import pl.edu.mimuw.cloudatlas.rmiutils.RmiWithAutoRecovery;

/**
 *
 * @author mrowqa
 */
public class HistoricalDataStorage extends Thread {
	private final RmiWithAutoRecovery<CloudAtlasInterface> zmiRmi;
	private final Duration sleepDuration;
	private final Duration storageLimit;
	private final List<DataEntry> entries = new ArrayList<>();
	private final JSONObject config;
	
	private class DataEntry {
		public Instant timestamp;
		public JSONArray data;
		
		public DataEntry(JSONArray data) {
			this.timestamp = Instant.now();
			this.data = data;
		}
	}
	
	public HistoricalDataStorage(JSONObject config) throws RemoteException, NotBoundException {
		this.config = config;
		
		this.zmiRmi = new RmiWithAutoRecovery<>(
				config.getString("agentRegistryHost"),
				"CloudAtlas" + config.getString("name"));
		this.sleepDuration = ConfigUtils.parseInterval(config.getString("dataDownloadInterval"));
		if (config.has("dataHistoryLimit")) {
			Object historyLimit = config.get("dataHistoryLimit");
			this.storageLimit = historyLimit != JSONObject.NULL ? ConfigUtils.parseInterval((String) historyLimit) : null;
		}
		else {
			this.storageLimit = null;
		}
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				ZMI zmi = zmiRmi.callWithAutoRecovery(rmi -> rmi.getWholeZMI());
				addNewEntry(new DataEntry(ZMIJSONSerializer.ZMIToJSON(zmi)));
				Logger.getLogger(Fetcher.class.getName()).log(Level.FINEST, "Added new data entry.");
			}
			catch (RemoteException ex) {
				Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE, "Call to rmi.getWholeZMI() failed.", ex);
			}

			try {
				Thread.sleep(sleepDuration.toMillis());
			}
			catch (InterruptedException ex) {}
		}
	}
	
	private synchronized void addNewEntry(DataEntry entry) {
		entries.add(entry);
		
		if (storageLimit != null) {
			Instant expirationTime = Instant.now().minus(storageLimit);
			while (!entries.isEmpty() && entries.get(0).timestamp.isBefore(expirationTime)) {
				entries.remove(0);
			}
		}
	}
	
	public synchronized String getHistoricalData(Integer limit) {
		String result = new String();
		
		int startIndex = 0;
		if (limit != null) {
			startIndex = Integer.max(0, entries.size() - limit);
		}
		
		JSONArray arr = new JSONArray();
		for (int i = startIndex; i < entries.size(); i++) {
			DataEntry entry = entries.get(i);
			JSONObject obj = new JSONObject();
			obj.put("ts", entry.timestamp.toString());
			obj.put("zmi", entry.data);
			arr.put(obj);
		}
		
		return arr.toString();
	}
}
