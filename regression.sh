#
# This script runs the Squawk3G regression tests. It takes one optional
# parameter which specifies whether or not the 64 bit version is to be
# tested.
#
if [ $# -ne 0 ]; then
    flag64="-64"
else
    flag64=""
fi

start=`date`

echo >regression.output

echo "Building and running regression in production mode ..." >> regression.output

# build everything
java -jar build.jar -DTYPEMAP=false $flag64 >>regression.output 2>&1

# romize the image and build the executable
java -jar build.jar $flag64 -mac -o2 -prod rom j2me translator tck graphics >>regression.output 2>&1

# set up the LD_LIBRARY_PATH variable
JVM_PATH="`java -jar build.jar jvmenv | grep export | sed 's:[^=]*=::g' | sed 's:\$LD_LIBRARY_PATH::g'`"
LD_LIBRARY_PATH=$JVM_PATH:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH

# run the regression harness
./squawk -J-Djava.awt.headless=true com.sun.squawk.regression.Main -tck -p:production. >>regression.output 2>&1

result=$?

# re-run the regression harnes with all assertions turned on. The samples are included in the image
# as they take too long on a debug build to load dynamically which screws up the timing dependent
# hibernation testing in the regression framework
echo "" >> regression.output
echo "Building and running regression in debug mode ..." >> regression.output
java -jar build.jar -DTYPEMAP=true -debug $flag64 >>regression.output 2>&1
java -jar build.jar $flag64 -typemap -assume -tracing rom -o:squawk_g -exclude:squawk.exclude j2me translator tck graphics samples >>regression.output 2>&1
./squawk_g -Xboot:squawk_g.suite -J-Djava.compiler=NONE -J-Djava.awt.headless=true com.sun.squawk.regression.Main -p:debug. >>regression.output 2>&1

debugResult=$?

end=`date`

# suffix for subject line in mail
if [ $result -ne 0 -o $debugResult -ne 0 ]; then
    suffix="-- FAILED"
else
    suffix=""
fi

msg="regression.mail"
echo "Subject: Squawk regression test results $suffix" > $msg
echo "" >> $msg
echo "" >> $msg

# Append some details about the regression testing context
echo "Host:                 `hostname`" >> $msg
echo "Arch:                 `uname -a`" >> $msg
echo "Path:                 `pwd`" >> $msg
echo "Start:                $start" >> $msg
echo "Finish:               $end" >> $msg
echo "Production exit code: $result" >> $msg
echo "Debug exit code:      $debugResult" >> $msg
echo "" >> $msg

# Append the squawk version info
echo "++++ squawk -version ++++" >> $msg
squawk -version >>$msg 2>&1
echo "---- squawk -version ----" >> $msg
echo "" >> $msg

# Append the contents of *regression.log files
for f in *regression.log; do
  echo "++++ $f ++++" >> $msg
  cat $f >> $msg
  echo "---- $f ----" >> $msg
  echo "" >> $msg
done

# Append the first 100 lines of *.failed.log if the regression failed
#if [ $result -ne 0 -o $debugResult -ne 0 ]; then
  for f in *.failed.log; do
    if [ -s $f ]; then
      echo "++++ $f ++++" >> $msg
      head -n 100 $f >> $msg
      echo "---- $f ----" >> $msg
      echo "" >> $msg
    fi
  done
#fi

# Append the output of running the production regression if it returned
# with a non-zero exit code. Also, rename the executable so that it is
# not copied to the nightly builds directory.
if [ $result -ne 0 ]; then
  echo "++++ regression.output ++++" >> $msg
  head -n 100 regression.output >> $msg
  echo "---- regression.output ----" >> $msg
  echo "" >> $msg
  mv squawk squawk.failed
fi

# Append the output of running the debug regression if it returned
# with a non-zero exit code. Also, rename the executable so that it is
# not copied to the nightly builds directory.
if [ $debugResult -ne 0 ]; then
  echo "++++ regression.output ++++" >> $msg
  head -n 100 regression.output >> $msg
  echo "---- regression.output ----" >> $msg
  echo "" >> $msg
  mv squawk_g squawk_g.failed
fi

# Only send mail on a system that has a 'mail' command
if [ "X`which mail`" != "X" ]; then
  # Send the mail(s)
  mail nik.shaylor@sun.com < $msg
  mail doug.simon@sun.com < $msg
else
  echo "------ Mail that would be sent with a 'mail' command -------"
  cat $msg
fi

