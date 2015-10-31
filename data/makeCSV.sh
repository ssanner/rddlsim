echo $1 $2
rm -f $1
#line=2
for f in `ls $2`
do
	rewards=`grep "ROUND END" $f | cut -f 2 -d '=' | tr ' ' ','`
	nrounds=`grep "ROUND END" $f | wc -l`
	
	
	if [ $nrounds -ge 30 ]; then
		exp_name=`echo $f | rev | cut -f 2,3 -d '.' | cut -f 3,4,5,6,7,8,9,10,11 -d '_' | rev`	
		echo $f $exp_name $nrounds $rewards
		echo "$exp_name, $nrounds" >> $1
		echo $rewards >> $1	
		#line=$((line+1))
	fi
done