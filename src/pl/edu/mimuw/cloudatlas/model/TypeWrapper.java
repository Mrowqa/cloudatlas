/**
 * Copyright (c) 2014, University of Warsaw
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package pl.edu.mimuw.cloudatlas.model;

import java.io.Serializable;


/**
 * Represents a collection type with specified element type.
 * 
 * @see TypePrimitve
 */
public class TypeWrapper extends Type implements Serializable {
	private final Type nestedType;
	
	/**
	 * Creates a new collection type.
	 * 
	 * @param nestedType a type of an element of this wrapper; may be a complex type (for instance
	 * <code>TypeCollection</code>)
	 */
	public TypeWrapper(Type nestedType) {
		super(Type.PrimaryType.WRAPPER);
		this.nestedType = nestedType;
	}
	
	/**
	 * Gets a type of element stored in this wrapper.
	 * 
	 * @return type of element in this wrapper
	 */
	public Type getNestedType() {
		return nestedType;
	}
	
	/**
	 * Returns a friendly textual representation of this type.
	 * 
	 * @return a textual representation of this type
	 */
	@Override
	public String toString() {
		return getPrimaryType().toString() + " of (" + nestedType.toString() + ")";
	}
	
	@Override
	public boolean isCompatible(Type type) {
		return super.isCompatible(type)
				|| (Type.PrimaryType.WRAPPER == type.getPrimaryType() && nestedType
						.isCompatible(((TypeWrapper)type).nestedType));
	}
}
