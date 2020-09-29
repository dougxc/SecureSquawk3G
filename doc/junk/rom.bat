java -jar build.jar romize -timer -cp j2me\j2meclasses;translator\j2meclasses;tck\tck.jar j2me\j2meclasses translator\j2meclasses

@echo off
rem java -jar build.jar romize -timer -cp j2me\j2meclasses;graphics\j2meclasses;samples\j2meclasses;translator\j2meclasses;tck\tck.jar j2me\j2meclasses graphics\j2meclasses samples\j2meclasses
rem java -jar build.jar romize -little -cp j2me\j2meclasses;graphics\j2meclasses;samples\j2meclasses;translator\j2meclasses;tck\tck.jar -traceir1 -traceverifier j2me\j2meclasses  >xx.x
rem java -jar build.jar romize -little -cp j2me\classes;graphics\classes;samples\classes;translator\classes;tck\tck.jar j2me\classes graphics\classes samples\classes >xx.x
rem java -jar build.jar romize -little -cp j2me\classes;translator\classes;tck\tck.jar -traceir0  j2me\classes translator\classes >xx.x
rem java -jar build.jar romize -little -cp j2me\classes;translator\classes -nometadata j2me\classes translator\classes
rem -nometadata -tracemethods -traceclassinfo
rem java -jar build.jar romize -little -cp j2me\classes -nometadata -tracemethods -traceclassinfo j2me\classes  >xx.x
rem java -jar build.jar romize -little -cp j2me\classes.zip -traceir0 -traceir1 -tracemethods j2me\classes.zip  >xx.x
rem java -jar build.jar romize -little -cp j2me\classes;samples\classes  -tracemethods j2me\classes samples\classes  >xx.x
