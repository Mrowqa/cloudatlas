/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.time.Duration;
import org.json.JSONObject;
import pl.edu.mimuw.cloudatlas.agent.ConfigUtils;
import pl.edu.mimuw.cloudatlas.agent.ModulesHandler;
import pl.edu.mimuw.cloudatlas.model.PathName;

/**
 *
 * @author pawel
 */
public class ExchangeProcesConfig {
	// TODO change all fields to private and add getters
	public final PathName name;
	public final NodeSelector selector;
	
	public Duration disseminationInterval = Duration.ofSeconds(5);
	public Duration resendInterval = Duration.ofSeconds(5);
	public int maxRetry = 3;
	public boolean initializeDissemination = true;
	
	public ModulesHandler handler;
	
	public ExchangeProcesConfig(PathName name, NodeSelector selector) {
		this.name = name;
		this.selector = selector;
		this.handler = null;
	}
	
	public static ExchangeProcesConfig createConfig(PathName name, NodeSelector selector, JSONObject config) {
		ExchangeProcesConfig result = new ExchangeProcesConfig(name, selector);
		if (config == null)
			return result;
		if (config.has("initializeDissemination"))
			result.initializeDissemination = config.getBoolean("initializeDissemination");
		if (config.has("disseminationInterval"))
			result.disseminationInterval = ConfigUtils.parseInterval(config.getString("disseminationInterval"));
		if (config.has("disseminationResendInterval"))
			result.resendInterval = ConfigUtils.parseInterval(config.getString("disseminationResendInterval"));
		if (config.has("maxRetry"))
			result.maxRetry = config.getInt("maxRetry");
		return result;
	}
	
	public void setHandler(ModulesHandler handler) {
		this.handler = handler;
	}
}
