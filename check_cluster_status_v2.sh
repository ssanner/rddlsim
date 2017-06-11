source /scratch/a1/sge/settings.csh
set jobs=`qstat -u nadamuna | grep -v "qw " | tail -n +3 | cut -f 4 -d ' ' #| grep -v "0.5"`
echo "running jobs " $jobs
echo `qstat -u nadamuna | grep -v "qw " | wc -l`
foreach j ($jobs)
	set name=`qstat -j $j | grep "job_name" | cut -f 2 -d ':' ` 
	set host=`qstat -u nadamuna | grep $j | cut -f 2 -d '@' | cut -f 1 -d '.'`
	echo $j $name $host

	rm -f ~/tmp_scr.sh
	echo "cd /tmp/$name\_tmp; grep '** Actions received' $name\_server.log | wc -l;" > ~/tmp_scr.sh # grep -i 'Round reward' $name.log ; " > ~/tmp_scr.sh
	echo `ssh $host 'sh ~/tmp_scr.sh' `
end
