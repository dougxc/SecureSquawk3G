//if[FLASH_MEMORY]
/*
 * Created on Oct 27, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.syn.squawk.arm.test;

import java.io.IOException;

import com.sun.squawk.vm.ChannelConstants;

/**
 * @author danielsj
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Leds {

    public static final int LED1 = 1;
    public static final int LED2 = 2;
    public static final int LED3 = 4;
    public static final int LED4 = 8;
    public static final int LED5 = 16;

    int ledChannelNumber;

    public Leds() throws IOException {
        ledChannelNumber = VM.getChannel(ChannelConstants.CHANNEL_LED);
    }

   public int turnOffAllLeds() throws IOException {
      return turnOffLeds(0xFF);
   }

   public int turnOffLeds(int mask) throws IOException {
      return VM.execIO(ChannelConstants.LED_OFF, ledChannelNumber, mask,0,0,0,0,0, null, null);
   }

   public int turnOnLeds(int mask) throws IOException {
      return VM.execIO(ChannelConstants.LED_ON, ledChannelNumber, mask,0,0,0,0,0, null, null);
   }

   public void flashLeds() throws IOException {
      int result;
      for (int ledMask=1; ledMask<0x10; ledMask<<=1 ) {
         try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            VM.println("Sleep interrupted");
        }
         result = turnOnLeds(ledMask);
         if (result != 0) {
              VM.println("Bad led_on result code: " + result);
              return;
         }
      }
   }
}
