portno=5862

for inst in 5 6  7            
do

	for timeOut in 16                
	do
		for lookahead in 20          
		do 
			for futureStrat in SAMPLE MEAN  
			do 
				for rootPolicy in ROOT ALL_ACTIONS CONSENSUS 
				do 
					for numFutures in 40 80 160     
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
	
						JOB_NAME=racetrack_$inst\_$timeOut\_$lookahead\_$numFutures\_$rootPolicy\_$futureStrat\_$portno 
						cp run_experiment.sh /tmp/racetrack/$JOB_NAME.sh
						sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/mdp_hybrid\/racetrack_mdp.rddl/" /tmp/racetrack/$JOB_NAME.sh
						sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/mdp_hybrid\/racetrack_inst_mdp__$inst\.rddl/" /tmp/racetrack/$JOB_NAME.sh
						sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/racetrack/$JOB_NAME.sh
						sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/racetrack/$JOB_NAME.sh
						sed -i "s/STEPS_LOOKAHEAD/$lookahead/" /tmp/racetrack/$JOB_NAME.sh 
						sed -i "s/NUM_FUTURES/$numFutures/" /tmp/racetrack/$JOB_NAME.sh
						sed -i "s/HOP_CONSTRAINT/$rootPolicy/" /tmp/racetrack/$JOB_NAME.sh 
						sed -i "s/FUTURE_STRATEGY/$futureStrat/" /tmp/racetrack/$JOB_NAME.sh 
						sed -i "s/SERVER_PORT_NUMBER/$portno/" /tmp/racetrack/$JOB_NAME.sh
						portno=$((portno+1))
						sed -i "s/SERVER_NUM_ROUNDS/30/" /tmp/racetrack/$JOB_NAME.sh 
				
						sed -i "s/SERVER_RANDOM_SEED/42/" /tmp/racetrack/$JOB_NAME.sh 
	
						sed -i "s/VISUALIZER/racetrack/" /tmp/racetrack/$JOB_NAME.sh 

						echo $JOB_NAME $portno  

						
					done
				done
			done
		done
	done
done
