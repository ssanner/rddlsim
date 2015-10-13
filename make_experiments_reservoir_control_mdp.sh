portno=2316

for reserv in 10 20 30 40 50 
do

	for timeOut in 0.25 0.5 1 10 100 
	do
		for lookahead in 4 8 16 
		do 
			for numFutures in 1 2 10 50 100 1000 
			do 
				for rootPolicy in ROOT ALL_ACTIONS
				do 
					for futureStrat in MEAN SAMPLE
					do
				
						JOB_NAME=reservoir_$reserv\_$timeOut\_$lookahead\_$numFutures\_$rootPolicy\_$futureStrat\_$portno 
						cp run_experiment.sh /tmp/reserv/$JOB_NAME.sh
						sed -i "s/DOMAIN_FILENAME/\/scratch\/rddlsim\/files\/mdp_hybrid\/reservoir_control_mdp.rddl/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/INSTANCE_FILENAME/\/scratch\/rddlsim\/files\/mdp_hybrid\/reservoir_control_mdp_$reserv\.rddl/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/STEPS_LOOKAHEAD/$lookahead/" /tmp/reserv/$JOB_NAME.sh 
						sed -i "s/NUM_FUTURES/$numFutures/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/HOP_CONSTRAINT/$rootPolicy/" /tmp/reserv/$JOB_NAME.sh 
						sed -i "s/FUTURE_STRATEGY/$futureStrat/" /tmp/reserv/$JOB_NAME.sh 
						sed -i "s/SERVER_PORT_NUMBER/$portno/" /tmp/reserv/$JOB_NAME.sh
						portno=$((portno+1))

						sed -i "s/SERVER_NUM_ROUNDS/30/" /tmp/reserv/$JOB_NAME.sh 
				
						sed -i "s/SERVER_RANDOM_SEED/42/" /tmp/reserv/$JOB_NAME.sh 
	
						sed -i "s/VISUALIZER/reservoir/" /tmp/reserv/$JOB_NAME.sh 

						echo $JOB_NAME $portno  
					done
				done
			done
		done
	done
done
