mkdir temp
rm -r -f temp/distribute
mkdir temp/distribute
cp * temp/distribute
#cp -r bco                   temp/distribute
cp -r builder               temp/distribute
cp -r bytecodes             temp/distribute
cp -r compiler              temp/distribute
cp -r CVS                   temp/distribute
cp -r define                temp/distribute
#cp -r doc                   temp/distribute
#cp -r graphics              temp/distribute
cp -r j2me                  temp/distribute
cp -r j2se                  temp/distribute
#cp -r jasmin                temp/distribute
#cp -r jbuilder              temp/distribute
cp -r mapper                temp/distribute
cp -r prototypecompiler     temp/distribute
cp -r romizer               temp/distribute
#cp -r samples               temp/distribute
cp -r slowvm                temp/distribute
#cp -r tck                   temp/distribute
cp -r tools                 temp/distribute
cp -r translator            temp/distribute
cp -r vmgen                 temp/distribute

rm temp/distribute/j2me/src/java/util/Date.java
rm temp/distribute/j2me/src/java/util/Calendar.java
rm temp/distribute/j2me/src/java/util/TimeZone.java
rm temp/distribute/j2me/src/com/sun/cldc/util/TimeZoneImplementation.java
rm temp/distribute/j2me/src/com/sun/cldc/util/j2me/CalendarImpl.java
rm temp/distribute/j2me/src/com/sun/cldc/util/j2me/TimeZoneImpl.java
