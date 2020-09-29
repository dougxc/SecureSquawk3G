# This script should be run as follows:
# 1, delete /define/src/com/sun/squawk/vm/Native.java
# 2, bld j2me
# 3, run this script
# 4, bld j2me a second time

javac NativeGen.java
java -cp . -Xbootclasspath/a:../j2me/classes NativeGen 0 > tmp && mv tmp ../define/src/com/sun/squawk/vm/Native.java
java -cp . -Xbootclasspath/a:../j2me/classes NativeGen 1 > tmp && mv tmp ../vmgen/src/com/sun/squawk/vm/InterpreterNative.java
rm *.class
