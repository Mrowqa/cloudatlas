/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

import java.io.Serializable;

/**
 *
 * @author mrowqa
 */
public class TestCommunicationMessage extends ModuleMessage implements NetworkSendable, Serializable {
	private CommunicationInfo info;
	private byte[] testData;
	
	public TestCommunicationMessage(byte[] testData) {
		this.testData = testData;
	}
	
	public byte[] getTestData() {
		return testData;
	}

	@Override
	public CommunicationInfo getCommunicationInfo() {
		return info;
	}

	@Override
	public void setCommunicationInfo(CommunicationInfo info) {
		this.info = info;
	}
}
