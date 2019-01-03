/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

/**
 *
 * @author pawel
 */
public interface NetworkSendable {
	CommunicationInfo getCommunicationInfo();
	void setCommunicationInfo(CommunicationInfo info);
}
