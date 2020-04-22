#!/bin/bash

### Script installs root.cert.pem to certificate trust store of applications using NSS
### (e.g. Firefox, Thunderbird, Chromium)
### Mozilla uses cert8, Chromium and Chrome use cert9

###
### Requirement: apt install libnss3-tools
###


###
### CA file to install
###

if [ $# -eq 0 ] || [ -z "$1" ] || [ -z "$2" ]
  then
    echo "Provide cert path and cert name!"
    exit 1
  else
    echo "File: $1"
    echo "Name: $2"
fi

#certfile="2048-sha256-root.pem"
#certname="QUIC Server Root CA"


###
### For cert8 (legacy - DBM)
###

for certDB in $(find ~/ -name "cert8.db")
do
    certdir=$(dirname ${certDB});
    certutil -A -n "$2" -t "TCu,Cu,Tu" -i $1 -d dbm:${certdir}
done


###
### For cert9 (SQL)
###

for certDB in $(find ~/ -name "cert9.db")
do
    certdir=$(dirname ${certDB});
    certutil -A -n "$2" -t "TCu,Cu,Tu" -i $1 -d sql:${certdir}
done