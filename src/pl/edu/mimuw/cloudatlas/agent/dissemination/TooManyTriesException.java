/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

/**
 *
 * @author pawel
 */
public class TooManyTriesException extends Exception {
    public TooManyTriesException(String errorMessage) {
        super(errorMessage);
    }
}
