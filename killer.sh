set proc = `ps aux | grep RDDLServer.jar | cut -f 3 -d ' '`
echo $proc
foreach p ($proc)
kill -9 $p
end
