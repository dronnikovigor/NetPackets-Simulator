#!/bin/bash

interface_name="tun1"
mode="tun"

ip tuntap del $interface_name mode $mode