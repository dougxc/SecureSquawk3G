# Copyright (c) 2000 by Sun Microsystems, Inc.
# All rights reserved.
# set JVMDLL=C:\j2sdk1.4.2_01\jre\bin\client\jvm.dll

#buildFlags="-traceclassinfo"
#buildFlags="-tracemethods -tracefilter javasoft.sqe.tests.api.java.lang.Class."
buildFlags=""

if [ -n "`uname | grep 'Windows'`" ]; then
  SEP="\;"
  interpreter="./squawk.exe"
  link="sh link.sh"
else
  SEP=":"
  interpreter="./squawk"
  link="link.sh"
fi

century=""
centuryExt=""
if [ $# -eq 1 ]
then
  century=":$1"
  centuryExt=".$1"
fi

if [ ! -d tck/log ]; then
  mkdir tck/log
fi

pfile=tck/log/passed$centuryExt.txt
ffile=tck/log/failed$centuryExt.txt
lfile=tck/log/log$centuryExt.txt

rm -f $pfile $ffile $lfile

sdate=`date`
echo $sdate > $pfile
echo $sdate > $ffile
echo $sdate > $lfile

#echo "The tests started. See tail -f $lfile"

# build the image
cmd="java -jar build.jar romize $buildFlags -tck:java.lang.PositiveTCK10a$century -cp j2me/j2meclasses:tck/tck.jar j2me/j2meclasses"
echo $cmd
$cmd >> $lfile

# build the slowvm interpreter
rm -f $interpreter
$link

args=""
classlist=./classlist
for test in `cat $classlist| sed -e 's/ /|/g'`
do
    args=`echo $test | sed -e 's/|/ /g'`
    echo "$interpreter -positivetck -resourcepath:tck/tck.jar $args " >> $lfile
    $interpreter       -positivetck  -resourcepath:tck/tck.jar $args   >> $lfile 2>&1
    if [ $? -ne 95 ]
    then
      echo $args >> $ffile
    else
      echo "$args  PASSED" >> $pfile
    fi
done

fdate=`date`

echo $fdate >> $pfile
echo $fdate >> $ffile
echo $fdate >> $lfile

plines=`cat $pfile | wc -l`
flines=`cat $ffile| wc -l`
echo "Passed: `expr $plines - 2`"
echo "Failed: `expr $flines - 2`"
#echo "The results are stored in files: $pfile, $ffile, $lfile"
