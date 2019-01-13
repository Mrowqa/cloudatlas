/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.rmiutils;

import java.rmi.RemoteException;

/**
 *
 * @author mrowqa
 */
public class RmiCallException extends RemoteException {
	public RmiCallException(String msg) {
		super(msg);
	}
	
	public RmiCallException(String msg, Throwable reason) {
		super(msg, reason);
	}
}
