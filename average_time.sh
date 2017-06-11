filename=$1
phrase=$2
#echo $filename $phrase
timez=`grep $phrase $filename | cut -f 2 -d ':'`
sumz=`echo $timez | tr ' ' '+'`
#avg=`echo "(1.0/240.0)*($sumz)"`
#echo $avg
total=`echo "scale=2;$sumz" | bc`
echo $filename ":" $total 
