declare ATOMSERVER_HOME=$(cd `dirname $0`/..; pwd)
declare LIB_DIR=$ATOMSERVER_HOME/lib
declare TC_HOST=localhost
declare STANDALONE=false

declare CP="$LIB_DIR/atomserver.jar"
for jarfile in `ls -1 $LIB_DIR/3rdparty`; do CP="$CP:$LIB_DIR/3rdparty/$jarfile" ; done

startEmbeddedTerracotta() {
    echo "running in STANDALONE mode - starting embedded TC server..."

    # enable "job control", so we can fg the terracotta process later to "join" it
    set -m

    java -Xms512m -Xmx512m \
        -XX:NewRatio=3 \
        -XX:MaxTenuringThreshold=15 \
        -XX:+HeapDumpOnOutOfMemoryError \
        -Dcom.sun.management.jmxremote \
        -Dtc.host=$TC_HOST \
        -cp $CP \
        com.tc.server.TCServerMain &
}

stopEmbeddedTerracotta() {
    echo "running in STANDALONE mode - stopping embedded TC server..."

    java -cp $CP com.tc.admin.TCStop

    # join the terracotta process - this script will end when it has successfully shut down
    fg %?TCServerMain
}

startAtomServer() {
    if [ "$STANDALONE" = "true" ]; then
        startEmbeddedTerracotta
    fi

    java -Xms512m -Xmx512m \
        -Xbootclasspath/p:$LIB_DIR/dso-boot.jar \
        -Dtc.config=$ATOMSERVER_HOME/bin/tc-config.xml \
        -Dcom.tc.l1.modules.repositories=$ATOMSERVER_HOME/tc-modules \
        -Dtc.host=$TC_HOST \
        -cp $CP \
        org.atomserver.AtomServer

    if [ "$STANDALONE" = "true" ]; then
        stopEmbeddedTerracotta
    fi
}

usage() {
    echo TODO: create a proper usage statement
}



while [ $# -gt 0 ]; do
  case "$1" in

    # print the usage statement and quit
    -usage|-help )
        usage
        exit -1
        ;;

    -standalone )
        STANDALONE=true
        ;;

    -tc-host )
        TC_HOST=$2
        shift
        ;;

    # anything else is unknown, and we should fail
    * )
        echo unknown option: $1
        exit -1
        ;;

  esac
  shift
done


startAtomServer