//if[FLASH_MEMORY]
/*
 * Created on 17-Nov-2004
 *
 */
package java.lang;

import java.io.InputStream;

import com.sun.squawk.util.BitSet;
import com.sun.squawk.io.j2me.flash.Protocol;

/**
 * @author Dave
 *
 */
public class FlashObjectMemoryLoader extends ObjectMemoryLoader {

   private int objectMemorySize;

   private Address memoryAddress;

   /**
    * @param reader
    * @param loadIntoReadOnlyMemory
    */
   public FlashObjectMemoryLoader(ObjectMemoryReader reader, boolean loadIntoReadOnlyMemory) {
      super(reader, loadIntoReadOnlyMemory);
   }

   public static ObjectMemory load0(String url, boolean loadIntoReadOnlyMemory) {
      return null;
   }

   protected byte[] loadMemory(ObjectMemory parent, int size) {
      // record the current address of the reader (this is the location of memory)
      // round up to a word boundary
      memoryAddress = ((FlashObjectMemoryReader)reader).getCurrentAddressRoundedToWordBoundary();

      // skip ahead size bytes to simulate reading that off the suite file
      reader.skip(size,"simulating flash memory load");
      byte[] dummy = {1, 2, 3, 4}; //return a dummy value to allow our hash to be (wrongly) calculated
      return dummy;
  }

   protected Address relocateMemory(ObjectMemory parent, byte[] buffer, BitSet oopMap) {

      // Return the previously cached address
      return memoryAddress;
  }

   protected BitSet loadOopMap(int size) {
      //no-op: there is no oopmap in a rom-ized suite file
      return null;
   }

   /**
    * @param i
    * @return
    */
   public static int getByte(int i) {
      int signedValue = Unsafe.getByte(Address.zero().add(i), 0);
      return signedValue & 0xff;
   }

}


class FlashObjectMemoryReader extends ObjectMemoryReader {
   private Protocol.MemoryInputStream mis;
   /**
    * Creates a <code>ObjectMemoryReader</code> that reads object memory file components
    * from a given input stream.
    *
    * @param   in        the input stream
    * @param   filePath  the file from which <code>in</code> was created
    */
   public FlashObjectMemoryReader(InputStream in, String filePath) {
       super(in, filePath);
       //cache the underlying input stream - we will want to talk to that.
       mis = (Protocol.MemoryInputStream)in;
   }

   public Address getCurrentAddressRoundedToWordBoundary() {
        return Address.zero().add(4 * ((mis.getCurrentAddress() + 3) / 4));
   }

//   public final void readEOF() {
//      // no-op
//   }
}
