/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package features.dynamic;

import java.util.Random;

/**
 *
 * @author falk
 */
public class Input {
  
  public void foo() {
    Random r = new Random();
    if (r.nextInt() < 500) {
      if (r.nextBoolean()) {
        assert false;        
      }
    }
  }
  
  public static void main(String[] args) {
    Input i = new Input();
    i.foo();
  }
  
}
