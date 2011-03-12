#!/bin/sh
echo Processing "${1}"
cat "${1}" | tr -d '\r' > "${1}"
