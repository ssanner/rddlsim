portno=4832 

for vehicles in 5                
do

	for timeOut in 20              
	do
		for lookahead in  3 4 5 
		do 
			for futureStrat in SAMPLE 
			do 
				for rootPolicy in ROOT #ALL_ACTIONS CONSENSUS 
				do 
					for numFutures in 3 5 10 
					do
						for randomize in false 
						do			
							for round in `seq 1 30`
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
	
							JOB_NAME=fitted_emergency_$vehicles\_vehicles_$timeOut\_$lookahead\_$numFutures\_$rootPolicy\_$futureStrat\_round_$round\_long  
						
							cp run_experiment_fitted_emergency.sh /tmp/fitted_emergency/$JOB_NAME.sh
							sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/emergency_domain\/fitting\/fitted_emergency_continuous_time_multiple_vehicles_test.rddl/" /tmp/fitted_emergency/$JOB_NAME.sh
							sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/emergency_domain\/fitting\/fitted_inst_emergency_continuous_time_multiple_vehicles_test_5.rddl/" /tmp/fitted_emergency/$JOB_NAME.sh
							sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/fitted_emergency/$JOB_NAME.sh
							sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/fitted_emergency/$JOB_NAME.sh
							sed -i "s/STEPS_LOOKAHEAD/$lookahead/" /tmp/fitted_emergency/$JOB_NAME.sh 
							sed -i "s/NUM_FUTURES/$numFutures/" /tmp/fitted_emergency/$JOB_NAME.sh
							sed -i "s/HOP_CONSTRAINT/$rootPolicy/" /tmp/fitted_emergency/$JOB_NAME.sh 
							sed -i "s/FUTURE_STRATEGY/$futureStrat/" /tmp/fitted_emergency/$JOB_NAME.sh 
							sed -i "s/SERVER_PORT_NUMBER/$portno/" /tmp/fitted_emergency/$JOB_NAME.sh
							portno=$((portno+1))
							sed -i "s/SERVER_NUM_ROUNDS/1/" /tmp/fitted_emergency/$JOB_NAME.sh 
					
							sed -i "s/SERVER_RANDOM_SEED/$round/" /tmp/fitted_emergency/$JOB_NAME.sh 
	
							sed -i "s/VISUALIZER/none/" /tmp/fitted_emergency/$JOB_NAME.sh 
						
							sed -i "s/DATA_FILENAME_CSV/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/emergency_domain\/jan_2011_calls.csv/" /tmp/fitted_emergency/$JOB_NAME.sh 
				
							sed -i "s/NUMBER_OF_FOLDS/2/" /tmp/fitted_emergency/$JOB_NAME.sh 
							sed -i "s/TRAIN_FOLD_INDEX/1/" /tmp/fitted_emergency/$JOB_NAME.sh 
				
							sed -i "s/TESTING_FOLD_INDEX/0/" /tmp/fitted_emergency/$JOB_NAME.sh 
	
							sed -i "s/OUTPUT_CSV_FILENAME/outFile_$JOB_NAME.csv/" /tmp/fitted_emergency/$JOB_NAME.sh 
				

							echo $JOB_NAME $portno  
							done
						done				
					done
				done
			done
		done
	done
done
