JAVAC=javac
if [ $# -gt 0 ]; then
    JAVAC=$1/bin/javac
fi;

$JAVAC -target 1.2 -source 1.3 -d . -g *.java ../j2se/src/com/sun/squawk/util/Find.java
jar cfm ../build.jar MANIFEST.MF *.class com/sun/squawk *.xml
rm *.class
