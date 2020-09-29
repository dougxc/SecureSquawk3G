#!/bin/sh
#
# Process a directory of JBuilder project files (i.e. all
# files with a ".jpx" suffix) to create a new directory
# of equivalent jpx files that have had certain entries
# reformatted to be unix compatibile.
#
# Usage: unixfyjpx <source dir> <dest_dir>
#

if [ $# -ne 2 ]; then
    echo "usage: unixfyjpx <source dir> <dest dir>";
    exit 1;
fi

src_dir=$1
dest_dir=$2

if [ -d $dest_dir ]; then
    echo "Cannot overwrite existing directory: $dest_dir";
    exit 1;
fi

mkdir -p $dest_dir

for f in $src_dir/*.jpx; do
    dest="$dest_dir/`basename $f`";
    # replace '\' with '/' and ';' with ':' in vm parameters
    sed '/application\.vmparameters/s/\\/\//g' < $f | \
    sed '/application\.vmparameters/s/;/:/g' > $dest;

done

cp $src_dir/*.bpgr $dest_dir
cp $src_dir/*.library $dest_dir
