/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.signer;

import java.io.Serializable;

/**
 *
 * @author mrowqa
 */
public class QueryOperation implements Serializable {
	public enum Operation {
		QUERY_INSTALL, QUERY_UNINSTALL,
	};
	
	private String name;
	private String text;
	private Operation operation;
	
	private QueryOperation() {
		// disallowed from outside
	}
	
	public static QueryOperation newQueryInstall(String name, String text) {
		QueryOperation op = new QueryOperation();
		op.name = name;
		op.text = text;
		op.operation = Operation.QUERY_INSTALL;
		return op;
	}
	
	public static QueryOperation newQueryUninstall(String name) {
		QueryOperation op = new QueryOperation();
		op.name = name;
		op.text = null;
		op.operation = Operation.QUERY_UNINSTALL;
		return op;
	}
	
	public String getName() {
		return name;
	}
	
	public String getText() {
		return text;
	}
	
	public Operation getOperation() {
		return operation;
	}
}
