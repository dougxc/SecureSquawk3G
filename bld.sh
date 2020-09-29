#!sh
if [ $# -gt 0 -a "$1" == "builder" ]; then
    cd builder
    ./bld.sh
    cd ..
    exit
fi

cmd="java -jar build.jar $*"
echo $cmd
exec $cmd
