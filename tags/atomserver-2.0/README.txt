
=======================================================
Building hcData
=======================================================

To build against Postgres, you *must* provide the "env" property to maven (i.e "-Denv=postgres")
env  is the atomserver environment to use. It controls which environment properties file to use. (default=dev)
           These files live at src/main/resources/env. And follow the naming convention {env}.properties
           postgres.properties is setup for a local Postgres Server.

$ mvn -Denv=postgres install

BTW; you can also control logging somewhat this same way; 
rootLoglevel    is the root log4j log level (default=DEBUG)
loglevel        is the log4j log level for "org.atomserver" (default=TRACE)

$ mvn -Denv=postgres -DrootLogLevel=WARN install

NOTES
------

A)   If you kill a build mid-flight you might leave the DB in a "bad state". So you may need to "clear it".
Just use this backdoor JUnit (which clears all rows from the tables)
(of course, you can just use a SQL client with "DELETE *  FROM EntryStore" to do the same, assuming you have access)

$ mvn -Denv=postgres -DENABLE_DB_CLEAR_ALL=true -Dtest=DBClearTest test

B) The JUnits seed the DB with Widget files from ./var/widgets. If the build fails or is killed mid-flight, it will
sometimes be necessary to delete spurious files (otherwise the build may see files it is not expecting to...). 
There is a convenience script for this cleanup;

$ ./bin/clean-test-files.sh 


=======================================================
Database Information
==============

Setting up Postgres
---------------------
A) Installing Postgres;
`````````````
On MAC:: Follow these instructions. http://developer.apple.com/internet/opensource/postgres.html
This is a fairly detailed process. You must actually compile postgres.
So you'll need XCode tools installed, and you'll have to install Fink, etc.
But don't get discouraged, it's not really hard, just a bit labourious
And ultimately, worth the trouble!!

B) Start Postgres;
``````````````
$ su postgres;
$ pg-ctl start;   (Or possibly pg-ctl reload;)

C) Create the DB;  (Note: postgres.properties is currently setup for to use the local "atomserver" user)
``````````````
$ su postgres;
$ createdb atomserver_dev;

D) Install the tables 
``````````````
$ psql atomserver_dev -f ./src/main/resources/postgres_ddl.sql
$ psql atomserver_dev -f ./src/main/resources/grant_perms.sql

E) Verify
$ psql atomserver_dev
# /d EntryStore;
# /q

F) Seed the DB
``````````
The JUnits seed (and tear down!!!!!) the DB, so you don't need to do this in you dev environment
But on, say, staging, you can seed the DB using the convenience script; dbseed.sh


