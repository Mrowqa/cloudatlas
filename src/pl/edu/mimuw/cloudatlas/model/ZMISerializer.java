/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


// tried:
// - Kryo - problem with collections
// - FST (https://github.com/RuedigerMoeller/fast-serialization) - Java can't create fst configuration (class/object finder fails)
//   -> https://github.com/RuedigerMoeller/fast-serialization/wiki/Serialization
//   -> https://github.com/RuedigerMoeller/fast-serialization/wiki/MinBin


/**
 *
 * @author mrowqa
 */
public class ZMISerializer {
	public static byte [] SerializeZMI(ZMI zmi) {
		try {
			ByteArrayOutputStream out1 = new ByteArrayOutputStream();
			ObjectOutputStream out2 = new ObjectOutputStream(out1);
			out2.writeObject(zmi);
			out2.close(); // should flush
			out1.close();
			return out1.toByteArray();
		}
		catch (IOException ex) {
			return null;
		}
	}
	
	public static ZMI DeserializeZMI(byte[] serialized) {
		try {
			ByteArrayInputStream in1 = new ByteArrayInputStream(serialized);
			ObjectInputStream in2 = new ObjectInputStream(in1);
			Object obj = in2.readObject();
			in2.close();
			in1.close();
			if (obj instanceof ZMI) {
				return (ZMI) obj;
			}
		}
		catch (IOException | ClassNotFoundException ex) {}
		return null;
	}
}
