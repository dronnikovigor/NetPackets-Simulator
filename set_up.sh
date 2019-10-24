#!/bin/bash

interface_name="tun1"
ip_addresses=("10.0.0.1/24" "10.0.1.254/24" "10.0.2.253/24")
mode="tun"

sudo ip tuntap add name $interface_name mode $mode

sudo ip link set $interface_name up

for ip_address in "${ip_addresses[@]}"; do
  sudo ip addr add "$ip_address" dev $interface_name
done
