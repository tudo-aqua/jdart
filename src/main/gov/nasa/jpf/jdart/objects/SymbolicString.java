/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.jpf.jdart.objects;

import gov.nasa.jpf.constraints.api.Expression;

/**
 *
 * @author falk
 */
public class SymbolicString {
  
  private final Expression<Integer> symbolicLength;
  
  private final Expression[] symbolicChars;

  public SymbolicString(Expression<Integer> symbolicLength, Expression[] symbolicChars) {
    this.symbolicLength = symbolicLength;
    this.symbolicChars = symbolicChars;
  }

  public Expression<Integer> getSymbolicLength() {
    return symbolicLength;
  }

  public Expression[] getSymbolicChars() {
    return symbolicChars;
  }
  
}
