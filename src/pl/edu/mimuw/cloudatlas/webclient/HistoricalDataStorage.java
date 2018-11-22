/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.webclient;

import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;
import pl.edu.mimuw.cloudatlas.fetcher.Fetcher;
import pl.edu.mimuw.cloudatlas.model.ZMI;
import pl.edu.mimuw.cloudatlas.model.ZMIJSONSerializer;

/**
 *
 * @author mrowqa
 */
public class HistoricalDataStorage extends Thread {
	private CloudAtlasInterface rmi;
	private Duration sleepDuration;
	private Duration storageLimit;
	private List<DataEntry> entries = new ArrayList<>();
	
	private class DataEntry {
		public Instant timestamp;
		public String data;
		
		public DataEntry(String data) {
			this.timestamp = Instant.now();
			this.data = data;
		}
	}
	
	public HistoricalDataStorage(CloudAtlasInterface rmi, Duration sleepDuration, Duration storageLimit) {
		this.rmi = rmi;
		this.sleepDuration = sleepDuration;
		this.storageLimit = storageLimit;
	}
	
	public void run() {
		while (true) {
			try {
				ZMI zmi = rmi.getWholeZMI();
				addNewEntry(new DataEntry(ZMIJSONSerializer.toJSON(zmi)));
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
		
		result += "[";
		for (int i = startIndex; i < entries.size(); i++) {
			DataEntry entry = entries.get(i);
			result += "{\"ts\":\"" + entry.timestamp.toString()
					+ "\",\"zmi\":" + entry.data + "}"
					+ (i + 1 < entries.size() ? "," : "");
		}
		result += "]";
		
		return result;
	}
}
