/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.rmiutils;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author mrowqa
 */
public class RmiWithAutoRecovery<T> {
	public final int CALL_RETRIES_CNT = 1;
	private final String registryHost;
	private final String stubName;
	private T rmi;
	
	public RmiWithAutoRecovery(String registryHost, String stubName) throws RemoteException, NotBoundException {
		this.registryHost = registryHost;
		this.stubName = stubName;
		reconnectRmi();
	}
	
	public <U> U callWithAutoRecovery(RmiCallClosure<T, U> closure) throws RemoteException {
		for (int retries = 0; ; retries++) {
			try {
				return closure.call(rmi);
			}
			catch (RemoteException ex) {
				Throwable ex1 = ex.getCause();
				if (ex1 != null && ex1 instanceof RmiCallException) {
					throw (RmiCallException) ex1;
				}
				
				if (retries == CALL_RETRIES_CNT) {
					throw new RemoteException("Cannot connect to RMI (tried " + (retries+1) + " times). Please try later.");
				}
				try {
					reconnectRmi();
				}
				catch (Exception ex2) {
				}
			}
		}
	}
	
	private void reconnectRmi() throws RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry(registryHost);
		this.rmi = (T) registry.lookup(stubName);
	}
}
