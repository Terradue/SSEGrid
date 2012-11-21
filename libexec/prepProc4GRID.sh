#!/bin/sh

echo "==== This is processing PREP ===="

echo "Environment variable set by the system:"
echo "GAI_JOB_WORKING_DIR : $GAI_JOB_WORKING_DIR"
echo "GAI_JOB_RESULTS_DIR : $GAI_JOB_RESULTS_DIR"
echo "GAI_JOB_TEMP_DIR : $GAI_JOB_TEMP_DIR"
echo "GAI_JOB_UID : $GAI_JOB_UID"
echo "GAI_TASK_ID : $GAI_TASK_ID"
echo "GAI_TOTAL_TASKS : $GAI_TOTAL_TASKS"

mkdir -p $GAI_JOB_WORKING_DIR
mkdir -p $GAI_JOB_RESULTS_DIR

chmod +x $GAI_JOB_WORKING_DIR/*.sh
chmod +x $GAI_JOB_WORKING_DIR/bin/*

sleep 5

exit 0
