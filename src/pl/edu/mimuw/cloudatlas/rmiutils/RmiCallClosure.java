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
public interface RmiCallClosure<T, U> {
	public U call(T rmi) throws RemoteException;
}
