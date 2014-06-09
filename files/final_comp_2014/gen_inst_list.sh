cd ../..

#############################################################################
# Generate Instance Lists
#############################################################################
DIR="files/final_comp_2014/rddl"
MDP_FILE="files/final_comp_2014/mdp_instance_list.txt"

if [[ -f ${MDP_FILE} ]]; then  
        rm -f ${MDP_FILE} # if the file exists, remove it
fi
echo
echo MDP instances exported to ${MDP_FILE}
for f in "${DIR}"/*inst_mdp*.rddl
do
  echo ${f} | sed 's#^.*/##' | sed 's#\.rddl##'
  echo ${f} | sed 's#^.*/##' | sed 's#\.rddl##' >> ${MDP_FILE}
done

POMDP_FILE="files/final_comp_2014/pomdp_instance_list.txt"

if [[ -f ${POMDP_FILE} ]]; then  
        rm -f ${POMDP_FILE} # if the file exists, remove it
fi
echo
echo POMDP instances exported to ${POMDP_FILE}
for f in "${DIR}"/*inst_pomdp*.rddl
do
  echo ${f} | sed 's#^.*/##' | sed 's#\.rddl##'
  echo ${f} | sed 's#^.*/##' | sed 's#\.rddl##' >> ${POMDP_FILE}
done

