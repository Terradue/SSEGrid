#!/bin/sh

echo "==== This is processing POST ===="

echo "Environment variable set by the system:"
echo "GAI_JOB_WORKING_DIR : $GAI_JOB_WORKING_DIR"
echo "GAI_JOB_RESULTS_DIR : $GAI_JOB_RESULTS_DIR"
echo "GAI_JOB_TEMP_DIR : $GAI_JOB_TEMP_DIR"
echo "GAI_JOB_UID : $GAI_JOB_UID"
echo "GAI_TASK_ID : $GAI_TASK_ID"
echo "GAI_TOTAL_TASKS : $GAI_TOTAL_TASKS"
echo "GAI_DEBUG_WORKING_DIR : $GAI_DEBUG_WORKING_DIR"

if [ -n "$GAI_DEBUG_WORKING_DIR" ] && [ "$GAI_DEBUG_WORKING_DIR" == "true" ] ; then
	echo "Do not delete WORKING DIR for debug purpose"
else
	rm -rf  $GAI_JOB_WORKING_DIR
fi

sleep 5

exit 0
