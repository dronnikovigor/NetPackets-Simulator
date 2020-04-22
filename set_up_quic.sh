#!/bin/bash

root_dir=$(cd `dirname $0` && pwd)

echo "Generating certificates for QUIC"
path_to_chrome_certs_gen="${PATH_TO_CHROMIUM}/src/net/tools/quic/certs/"
cd ${path_to_chrome_certs_gen}
sh "./generate-certs.sh"

cd ${root_dir}
echo "Setting up QUIC"
file="${PATH_TO_CHROMIUM}/src/net/tools/quic/certs/out/2048-sha256-root.pem"
name="QUIC Server Root CA"
sudo sh "./add-certs-to-stores.sh" ${file} "${name}"