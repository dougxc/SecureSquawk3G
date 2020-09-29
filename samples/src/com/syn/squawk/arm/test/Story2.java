//if[FLASH_MEMORY]
package com.syn.squawk.arm.test;

import java.io.IOException;

/**
  Simple test of ARM VM
  */
public class Story2 {

    public static void main(String args[]) {
        (new Story2()).doIt();
    }

    private void doIt() {
        //VM.println("ARMTest starting");
        try {
            Leds theLeds = new Leds();
            theLeds.turnOffAllLeds();
            int i = 0;
            int j = 0;
            while (i < 5002) {
                j = i * i;
                i++;
            }
            i = 0;
            theLeds.turnOnLeds(Leds.LED1);
            while (i < 5000) {
                j = i * i;
                i++;
            }
            i = 0;
            theLeds.turnOnLeds(Leds.LED2);
            while (i < 5000) {
                j = i * i;
                i++;
            }
            i = 0;
            theLeds.turnOnLeds(Leds.LED3);
            while (i < 1000) {
                j = i * i;
                i++;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
   }
}
