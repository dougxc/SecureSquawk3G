#cmd="${JAVA_HOME}/bin/java -jar build.jar romize -timer "
cmd="${JAVA_HOME}/bin/java -jar build.jar romize "


extra=""

classes="j2me/j2meclasses"
#classes="j2me/j2meclasses translator/j2meclasses graphics/j2meclasses"
cp=`echo $classes | tr ' ' ':'`

if [ -n "`echo $0 | grep 'romtck.sh'`" ]; then
    cp="$cp:translator/j2meclasses:tck/tck.jar"
    classes="$classes translator/j2meclasses"
fi

for arg in $*; do
    if [ -z "`echo $arg | grep '^-'`" ]; then
        if [ -z "`echo $0 | grep 'romtck.sh'`" ]; then
            cp="$cp:$arg"
        fi
        classes="$classes $arg"
    else
        extra="$extra $arg"
    fi
#echo "arg: $arg"        
done

cmd="$cmd $extra -cp:$cp $classes"
echo $cmd
$cmd
