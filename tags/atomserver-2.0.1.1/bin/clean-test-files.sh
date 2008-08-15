#!/bin/sh

export SERVER_ROOT=$(cd `dirname $0`/..; pwd)

echo "clean-test-files.sh..."
echo "SERVER_ROOT= $SERVER_ROOT"

rm -rf $SERVER_ROOT/var/widgets/dummy

rm -f $SERVER_ROOT/var/widgets/acme/4/4/en/*.r1
rm -f $SERVER_ROOT/var/widgets/acme/4/4/en/*.r2
rm -f $SERVER_ROOT/var/widgets/acme/4/4/en/*.r3
rm -f $SERVER_ROOT/var/widgets/acme/4/4/en/*.r4

rm -f $SERVER_ROOT/var/widgets/acme/27/2797/en/*.r1
rm -f $SERVER_ROOT/var/widgets/acme/27/2797/en/*.r2
rm -f $SERVER_ROOT/var/widgets/acme/27/2797/en/*.r3
rm -f $SERVER_ROOT/var/widgets/acme/27/2797/en/*.r4

rm -rf $SERVER_ROOT/var/widgets/acme/12
rm -rf $SERVER_ROOT/var/widgets/acme/55
rm -rf $SERVER_ROOT/var/widgets/acme/23
rm -rf $SERVER_ROOT/var/widgets/acme/33
rm -rf $SERVER_ROOT/var/widgets/acme/34
rm -rf $SERVER_ROOT/var/widgets/acme/77
rm -rf $SERVER_ROOT/var/widgets/acme/92
rm -rf $SERVER_ROOT/var/widgets/acme/64
rm -rf $SERVER_ROOT/var/widgets/acme/98

rm -rf $SERVER_ROOT/var/widgets/Foo-*
rm -rf $SERVER_ROOT/var/widgets/trips

rm -rf $SERVER_ROOT/var/widgets/BLAH
rm -rf $SERVER_ROOT/var/widgets/goop
rm -rf $SERVER_ROOT/var/widgets/UNKNOWN
rm -rf $SERVER_ROOT/var/widgets/mttest
rm -rf $SERVER_ROOT/var/widgets/mttest2
rm -rf $SERVER_ROOT/var/widgets/ompah

rm -rf $SERVER_ROOT/var/dummy/floop
rm -rf $SERVER_ROOT/var/dummy/ugga
rm -rf $SERVER_ROOT/var/dummy/mttest
rm -rf $SERVER_ROOT/var/dummy/mttest2
rm -rf $SERVER_ROOT/var/dummy/lumpah
rm -rf $SERVER_ROOT/var/dummy/dumber

rm -f $SERVER_ROOT/var/NFS-indicator-*.tmp



