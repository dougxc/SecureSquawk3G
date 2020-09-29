src=slowvm/src/vm/squawk.c
cpu=`uname -m`
def64=""
archflag=""

test -n "`cat build.properties | grep 'SQUAWK_64=true'`"
is64=$?

if [ "x$cpu" = "xsun4u" ]; then
  comp="cc"
  if [ $is64 -eq 0 ]; then
    def64="-DSQUAWK_64=true"
    archflag="-xarch=v9"
  fi
  thread="-lthread"
else
  if [ -n "`uname | grep -i windows`" ]; then
    # windows
    cmd="cl /I${JAVA_HOME}\\include /Islowvm\\src\\rts\\msc /Ogityb0 /Gs ${src}"
    #cmd="cl /DTRACE /I${JAVA_HOME}\\include /Islowvm\\src\\rts\\msc /Ogityb0 /Gs /DTRACE ${src}"
  elif [ -n "`uname | grep -i darwin`" ]; then 
    # Mac OS X
    #cmd="gcc -Islowvm/src/rts/gcc-macosx -I${JAVA_HOME}/include -DTRACE -O2 -o squawk ${src} /System/Library/Frameworks/JavaVM.framework/Versions/A/JavaVM"
    cmd="gcc -Islowvm/src/rts/gcc-macosx -I${JAVA_HOME}/include -O3 -o squawk ${src} /System/Library/Frameworks/JavaVM.framework/Versions/A/JavaVM"
  else
    cmd="cl /I${JAVA_HOME}\\include /Islowvm\\src\\rts\\msc /Zi /DTRACE ${src}"
    comp=gcc
    thread=""
  fi
fi

if [ -z "${cmd}" ]; then
  cmd="$comp -xsb -I${JAVA_HOME}/include -Islowvm/src/rts/${comp} ${archflag} ${thread} ${def64} -DTRACE -g -o squawk $* ${src} -ldl -lm"
  type lint 2>/dev/null && lint -I${JAVA_HOME}/include -Islowvm/src/rts/${comp}  ${def64} -DTRACE -F ${src} 2>squawk.lint
fi

echo $cmd
$cmd
