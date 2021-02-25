# Network-Programming-Project
A software failure tolerant CORBA distributed system supply management system for three stores Quebec, Ontario and British Columbia. 
This was acheived using active replication.
Implemented in Java. Communication is done through socket programming using the UDP/IP protocol.

Includes:
- a CORBA front-end able to take requests from Clients and Managers.
- a failure free sequencer receving the requests from the front-end and reliably multicasting them to three server replicas.
- three Replica Managers each receiving the same requests from the sequencer and enacting the requests on the replica
- three replicas, each a mock server containg the three stores



