Lagom support in Scala IDE
==========================

This project contains plugins for seamless support of `lagom`_ in `Scala IDE`_.

*This is a work in progress. Please file `tickets`_ if you encounter problems.*

Building
--------

Maven is used to manage the build process. TODO

You then clone and checkout master trunk:-

    `$ git clone git://github.com/scala-ide/lagom-eclipse-plugin.git`
    
    `$ cd lagom-eclipse-plugin`

    `$ git checkout master`

Finally use the following commands to build for Scala IDE nightly: 

    `$ ./build.sh`

The built update site will be available in org.scala-ide.sdt.scalatest.update-site/target.

.. _Scala IDE: http://scala-ide.org
.. _tickets: http://scala-ide.org/docs/user/community.html
.. _scala-ide/scala-ide: http://github.com/scala-ide/scala-ide

Using Lagom Plugin in Scala IDE
===============================

What is in this guide?
----------------------

This guide will show you how to use the Lagom plugin in Scala IDE.

Prerequisites
.............

*   `Eclipse`_ 4.6 (Neon) or higher with Scala IDE for Scala 2.12 installed (http://scala-ide.org).

    Check the getting started page http://scala-ide.org/docs/user/gettingstarted.html page for instructions on how to install Scala IDE.

*   Basic knowledge of the Eclipse user interface is required.

*   Basic knowledge of the Scala language is required.

Using Lagom in a Scala project
------------------------------

Lagom plugin allows for the following:-

*   Run/Stop `infrastructure` services (`Kafka`, `Cassandra` and `Service Locator`).
*   Run/Stop selected user's microservice.

Run/Stop infrastructure services
--------------------------------

To run the selected user's microservice, click on the Lagom `Loader` element in the editor, right click and choose:-

  Run As -> Lagom Locator or
  Run As -> Lagom Cassandra or
  Run As -> Lagom Kafka

A Run Configuration with the service name will be created automatically.

To stop given service just press `Terminate` button of service console.

Run/Stop user's microservice 
----------------------------

To run the selected user's microservice, click on the Lagom `Loader` element in the editor, right click and choose:-

  Run As -> Lagom Service

A Run Configuration with the service name will be created automatically.

To stop given service just press `Terminate` button of service console.


