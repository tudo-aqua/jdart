/*
 * Copyright 2019 TU Dortmund, Falk Howar (@fhowar)
 *
 *
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
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
