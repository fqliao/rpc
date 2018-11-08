#!/bin/bash
echo $@
java -cp './:lib/*:./' fisco.rpc.App $@
