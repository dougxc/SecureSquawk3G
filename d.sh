# Doug's version of the launcher

#----------------------------------------------------------#
#              Setup environment                           #
#----------------------------------------------------------#

if [ ! -f d ]; then
    if [ -z "`uname | grep 'Windows'`" ]; then
        ln -s d.sh d
        chmod +x `find . -name '*.sh'`
        chmod +x tools/preverify*
    fi
fi

# Find all available C compilers ...
comp=""
#if [ -z "$comp" ]; then cl         >/dev/null 2>&1 && comp="-comp msc"; fi
#if [ -z "$comp" ]; then gcc -v     >/dev/null 2>&1 && comp="-comp gcc"; fi
#if [ -z "$comp" ]; then cc -flags  >/dev/null 2>&1 && comp="-comp cc";  fi

if [ -n "`uname | grep 'Windows'`" ]; then
  SEP="\;"
else
  SEP=":"
fi

if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME=`which java`
  JAVA_HOME=`dirname $JAVA_HOME`
  JAVA_HOME=`dirname $JAVA_HOME`
fi

#echo "JAVA_HOME=$JAVA_HOME"
builder="${JAVA_HOME}/bin/java -cp ${JAVA_HOME}/lib/tools.jar${SEP}build.jar Build $comp -diag"
#echo $builder

if [ "X$CSQUAWK" != "X" ]; then
    vm="vm/bld/squawk"
else
    vm="java -jar build.jar squawk"
fi

#----------------------------------------------------------#
#   Show the usage message if no command is specified      #
#----------------------------------------------------------#

if [ $# -eq 0 ]; then 
    $builder -help    
    exit
fi

#----------------------------------------------------------#
#              Rebuild the builder                         #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xbuilder" ]; then 
    cd builder;
    bld.sh
    cd ..
    exit
fi

#----------------------------------------------------------#
#              Rebuild the CSystem.dll                     #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xcsystem" ]; then 
    cl "/I${JAVA_HOME}\include" "/I${JAVA_HOME}\include\win32" /c \
        prototypecompiler/src/com/sun/squawk/compiler/jni/CSystem.c \
        prototypecompiler/src/com/sun/squawk/compiler/jni/dispatch_x86.c
    link /nologo /debug /dll /out:CSystem.dll CSystem.obj dispatch_x86.obj
    exit
fi

#----------------------------------------------------------#
#              Rebuild CodeGen.java and run it             #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xbytecodes" ]; then 
    cd bytecodes;
    bld.sh
    cd ..
    exit
fi

#----------------------------------------------------------#
#       Find all class files in a directory and            #
#       format them as a list of class names               #
#----------------------------------------------------------#

if [ $# -gt 0 -a "X$1" = "Xclasslist" ]; then 
    if [ $# -ne 2 ]; then
        echo "usage: classlist <dir>";
        exit 1;
    fi
    
    dir=$2
    cmd="find $dir -name '*.class' |"
    cmd="$cmd sed 's:$dir::g' |"
    cmd="$cmd sed 's:\.class\$::g'  |"
    cmd="$cmd tr '/'  '.' |"
    cmd="$cmd tr '\\' '.'"
    eval $cmd 
    exit
fi

#----------------------------------------------------------#
#              Fall through to build.jar                   #
#----------------------------------------------------------#

#echo $builder $*
exec $builder $*
