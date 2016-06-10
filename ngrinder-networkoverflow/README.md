ngrinder-networkoverflow
========================

Overview
========
nGridner network overflow blocking plugin. 
Some test case can causes a lot of traffic which the network may not handle and crash the other systems existing in the same network.
This plugin provides a limitation on the total bandwidth and per test bandwidth usage so it shutdowns the tests by force which threaten the network stability.

Build
=====
You can build ngrinder networkoverflow plugin with Plugin Framework for Java.
Then copy a target/classes file in the target folder into ${NGRINDER_HOME}/plugins folder.
This plugin is compatible with nGrinder 3.0.3 or above.

Configuration
=============
There are two options a user can set in the system.conf.
* ngrinder.bandwidth.limit.megabyte - total ngrinder using bandwidth limit in the unit of *mega byte*. The default value is 128 = 1Gbps 
* ngrinder.pertest.bandwidth.limit.megabyte - per test bandwidth limit in the unit of *mega byte*. The default value is 128 = 1Gbps 


Lisense
=======
Follows nGrinder license.
