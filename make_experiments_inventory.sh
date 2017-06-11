portno=2126

for shops  in 10 20 30 40 50     
do

	for timeOut in 4   8  
	do
		for lookahead in 5  10 
		do 
			for futureStrat in SAMPLE 
			do 
				for rootPolicy in ROOT ALL_ACTIONS CONSENSUS 
				do 
					for numFutures in 5 10 30       
					do
						if [ $futureStrat == MEAN ] 
						then  
							if [ $numFutures != 1 ]
							then
								continue;
							fi
							if [ $rootPolicy == ALL_ACTIONS ]
							then
								continue;
							fi
						fi
	
						JOB_NAME=inventory_$shops\_$timeOut\_$lookahead\_$numFutures\_$rootPolicy\_$futureStrat\_$portno 
						cp run_experiment.sh /tmp/ic/$JOB_NAME.sh
						sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/mdp_hybrid\/inventory_control_continuous_mdp.rddl/" /tmp/ic/$JOB_NAME.sh
						sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/mdp_hybrid\/inventory_control_$shops\.rddl/" /tmp/ic/$JOB_NAME.sh
						sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/ic/$JOB_NAME.sh
						sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/ic/$JOB_NAME.sh
						sed -i "s/STEPS_LOOKAHEAD/$lookahead/" /tmp/ic/$JOB_NAME.sh 
						sed -i "s/NUM_FUTURES/$numFutures/" /tmp/ic/$JOB_NAME.sh
						sed -i "s/HOP_CONSTRAINT/$rootPolicy/" /tmp/ic/$JOB_NAME.sh 
						sed -i "s/FUTURE_STRATEGY/$futureStrat/" /tmp/ic/$JOB_NAME.sh 
						sed -i "s/SERVER_PORT_NUMBER/$portno/" /tmp/ic/$JOB_NAME.sh
						portno=$((portno+1))
						sed -i "s/SERVER_NUM_ROUNDS/30/" /tmp/ic/$JOB_NAME.sh 
				
						sed -i "s/SERVER_RANDOM_SEED/231/" /tmp/ic/$JOB_NAME.sh 
	
						sed -i "s/VISUALIZER/none/" /tmp/ic/$JOB_NAME.sh 

						echo $JOB_NAME $portno  

						
					done
				done
			done
		done
	done
done
