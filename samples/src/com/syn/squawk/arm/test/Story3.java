//if[FLASH_MEMORY]
/*
 * Created on Nov 1, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.syn.squawk.arm.test;

import java.io.IOException;

/**
 * @author danielsj
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Story3 {

   boolean pressed = false;
   Leds theLeds;

    public static void main(String args[]) {
        Story3 theInstance = new Story3();
      //theInstance.runXTests();
        theInstance.runTimerTests();
        theInstance.runChannelTests();
    }

    private void runTimerTests() {
        VM.println("Starting story 3 timer tests");
        VM.println("About to sleep for 5 secs...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            VM.println("Sleep was interrupted");
        }
        VM.println("Woke up!");
    }
    private void runXTests() {
        VM.println("ARMTest Story3 via VM.println");
        System.out.println("ARMTest Story3 via System.out.println");
        Test.runXTests();
    }
    private void runChannelTests() {
       VM.println("Starting story 3 channel tests");

       // Use channel IO to flash LEDs in a sequence
       try {
          theLeds = new Leds();

          int result = theLeds.turnOffAllLeds();
          if (result != 0) {
               VM.println("Bad led_on result code: " + result);
               return;
          }

          for (int ledMask=1; ledMask<0x10; ledMask<<=1 ) {
             //TODO sleep for a couple of seconds
             result = theLeds.turnOnLeds(ledMask);
             if (result != 0) {
                  VM.println("Bad led_on result code: " + result);
                  return;
             }
          }

          VM.println("Waiting for SW1 to be pressed");
          // Use blocking channel IO to read switch input
          Switches theSwitches = new Switches();

          result = theSwitches.waitForSwitch(Switches.SW1);
          if (result != 0) {
             VM.println("Bad sw_read result code: " + result);
             return;
          }
          result = theLeds.turnOnLeds(Leds.LED5);
          if (result != 0) {
               VM.println("Bad led_on result code: " + result);
               return;
          }

          VM.println("Starting button flasher thread");

          pressed = false;

          // Use blocking channel IO to read switch input while using separate thread to flash LEDs
          Thread buttonFlasher = new Thread() { //Begin class definition
               public void run() {
                  while (!pressed) {
                     try {
                        int result = theLeds.turnOffAllLeds();
                        if (result != 0) {
                             VM.println("Bad led_on result code: " + result);
                             return;
                        }
                        theLeds.flashLeds();
                     } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                     }
                     yield();
                  }
               }
          };
          buttonFlasher.start();

          VM.println("Starting to wait for SW2");
          result = theSwitches.waitForSwitch(Switches.SW2);
          if (result != 0) {
             VM.println("Bad sw_read result code: " + result);
             return;
          }
          VM.println("Detected SW2");

          pressed = true;

          try {
            buttonFlasher.join();
         } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }
          result = theLeds.turnOffAllLeds();
          if (result != 0) {
               VM.println("Bad led_on result code: " + result);
               return;
          }
          VM.println("Turned off LEDs - story 3 channel tests completed.");



      } catch (IOException e) {
         VM.println("Encountered IOException " + e.getMessage());
         return;
      }
    }
}



