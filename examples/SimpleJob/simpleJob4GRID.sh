#!/bin/sh

echo "==== This is processing simpleJob ===="

echo "Environment variable set by the system:"
echo "GAI_JOB_WORKING_DIR : $GAI_JOB_WORKING_DIR"
echo "GAI_JOB_RESULTS_DIR : $GAI_JOB_RESULTS_DIR"
echo "GAI_JOB_TEMP_DIR : $GAI_JOB_TEMP_DIR"
echo "GAI_JOB_UID : $GAI_JOB_UID"
echo "GAI_TASK_ID : $GAI_TASK_ID"
echo "GAI_TOTAL_TASKS : $GAI_TOTAL_TASKS"

echo "test"
cal

echo "PATH: $PATH"
echo "LD_LIBRARY_PATH: $LD_LIBRARY_PATH"
echo "GAI_ARCH: $GAI_ARCH"

sleep 10
echo "importedList" > importedList.${GAI_TASK_ID}

while read input
do
        echo "$input" >> importedList.${GAI_TASK_ID}
done

echo "Here is a message from task #${GAI_TASK_ID} on `hostname`" >> dropbox
echo "exitCode" > exitMessage.${GAI_TASK_ID}
echo "0" >> exitMessage.${GAI_TASK_ID}
echo "exitText" >> exitMessage.${GAI_TASK_ID}
echo "processing completed" >> exitMessage.${GAI_TASK_ID}
echo "processing completed2" >> exitMessage.${GAI_TASK_ID}
echo
echo "processed by " 
hostname

exit 0