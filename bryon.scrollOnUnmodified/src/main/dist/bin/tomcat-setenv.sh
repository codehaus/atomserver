#!/bin/sh

if [ -z "$ATOMSERVER_HOME" ]; then
    echo "You MUST set ATOMSERVER_HOME"
    exit 1
fi

ATOMSERVER_ARGS="-Datomserver.home=$ATOMSERVER_HOME"
ATOMSERVER_ARGS="-Datomserver.data.dir=$ATOMSERVER_HOME/data $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.conf.dir=$ATOMSERVER_HOME/conf $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.ops.conf.dir=$ATOMSERVER_HOME/conf $ATOMSERVER_ARGS"

ATOMSERVER_ARGS="-Datomserver.env=myenv $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.port=8080 $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.http.port=8080 $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.http.host=0.0.0.0 $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.jmxhttp.hostname=0.0.0.0 $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.jmxhttp.port=50505 $ATOMSERVER_ARGS"

ATOMSERVER_ARGS="-Datomserver.servlet.context=atomserver $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.servlet.mapping=v1 $ATOMSERVER_ARGS"

export ATOMSERVER_ARGS="$ATOMSERVER_ARGS"
echo " ATOMSERVER_ARGS= $ATOMSERVER_ARGS"

# ----- log4j specific arguments
#  NOTE: log4j ONLY takes System vars for substitution in log4j.properties
#
LOG4J_ARGS="-Droot.loglevel=DEBUG -Droot.appender=StdoutFile"
LOG4J_ARGS="-Datomserver.loglevel=DEBUG -Datomserver.logdir=$ATOMSERVER_HOME/logs $LOG4J_ARGS"
LOG4J_ARGS="-Datomserver.logfilename=atomserver $LOG4J_ARGS"

export LOG4J_ARGS="$LOG4J_ARGS"
echo "LOG4J_ARGS= $LOG4J_ARGS"

#---------------------------
export CATALINA_OPTS="$ATOMSERVER_ARGS $LOG4J_ARGS"
echo "%%%%%%%%%%%%%%%  CATALINA_OPTS= $CATALINA_OPTS"
