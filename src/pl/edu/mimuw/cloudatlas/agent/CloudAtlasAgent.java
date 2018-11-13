/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.edu.mimuw.cloudatlas.interpreter.AttributesExtractor;
import pl.edu.mimuw.cloudatlas.interpreter.Interpreter;
import pl.edu.mimuw.cloudatlas.interpreter.InterpreterException;
import pl.edu.mimuw.cloudatlas.interpreter.QueryResult;
import pl.edu.mimuw.cloudatlas.interpreter.query.Yylex;
import pl.edu.mimuw.cloudatlas.interpreter.query.parser;
import pl.edu.mimuw.cloudatlas.model.Attribute;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.Type;
import pl.edu.mimuw.cloudatlas.model.TypeCollection;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class CloudAtlasAgent implements CloudAtlasInterface {
	private ZMI zmi;
	private ValueSet fallbackContacts = new ValueSet(new HashSet<Value>(), TypePrimitive.STRING);

	public CloudAtlasAgent(ZMI _zmi) {
		zmi = _zmi;
	}
	
	@Override
	public ValueList getZones() throws RemoteException {
		return zmi.getZones();
	}

	@Override
	public AttributesMap getAttributes(ValueString zone) throws RemoteException {
		return findZone(zmi, new PathName(zone.getValue())).getAttributes();
	}

	@Override
	public void installQuery(ValueString query) throws RemoteException {
		try {
			executeQueries(zmi, query.getValue());
		} catch (Exception ex) {
			throw new RemoteException("Failed to install query: " + ex.getMessage());
		}
	}

	@Override
	public void uninstallQuery(ValueString query) throws RemoteException {
		AttributesExtractor extractor = new AttributesExtractor(zmi);
		Yylex lex = new Yylex(new ByteArrayInputStream(query.getValue().getBytes()));
		Set<String> attributes;
		try {
			attributes = extractor.extractAttributes((new parser(lex)).pProgram());
			removeAttributes(zmi, attributes);
		} catch (Exception ex) {
			Logger.getLogger(CloudAtlasAgent.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	// TODO handle exceptions.
	// TODO consider syncronisation
	@Override
	public void setValue(ValueString zone, ValueString attribute, Value value) throws RemoteException {
		ZMI zoneZmi = findZone(zmi, new PathName(zone.getValue()));
		zoneZmi.getAttributes().addOrChange(attribute.getValue(), value);
	}

	@Override
	public void setFallbackContacts(ValueSet contacts) throws RemoteException {
		if (contacts.isNull()) {
			throw new RemoteException("Fallback contacts set can't be null");
		}
		Type elementType = ((TypeCollection)contacts.getType()).getElementType();
		if (elementType != TypePrimitive.STRING) {
			throw new RemoteException("Illegal set element type. Got " + TypePrimitive.STRING + " expected " + elementType);
		}
		fallbackContacts = contacts;
	}
	
	private static void removeAttributes(ZMI zmi, Set<String> attributes) {
		if (!zmi.getSons().isEmpty()) {
			for (String attribute : attributes)
				zmi.getAttributes().remove(attribute);
			for (ZMI son : zmi.getSons())
				removeAttributes(son, attributes);
		}
	}
	
	private static void executeQueries(ZMI zmi, String query) throws Exception {
		if(!zmi.getSons().isEmpty()) {
			for(ZMI son : zmi.getSons())
				executeQueries(son, query);
			Interpreter interpreter = new Interpreter(zmi);
			Yylex lex = new Yylex(new ByteArrayInputStream(query.getBytes()));
			try {
				List<QueryResult> result = interpreter.interpretProgram((new parser(lex)).pProgram());
				for (QueryResult r : result) {
					zmi.getAttributes().addOrChange(r.getName(), r.getValue());
				}
			} catch(InterpreterException exception) {
				//System.err.println("Interpreter exception on " + getPathName(zmi) + ": " + exception.getMessage());
			}
		}
	}

	private ZMI findZone(ZMI zmi, PathName pathName) throws RemoteException {
		if (pathName.getComponents().isEmpty()) {
			return zmi;
		}
		String currentName = pathName.getComponents().get(0);
		for (ZMI son : zmi.getSons()) {
			PathName sonPathName = son.getPathName();
			if (!sonPathName.getComponents().isEmpty() && sonPathName.getSingletonName().equals(currentName)) {
				return findZone(son, pathName.consumePrefix());
			}
		}
		throw new RemoteException("Zone not found.");
	}
}
