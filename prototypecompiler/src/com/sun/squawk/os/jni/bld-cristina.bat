cl -IC:\j2sdk1.4.2_02\include -IC:\j2sdk1.4.2_02\include\win32 /c prototypecompiler\src\com\sun\squawk\os\jni\CSystem.c prototypecompiler\src\com\sun\squawk\os\jni\dispatch_x86.c
link.exe -nologo -debug -dll -out:CSystem.dll CSystem.obj dispatch_x86.obj
