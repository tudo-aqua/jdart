/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package features.strings;

import java.util.Random;

/**
 *
 * @author falk
 */
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
