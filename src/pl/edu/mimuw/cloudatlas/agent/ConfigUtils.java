/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.time.Duration;

/**
 *
 * @author pawel
 */
public class ConfigUtils {
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
