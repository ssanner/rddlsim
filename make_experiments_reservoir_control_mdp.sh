portno=3691

for reserv in 10  
do

	for timeOut in  2            
	do
		for lookahead in 5   
		do 
			for futureStrat in SAMPLE      MEAN
			do 
				for rootPolicy in ALL_ACTIONS   ROOT CONSENSUS
				do 
					for numFutures in  1 5 
					do
						if [ $futureStrat == MEAN ] 
						then  
							if [ $numFutures != 1 ]
							then
								continue;
							fi
							if [ $rootPolicy != ROOT ]
							then
								continue;
							fi
						fi

	
						JOB_NAME=reservoir_$reserv\_$timeOut\_$lookahead\_$numFutures\_$rootPolicy\_$futureStrat\_$portno 
						cp run_experiment.sh /tmp/reserv/$JOB_NAME.sh
						sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/mdp_hybrid\/reservoir_control_mdp.rddl/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/mdp_hybrid\/reservoir_control_mdp_$reserv\.rddl/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/STEPS_LOOKAHEAD/$lookahead/" /tmp/reserv/$JOB_NAME.sh 
						sed -i "s/NUM_FUTURES/$numFutures/" /tmp/reserv/$JOB_NAME.sh
						sed -i "s/HOP_CONSTRAINT/$rootPolicy/" /tmp/reserv/$JOB_NAME.sh 
						sed -i "s/FUTURE_STRATEGY/$futureStrat/" /tmp/reserv/$JOB_NAME.sh 
						sed -i "s/SERVER_PORT_NUMBER/$portno/" /tmp/reserv/$JOB_NAME.sh
						portno=$((portno+1))

						sed -i "s/SERVER_NUM_ROUNDS/30/" /tmp/reserv/$JOB_NAME.sh 
				
						sed -i "s/SERVER_RANDOM_SEED/231/" /tmp/reserv/$JOB_NAME.sh 
	
						sed -i "s/VISUALIZER/none/" /tmp/reserv/$JOB_NAME.sh 

						echo $JOB_NAME $portno  

						
					done
				done
			done
		done
	done
done
