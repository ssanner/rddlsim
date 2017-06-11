for vehicles in 5           
do

	for timeOut in 20     
	do
		for lookahead in 3 
		do 
			for futureStrat in SAMPLE 
			do 
				for rootPolicy in ROOT  
				do 
					for numFutures in 3   
					do
						for randomize in false 
						do			
							for round in `seq 1 30`
							do 
	
						JOB_NAME=emergency_$vehicles\_vehicles_$timeOut\_$lookahead\_$numFutures\_$rootPolicy\_$futureStrat\_round_$round\_long  
						cp run_experiment_emergency.sh /tmp/emergency/$JOB_NAME.sh
						sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/emergency_domain\/continuous-time-test\/emergency_continuous_time_multiple_vehicles_test.rddl/" /tmp/emergency/$JOB_NAME.sh
						sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/emergency_domain\/continuous-time-test\/inst_emergency_continuous_time_multiple_vehicles_test_$vehicles.rddl/" /tmp/emergency/$JOB_NAME.sh
						sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/emergency/$JOB_NAME.sh
						sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/emergency/$JOB_NAME.sh
						sed -i "s/STEPS_LOOKAHEAD/$lookahead/" /tmp/emergency/$JOB_NAME.sh 
						sed -i "s/NUM_FUTURES/$numFutures/" /tmp/emergency/$JOB_NAME.sh
						sed -i "s/HOP_CONSTRAINT/$rootPolicy/" /tmp/emergency/$JOB_NAME.sh 
						sed -i "s/FUTURE_STRATEGY/$futureStrat/" /tmp/emergency/$JOB_NAME.sh 
						sed -i "s/NMBER_OF_ROUNDS/1/" /tmp/emergency/$JOB_NAME.sh 
						sed -i "s/DATA_FILENAME_CSV/\/nfs\/stak\/students\/n\/nadamuna\/rddlsim\/files\/emergency_domain\/jan_2011_calls.csv/" /tmp/emergency/$JOB_NAME.sh 
				
						sed -i "s/NUMBER_OF_FOLDS/2/" /tmp/emergency/$JOB_NAME.sh 
						sed -i "s/TRAIN_FOLD_INDEX/1/" /tmp/emergency/$JOB_NAME.sh 
				
						sed -i "s/TESTING_FOLD_INDEX/0/" /tmp/emergency/$JOB_NAME.sh 
				
						sed -i "s/OUTPUT_CSV_FILENAME/outFile_$JOB_NAME.csv/" /tmp/emergency/$JOB_NAME.sh 
				
						sed -i "s/TESTING_PHASE_RANDOM/$randomize/" /tmp/emergency/$JOB_NAME.sh 
				
						sed -i "s/VISUALIZER/none/" /tmp/emergency/$JOB_NAME.sh 

						echo $JOB_NAME   

							done
						done
					done
				done
			done
		done
	done
done
