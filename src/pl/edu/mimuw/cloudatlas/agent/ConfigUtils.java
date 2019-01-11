/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import org.json.JSONObject;

/**
 *
 * @author pawel
 */
public class ConfigUtils {
	public static JSONObject getConfigObjectFromFile(String file) throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(file)));
		return new JSONObject(content);
	}
	
	public static Duration parseInterval(String interval) {
		char durationUnit = interval.charAt(interval.length() - 1);
		int durationValue = Integer.parseInt(interval.substring(0, interval.length() - 1));
		switch(durationUnit) {
			case 'h': 
				return Duration.ofHours(durationValue); 
			case 'm':
				return Duration.ofMinutes(durationValue); 
			case 's':
				return Duration.ofSeconds(durationValue); 
			default:
				throw new IllegalArgumentException("Invalid duration unit, got " + durationUnit + " allowed h|m|s.");
		}
	}
}
