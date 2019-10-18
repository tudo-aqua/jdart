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
package features.strings;

import java.util.Random;

public class Input {
  
  public static void main(String args[]) {

    Random r = new Random();
    int len = r.nextInt();    
    byte[] bytes = new byte[len];
    r.nextBytes(bytes);    
    String s = new String(bytes);
    
    int idx = r.nextInt();
    
    if(s.charAt(idx) == 'A') {
      assert false;
    }
    
  }
}
