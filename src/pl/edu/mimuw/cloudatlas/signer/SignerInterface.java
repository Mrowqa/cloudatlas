/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.signer;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author mrowqa
 */
public interface SignerInterface extends Remote {
	public byte[] signQueryOperation(QueryOperation query) throws RemoteException;
}
