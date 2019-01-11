/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.model;

import java.io.Serializable;
import java.time.Duration;

/**
 *
 * @author pawel
 */
public class Query implements Serializable {
	private String text;
	private String signature;
	private ValueTime freshness;

	public Query(String text, String signature) {
		this(text, signature, ValueTime.now());
	}
	
	public Query(String text, String signature, ValueTime freshness) {
		this.text = text;
		this.signature = signature;
		this.freshness = freshness;
	}
	
	public Query adjustTime(Duration diff) {
		return new Query(text, signature, freshness.adjustTime(diff));
	}
	
	public ValueTime getFreshness() {
		return freshness;
	}
	
	public String getText() {
		return text;
	}

	public String getSignature() {
		return signature;
	}

	public Query getFresher(Query query) {
		if (freshness.isLowerThan(query.freshness).getValue()) {
			return query;
		}
		return this;
	}
}
