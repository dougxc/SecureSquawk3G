//if[FLASH_MEMORY]
/*
 * Created on 08-Nov-2004
 *
 */
package com.syn.squawk.arm.test;

import java.io.IOException;

import com.sun.squawk.vm.ChannelConstants;

/**
 * @author Dave
 *
 */
public class Switches {
   public static final int SW1 = 1;
   public static final int SW2 = 2;
   public static final int SW3 = 4;
   public static final int SW4 = 8;

   public int waitForSwitch(int mask) throws IOException {
      return VM.execIO(ChannelConstants.SW_READ, ChannelConstants.SW_READ, mask,0,0,0,0,0, null, null);
   }


}
