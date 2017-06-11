set hosts = `qhost | grep "compute-*" | cut -f 1 -d ' ' `
foreach h ($hosts)
	echo $h
	ssh $h 'tcsh ~/rddlsim/killer.sh'
end 
