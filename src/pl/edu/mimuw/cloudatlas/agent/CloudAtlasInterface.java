/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.rmi.Remote;
import java.rmi.RemoteException;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;

/**
 *
 * @author pawel
 */
public interface CloudAtlasInterface extends Remote {
	public ValueList getZones()throws RemoteException;
	
	/**
	 * Returns attributes map for given zone.
	*/
	public AttributesMap getAttributes(ValueString zone) throws RemoteException; 
	
	public void installQueries(ValueList queryNames, ValueList queries) throws RemoteException;
	
	public void uninstallQueries(ValueList queryNames) throws RemoteException;
	
	public void setValue(ValueString zone, ValueString attribute, Value value) throws RemoteException;
	
	public void setFallbackContacts(ValueSet contacts) throws RemoteException;
	
	public ValueSet getFallbackContacts() throws RemoteException;
}
