#!sh
javac CodeGen.java
java -cp . CodeGen 1 > ../define/src/com/sun/squawk/vm/OPC.java
java -cp . CodeGen 2 > ../define/src/com/sun/squawk/vm/Mnemonics.java
java -cp . CodeGen 3 > ../slowvm/src/vm/switch.c
java -cp . CodeGen 4 > ../translator/src/com/sun/squawk/translator/ir/Verifier.java
java -cp . CodeGen 5 > ../vmgen/src/com/sun/squawk/vm/JitterSwitch.java
java -cp . CodeGen 6 > ../vmgen/src/com/sun/squawk/vm/InterpreterSwitch.java
java -cp . CodeGen 7 > ../vmgen/src/com/sun/squawk/vm/BytecodeRoutines.java
rm *.class
