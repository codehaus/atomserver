#!/bin/sh

#####################
#    log
#####################
ATOMSERVER_DEBUG=true

log() {
    if [ "$ATOMSERVER_DEBUG" == "true" ] ; then
	    echo "atomserver:: $1"
    fi
}
#####################
#    usage
#####################
usage() {
    echo "USAGE: "
    echo " atomserver.sh  [-env ATOMSERVER_ENVIRONMENT] [-log ATOMSERVER_LOG_LEVEL] [-rootlog ROOT_LOG_LEVEL] ..."
    echo ""
    echo "  -env          : the ATOMSERVER service environment in which to run the server"
    echo "                  (default=hsql) specifies the [ENV].properties file"
    echo "                  most particularly database settings"
    echo "  -seed         : seed the configured database with pets from ATOMSERVER_DATA_DIR"
    echo "                  (default = false) No argument required"
    echo "  -data         : specify the data directory to use for Entry Content."
    echo "                  Note: required when using a file-based Entry Content storage"
    echo "                  (default = ATOMSERVER_HOME/data)"
    echo "  -port         : the HTTP port"
    echo "                  (default = 7890)"
    echo "  -rootlog      : the ROOT log level to use (one of TRACE|DEBUG|INFO|WARN|ERROR|FATAL)"
    echo "                  (default = WARN)"
    echo "  -log          : the ATOMSERVER log level to use (one of TRACE|DEBUG|INFO|WARN|ERROR|FATAL)"
    echo "                  (default = INFO)"
    echo "  -logdir       : the directory to log to"
    echo "                  (default = ATOMSERVER_HOME/logs)"
    echo "  -jmxrmiport   : the RMI port for JMX"
    echo "                  (default = 3336)"
    echo "  -jmxhttpport  : the HTTP port for JMX"
    echo "                  (default = 50505)"
    echo "  -memory       : e.g. 740m  NOTE: the m is REQUIRED"
    echo "                  (default = 256m)"
    echo "  -help, -usage : display this message"
    echo ""
}

#####################
#    error_exit
#####################
error_exit() {
	echo " ================ "
	echo " ERROR:: "
    echo "         $1"
	echo " ================ "
	usage
    exit 123
}

#
#####################
#    MAIN 
#####################

# ----------------------
# set ATOMSERVER_HOME if it is not set
#
if [ -z "$ATOMSERVER_HOME" ]; then
    export ATOMSERVER_HOME=$(cd `dirname $0`/..; pwd)
fi

#-----------------------------------
# If an environment file is available in the $ATOMSERVER_HOME
# folder, load it.  This trumps any set environment variables
# and is the preferred method for configuring machine settings
# because operators do not have to type anything but "./atomserver.sh"
#
# NOTE: we do this FIRST so that we can set any values we wish,
#       but still get defaults
#
# NOTE: be sure to use "export FOO=BAR" in your atomserver-config.sh
#       so that other sub-scripts will get your overrides
#
PROPS_FILE=$ATOMSERVER_HOME/atomserver-config.sh
if [ -e $PROPS_FILE ]
then
  echo Found $PROPS_FILE, loading...
 . $PROPS_FILE
else
  echo Did not find $PROPS_FILE.
fi

# -------------------------------------------------------------
# set the defaults for these parameters - these can all be overridden with the appropriate
# command-line options or using $ATOMSERVER_HOME/atomserver-config.sh
#

#  ----- Atomserver Environment -----
# This determines which atomserver envirnment file to load.
# By default we set this to the one in ../conf; myenv.properties, which is configured to use hsql
#
if [ -z "$ATOMSERVER_ENVIRONMENT" ]; then
    ATOMSERVER_ENVIRONMENT=myenv
fi

#  ----- Data Directory -----
# the location of the "data dir" : where we put the actual Atom Entry Content files
# NOTE: this MUST be an absolute path
#
if [ -z "$ATOMSERVER_DATA_DIR" ]; then
    ATOMSERVER_DATA_DIR=$ATOMSERVER_HOME/data
fi

#  ----- Config Directories -----
# the location of the external configuration dirs, a place where you can externally configure AtomServer
# NOTE: this MUST be an absolute path
# NOTE: ATOMSERVER_CONF_DIR is treated like a WEB-INF, and expects to find /classes and /lib subdirs
# NOTE: ATOMSERVER_OPSCONF_DIR is meant for config files that Ops may widh to override (e.g. DB properties, etc)
#
if [ -z "$ATOMSERVER_CONF_DIR" ]; then
    ATOMSERVER_CONF_DIR=$ATOMSERVER_HOME/conf
fi

if [ -z "$ATOMSERVER_OPSCONF_DIR" ]; then
    ATOMSERVER_OPSCONF_DIR=$ATOMSERVER_HOME/conf
fi

#  ----- Servlet Options -----

if [ -z "$ATOMSERVER_SERVLET_CONTEXT" ]; then
    ATOMSERVER_SERVLET_CONTEXT=atomserver
fi

if [ -z "$ATOMSERVER_SERVLET_MAPPING" ]; then
    ATOMSERVER_SERVLET_MAPPING=v1
fi

#  ----- Logging Options -----

if [ -z "$ATOMSERVER_LOG_LEVEL" ]; then
    ATOMSERVER_LOG_LEVEL=INFO
fi

if [ -z "$ROOT_LOG_LEVEL" ]; then
    ROOT_LOG_LEVEL=WARN
fi

if [ -z "$ATOMSERVER_LOG_DIR" ]; then
    ATOMSERVER_LOG_DIR=$ATOMSERVER_HOME/logs
fi

if [ -z "$ATOMSERVER_LOG_FILENAME" ]; then
    ATOMSERVER_LOG_FILENAME=atomserver
fi

# this name MUST match that configured in the log4j.properties file
#  NOTE: it is only configurable so that we can use Stdout in development
#        Make it ROOT_APPENDER=StdoutFile if you want it to log stdout to a file
if [ -z "$ROOT_APPENDER" ]; then
    ###ROOT_APPENDER=StdoutFile
    ROOT_APPENDER=Stdout
fi

#  ----- Hosts and Ports -----

if [ -z "$ATOMSERVER_HTTP_PORT" ]; then
    ATOMSERVER_HTTP_PORT=7890
fi

# by default we bind to all addresses, this helps with DNS issues
# etc.  For a more locked down security situation, you can use a specific
# IP or hostname
if [ -z "$ATOMSERVER_HTTP_HOST" ]; then
    ATOMSERVER_HTTP_HOST=0.0.0.0
fi

if [ -z "$ATOMSERVER_RMI_JMX_PORT" ]; then
    ATOMSERVER_RMI_JMX_PORT=3336
fi

if [ -z "$ATOMSERVER_HTTP_JMX_PORT" ]; then
    ATOMSERVER_HTTP_JMX_PORT=50505
fi

# by default we bind to all addresses, this helps with DNS issues
# etc.  For a more locked down security situation, you can use a specific
# IP or hostname
if [ -z "$ATOMSERVER_HTTP_JMX_HOSTNAME" ]; then
    ATOMSERVER_HTTP_JMX_HOSTNAME=0.0.0.0
fi

#  ----- Memory -----
if [ -z "$ATOMSERVER_MEMORY" ]; then
    ATOMSERVER_MEMORY="256m"
fi

SEED_DATABASE_WITH_PETS=false

#--------------------------------------------------------
# step over the command-line arguments and interpret them
#
while [ $# -gt 0 ]; do
  case "$1" in

    # print the usage statement and quit
    -usage )
        usage
        exit -1
        ;;

    # print the usage statement and quit
    -help )
        usage
        exit -1
        ;;

    # set the environment in which the atomserver service should run
    #  NOTE: this var is used for determining which atomserver-XXX.properties file to load
    -env )
        ATOMSERVER_ENVIRONMENT=$2
        shift
        ;;

    # the data directory
    -data )
        ATOMSERVER_DATA_DIR=$2
        shift
        ;;

    # seed the database
    -seed )
        SEED_DATABASE_WITH_PETS=true
        ;;

    # override the port to serve HTTP requests to the atomserver service
    -port )
        case "$2" in
        *[!0-9]*|"" ) # if the 'port' parameter is empty or non-numeric, fail
            error_exit "invalid port specified : $2"
            ;;
        * )
            ATOMSERVER_HTTP_PORT=$2
            shift
            ;;
        esac
        ;;

    # override the Http host
    -host )
        ATOMSERVER_HTTP_HOST=$2
        shift
        ;;
        
    # override the port to use for JMX access
    -jmxrmiport )
        case "$2" in
        *[!0-9]*|"" ) # if the 'port' parameter is empty or non-numeric, fail
            echo "invalid JMX RMI port specified : $2"
            exit -1
            ;;
        * )
            ATOMSERVER_RMI_JMX_PORT=$2
            shift
            ;;
        esac
        ;;

    # override the port to use for JMX HttpAdaptor
    -jmxhttpport )
        case "$2" in
        *[!0-9]*|"" ) # if the 'port' parameter is empty or non-numeric, fail
            echo "invalid JMX HTTP port specified : $2"
            exit -1
            ;;
        * )
            ATOMSERVER_HTTP_JMX_PORT=$2
            shift
            ;;
        esac
        ;;

    # set the ROOT log4j debug level 
    -rootlog )
        case "$2" in
        TRACE|DEBUG|INFO|WARN|ERROR|FATAL )
            ROOT_LOG_LEVEL=$2
            shift
            ;;
        * )
            error_exit "unknown ROOT_LOG_LEVEL $2"
            ;;
        esac
        ;;

    # set the ATOMSERVER log4j debug level
    -log )
        case "$2" in
        TRACE|DEBUG|INFO|WARN|ERROR|FATAL )
            ATOMSERVER_LOG_LEVEL=$2
            shift
            ;;
        * )
            error_exit "unknown ATOMSERVER_LOG_LEVEL $2"
            ;;
        esac
        ;;

    # set the memory that should be allocated to resin
    -memory )
        RESIN_MEM=$2
        shift
        ;;

    # anything else is unknown, and we should fail
    * )
        error_exit "unknown command-line argument $1"
        ;;

  esac
  shift
done

#-----------------------
if [ "$JAVA_HOME" = "" ] ; then
    error_exit "WARNING:: Unable to determine the value of JAVA_HOME."
fi
log "===================="
log "JAVA_HOME= $JAVA_HOME"
log "===================="

#-----------------------
# So that we can start up AtomServer standalone using the same JARs that we already ship with AtomServer
#  we put all of the necessary stuff right on the ClassPath
#  Of course, if you're using a container setup, like Tomcat, Jetty, or Resin, you won't need to do this...
#
CONFDIR=$ATOMSERVER_CONF_DIR
WEBAPPS=$ATOMSERVER_HOME/webapps

ATOMSERVER_VERSION=`cat $CONFDIR/classes/version.properties | grep "atomserver.version" | cut -d = -f 2`
log "ATOMSERVER_VERSION = $ATOMSERVER_VERSION"

ATOMSERVER_WEBAPP=$WEBAPPS/atomserver-$ATOMSERVER_VERSION
ATOMSERVER_WAR=$WEBAPPS/atomserver-$ATOMSERVER_VERSION.war

if [ -d "$ATOMSERVER_WEBAPP" ] ; then
   log "$ATOMSERVER_WEBAPP already exists."
else
   log "$ATOMSERVER_WEBAPP does NOT exist. Must be expanded"
   if [ -f "$ATOMSERVER_WAR" ]; then
      mkdir -p $ATOMSERVER_WEBAPP
      cd $ATOMSERVER_WEBAPP
      $JAVA_HOME/bin/jar -xvf $ATOMSERVER_WAR
      cd $ATOMSERVER_HOME
   else
      error_exit "The WAR does NOT exist ($ATOMSERVER_WAR)" 
   fi
fi

LIBDIR=$ATOMSERVER_WEBAPP/WEB-INF/lib
CLASSES=$ATOMSERVER_WEBAPP/WEB-INF/classes

log " LIBDIR= $LIBDIR"

log "---------------------------------"
log "Including all JAR files from $LIBDIR onto the Classpath"
log "---------------------------------"

# add in the JAR files 
unset LOCALCLASSPATH
LOCALCLASSPATH=.

DIRLIBS=${LIBDIR}/*.jar
for i in ${DIRLIBS}
do
    # if the directory is empty, then it will return the input string
    # this is stupid, so case for it
    if [ "$i" != "${DIRLIBS}" ] ; then
        LOCALCLASSPATH=$LOCALCLASSPATH:"$i"
    fi
done

export CLASSPATH=:$LOCALCLASSPATH:$CLASSES

log "---------------------------------"
log "Classpath"
log "$CLASSPATH"
log "---------------------------------"

#-----------------------------------------------------------------------
# Build all the command line arguments for AtomServer
#
# ----- atomserver specific arguments
#
ATOMSERVER_ARGS="-Datomserver.home=$ATOMSERVER_HOME"
ATOMSERVER_ARGS="-Datomserver.data.dir=$ATOMSERVER_DATA_DIR $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.conf.dir=$ATOMSERVER_CONF_DIR $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.ops.conf.dir=$ATOMSERVER_OPSCONF_DIR $ATOMSERVER_ARGS"

ATOMSERVER_ARGS="-Datomserver.env=$ATOMSERVER_ENVIRONMENT $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.port=$ATOMSERVER_HTTP_PORT $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.http.port=$ATOMSERVER_HTTP_PORT $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.http.host=$ATOMSERVER_HTTP_HOST $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.jmxhttp.hostname=$ATOMSERVER_HTTP_JMX_HOSTNAME $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.jmxhttp.port=$ATOMSERVER_HTTP_JMX_PORT $ATOMSERVER_ARGS"

ATOMSERVER_ARGS="-Datomserver.servlet.context=$ATOMSERVER_SERVLET_CONTEXT $ATOMSERVER_ARGS"
ATOMSERVER_ARGS="-Datomserver.servlet.mapping=$ATOMSERVER_SERVLET_MAPPING $ATOMSERVER_ARGS"

ATOMSERVER_ARGS="-Dseed.database.with.pets=$SEED_DATABASE_WITH_PETS $ATOMSERVER_ARGS"

# ----- log4j specific arguments
#  NOTE: log4j ONLY takes System vars for substitution in log4j.properties
#
LOG4J_ARGS="-Droot.loglevel=$ROOT_LOG_LEVEL -Droot.appender=$ROOT_APPENDER "
LOG4J_ARGS="-Datomserver.loglevel=$ATOMSERVER_LOG_LEVEL -Datomserver.logdir=$ATOMSERVER_LOG_DIR $LOG4J_ARGS"
LOG4J_ARGS="-Datomserver.logfilename=$ATOMSERVER_LOG_FILENAME $LOG4J_ARGS"

#------  java settings
JAVA_ARGS="-Xms$ATOMSERVER_MEMORY -Xmx$ATOMSERVER_MEMORY -XX:+UseParallelGC -client"

# --------------------------------------------------------------------------------
# start the Java program
#
echo "-------------------------------------"
echo "AtomServer Configuration"
echo "Directories"
echo "  Atomserver Home        : $ATOMSERVER_HOME"
echo "  Atomserver Data Dir    : $ATOMSERVER_DATA_DIR"
echo "  Atomserver Conf Dir    : $ATOMSERVER_CONF_DIR"
echo "  Atomserver Ops Conf Dir: $ATOMSERVER_OPSCONF_DIR"
echo ""
echo "Configuration"
echo "  Environment        : $ATOMSERVER_ENVIRONMENT"
echo "  Http port          : $ATOMSERVER_HTTP_PORT"
echo "  Http host          : $ATOMSERVER_HTTP_HOST"
echo "  JMX RMI port       : $ATOMSERVER_RMI_JMX_PORT"
echo "  JMX HTTP port      : $ATOMSERVER_HTTP_JMX_PORT"
echo "  JMX Host           : $ATOMSERVER_HTTP_JMX_HOSTNAME"
echo ""
echo "Java "
echo "  Java Args  : $JAVA_ARGS"
echo ""
echo "Logging"
echo "  Log4j Args  : $LOG4J_ARGS"
echo "-------------------------------------"

if [ "$SEED_DATABASE_WITH_PETS" = "true" ]; then
    echo "WARNING: seeding the configured database with pets from $ATOMSERVER_DATA_DIR"
else
    echo "WARNING: NOT seeding the configured database with pets from $ATOMSERVER_DATA_DIR"
    echo "         If this is your first time around you may want to start with -seed option"
fi
echo "-------------------------------------"


$JAVA_HOME/bin/java -classpath "$CLASSPATH" $JAVA_ARGS $LOG4J_ARGS $ATOMSERVER_ARGS org.atomserver.utils.jetty.StandAloneAtomServer