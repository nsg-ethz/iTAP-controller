# iTAP prototype implementation
More information about iTAP: https://itap.ethz.ch

## Overview
The prototype implementation is based on the [Floodlight OpenFlow Controller](http://www.projectfloodlight.org/floodlight/) (Version 1.1) and adds some additional modules to it. The iTAP-specific code can be found in
```
/src/main/java/ch/ethz/tik/sdnobfuscation
```

## Install instructions
Clone this repository:
```
$ git clone git@github.com:nsg-ethz/iTAP-controller.git
```
Install dependencies:
```
$ sudo apt-get install build-essential openjdk-7-jdk ant maven python-dev eclipse
```
Build and run the controller:
```
$ cd itap-controller
$ ant
$ java -jar target/floodlight.jar
```
To run the controller in a virtual network, we recommend using [Mininet](http://mininet.org/download/). After downloading & installing Mininet (http://mininet.org/download/), run for example:
```
sudo mn --topo tree,5 --controller=remote,ip=127.0.0.1,port=6653 --switch ovsk,protocols=OpenFlow13
```




