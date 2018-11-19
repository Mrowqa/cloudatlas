/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.fetcher;

import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.nashorn.internal.codegen.types.Type;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.ValueDouble;
import pl.edu.mimuw.cloudatlas.model.ValueInt;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;

/**
 *
 * @author mrowqa
 */
public class Fetcher extends Thread {
	private final Duration updateInterval;
	private final CloudAtlasInterface rmi;
	private final ValueString zone;

	public Fetcher(Duration updateInterval, CloudAtlasInterface rmi, String zone) {
		this.updateInterval = Duration.ofMillis(updateInterval.toMillis()); // clone
		this.rmi = rmi;
		this.zone = new ValueString(zone);
	}

	@Override
	public void run() {
		while (true) {
			AttributesMap data = collectData();
			try {
				rmi.setZoneAttributes(zone, data);
				Logger.getLogger(Fetcher.class.getName()).log(Level.FINEST, "Data collected & set.");
			}
			catch (RemoteException ex) {
				System.err.println();
				Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE, "Call to rmi.setZoneAttributes() failed.", ex);
			}

			try {
				Thread.sleep(updateInterval.toMillis());
			}
			catch (InterruptedException ex) {}
		}
	}

	public static AttributesMap collectData() { // public, so it can be called as "a test"
		AttributesMap attrs = new AttributesMap();

		OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

		long num_cores = os.getAvailableProcessors();
		double cpu_load = os.getSystemLoadAverage() / num_cores; // could be >100% http://blog.scoutapp.com/articles/2009/07/31/understanding-load-averages
		attrs.add("cpu_load", new ValueDouble(cpu_load >= 0 ? cpu_load : null));
		attrs.add("num_cores", new ValueInt(num_cores));

		File diskRoot = new File("/");
		attrs.add("free_disk", new ValueInt(diskRoot.getFreeSpace()));
		attrs.add("total_disk", new ValueInt(diskRoot.getTotalSpace()));

		attrs.add("free_ram", new ValueInt(os.getFreePhysicalMemorySize()));
		attrs.add("total_ram", new ValueInt(os.getTotalPhysicalMemorySize()));
		attrs.add("free_swap", new ValueInt(os.getFreeSwapSpaceSize()));
		attrs.add("total_swap", new ValueInt(os.getTotalSwapSpaceSize()));
		attrs.add("kernel_ver", new ValueString(os.getVersion()));

		{
			String[] cmd = {"bash", "-c", "ps aux | wc -l"};
			ValueInt val = runCommandGetValueInt(cmd);
			attrs.add("num_processes", val);
		}

		{
			String[] cmd = {"bash", "-c", "users | wc -w"};
			ValueInt val = runCommandGetValueInt(cmd);
			attrs.add("logged_users", val);
		}

		{
			String[] cmd = {"hostname"};
			String cmdResult = runCommand(cmd);
			ValueSet valSet = new ValueSet(TypePrimitive.STRING);
			valSet.add(new ValueString(cmdResult));
			attrs.add("dns_names", valSet);
		}

		return attrs;
	}

	private static String runCommand(String[] cmd) {
		try {
			Process p = new ProcessBuilder(cmd).start();
			if (!p.waitFor(1, TimeUnit.MINUTES)) {
				throw new InterruptedException("process halted!");
			}
			return new Scanner(p.getInputStream()).next();
		}
		catch(IOException | InterruptedException | NoSuchElementException ex) {}
		return null;
	}

	private static ValueInt runCommandGetValueInt(String[] cmd) {
		String cmdResult = runCommand(cmd);
		ValueInt val = new ValueInt(null);
		if (cmdResult != null) {
			try {
				long num = Integer.parseInt(cmdResult);
				val = new ValueInt(num);
			}
			catch (NumberFormatException ex) {}
		}

		return val;
	}
}
