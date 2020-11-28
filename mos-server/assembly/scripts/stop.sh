#!/bin/bash
jarname="mos-server"
stopCmd="kill `ps -ef | grep java | grep $jarname | grep -v 'grep'|awk '{print $2}'`"
echo $stopCmd
$stopCmd
