portno=17862

for shops  in 10 15 20 25 30  
do

	for timeOut in 0.25 0.5 1 2   
	do
		for lookahead in 2 5 10 
		do 
			for futureStrat in MEAN SAMPLE 
			do 
				for rootPolicy in ROOT ALL_ACTIONS CONSENSUS 
				do 
					for numFutures in 1 5 10 15 30 
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
	
						JOB_NAME=powergen_$shops\_$timeOut\_$lookahead\_$numFutures\_$rootPolicy\_$futureStrat\_$portno 
						cp run_experiment.sh /tmp/power/$JOB_NAME.sh
						sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/mdp_hybrid\/power_gen_mdp.rddl/" /tmp/power/$JOB_NAME.sh
						sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/mdp_hybrid\/power_gen_mdp_$shops\.rddl/" /tmp/power/$JOB_NAME.sh
						sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/power/$JOB_NAME.sh
						sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/power/$JOB_NAME.sh
						sed -i "s/STEPS_LOOKAHEAD/$lookahead/" /tmp/power/$JOB_NAME.sh 
						sed -i "s/NUM_FUTURES/$numFutures/" /tmp/power/$JOB_NAME.sh
						sed -i "s/HOP_CONSTRAINT/$rootPolicy/" /tmp/power/$JOB_NAME.sh 
						sed -i "s/FUTURE_STRATEGY/$futureStrat/" /tmp/power/$JOB_NAME.sh 
						sed -i "s/SERVER_PORT_NUMBER/$portno/" /tmp/power/$JOB_NAME.sh
						portno=$((portno+1))
						sed -i "s/SERVER_NUM_ROUNDS/30/" /tmp/power/$JOB_NAME.sh 
				
						sed -i "s/SERVER_RANDOM_SEED/231/" /tmp/power/$JOB_NAME.sh 
	
						sed -i "s/VISUALIZER/none/" /tmp/power/$JOB_NAME.sh 

						echo $JOB_NAME $portno  

						
					done
				done
			done
		done
	done
done
