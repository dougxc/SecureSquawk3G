//if[FLASH_MEMORY]
/*
 * Created on 16-Nov-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sun.squawk.io.j2me.flash;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;

import com.sun.squawk.io.ConnectionBase;

/**
 *
 */
public class Protocol extends ConnectionBase implements InputConnection {

   private int memoryBase;

   public int getMemoryBase() {
      return memoryBase;
   }
   public InputStream openInputStream( ) {
      return new MemoryInputStream(this);
   }
   public Connection open(String protocol, String url, int mode, boolean timeouts) throws IOException {
      if (mode != Connector.READ) {
            throw new IOException("illegal mode: " + mode);
      }
      this.memoryBase = Integer.parseInt(url.substring(2),16);
      return this;
   }

   public class MemoryInputStream extends InputStream {
      private Protocol parent;
      private int currentMemoryPointer;

      public MemoryInputStream(Protocol protocol) {
         parent = protocol;
         currentMemoryPointer = 0;
      }

      public int read() throws IOException {
         int result =
            FlashObjectMemoryLoader.getByte(getCurrentAddress());
//         VM.println("reading a byte: at [" + getCurrentAddress() + "] value: [" + result + "]");
         currentMemoryPointer++;
         return result;
      }

      public long skip(long n) throws IOException {
         currentMemoryPointer = (int) (n + currentMemoryPointer);
         return n;
     }

      public int getCurrentAddress() {
         return parent.getMemoryBase() + currentMemoryPointer;
      }
   }
}

