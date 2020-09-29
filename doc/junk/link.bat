rem cl /IC:\j2sdk1.4.2_01\include /Islowvm\src\rts\msc /Zi /DTRACE %1 %2 %3 %4 %5 %6 slowvm/src/vm/squawk.c

@echo off
cl /IC:\j2sdk1.4.2_01\include /Islowvm\src\rts\msc /FAs /Ogityb2 /Gs %1 %2 %3 %4 %5 %6 slowvm/src/vm/squawk.c
