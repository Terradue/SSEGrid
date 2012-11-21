#!/bin/sh

# Function to handle a term signal from the Grid middleware
function handleTerm {
        echo "`basename $0`: caught TERM signal, dying..."
        exit 100
}

# Function to trap the exit 
# and log in the exitMessage system 
function cleanExit()
{
	local res=$?
    local msg=""
    
    echo "exitCode" > exitMessage.${GAI_TASK_ID}
    echo $res >> exitMessage.${GAI_TASK_ID}
    echo "exitMessage" >> exitMessage.${GAI_TASK_ID}
        
    case "$res" in
        5)
        	msg = "There was a problem during Application unpacking" 
        10)
        	msg = "Error during the retrieval of data $URL. Processing Aborted!"
        20)
        	msg = "Error during the processing of the file $product_filename. Processing Aborted!"
        100)
        	msg = "Processing concluded at system request" 
 	esac

    if [ "$res" -ne 0 ]; then
    	echo "$msg" | tee -a exitMessage.${GAI_TASK_ID} >&2
    else
    	echo "Processing completed successfully!" | tee -a exitMessage.${GAI_TASK_ID}
    fi
    exit $res
}

# set the trap
trap handle TERM
trap cleanExit EXIT

echo "==== This is processing MODIS ===="

echo "Environment variable set by the system:"
echo "GAI_JOB_WORKING_DIR : $GAI_JOB_WORKING_DIR"
echo "GAI_JOB_RESULTS_DIR : $GAI_JOB_RESULTS_DIR"
echo "GAI_JOB_TEMP_DIR : $GAI_JOB_TEMP_DIR"
echo "GAI_JOB_UID : $GAI_JOB_UID"
echo "GAI_TASK_ID : $GAI_TASK_ID"
echo "GAI_TOTAL_TASKS : $GAI_TOTAL_TASKS"

echo "Here is the content of the working dir before the application setup"

ls -l

# preparing a temporary directory which is task unique
UNIQUE_TEMP_DIR=${GAI_JOB_TEMP_DIR}/${GAI_JOB_UID}/${GAI_TASK_ID}
echo "Creating the temporary directory for this task at $UNIQUE_TEMP_DIR
mkdir -p $UNIQUE_TEMP_DIR

# Here is an important part controlled with a lock file
# in order that it is only the first job that access this part
# preforms the operations of application setup here
# BEGIN lock part

# locking the application setup for max 2 minutes
# is time's up, another task tries to perform the setup 
lockfile -10 -r-1 -l 120 $GAI_JOB_WORKING_DIR/install.lock
# if the application package is still there, the installation
# setup has not been performed
if [ -e Application.tgz ]; then
	# if the application tar is still there 
    # the first job must expand it and remove it
	echo -n "Unpacking Application..."
	tar xvzf Application.tgz
	if [ $? != 0 ]; then
		echo "FAILED!"
		echo "Please check stderr for more information" 
		rm -f $GAI_JOB_WORKING_DIR/install.lock
		exit 5;
	else
		echo "OK!"
		rm -f $GAI_JOB_WORKING_DIR/install.lock
	fi	
	rm -f Application.tar
fi
# END lock part

# I set the environment of VGT application
. MODIS.ENV

# I read the arguments
latMax=$1
lonMin=$2
latMin=$3
LonMax=$4
stopOnError=$5
destinationURL="$6"

# create control dir as required by the main executable
mkdir -p $UNIQUE_TEMP_DIR/control

# prepare the results directory
[ -z "$destinationURL" ] $$ {
	destinationURL="$GAI_JOB_RESULTS_DIR"
}

NbrImportedProduct = 0

while read URL
do
	
	# retrieve the data file referenced by $URL to the directory $UNIQUE_TEMP_DIR/data/
	# options: override data and create dir if does not exists
	echo -n "Get data $URL..."
	product_file = `FindThenFetch -c -O $UNIQUE_TEMP_DIR/data/ $URL`

	# check the data retrieval status
	[ $? != 0 ] && {
		echo "FAILED!"
		[ "$stopOnError" == "true" ] && {
			exit 10
		}
		echo "Error during the retrieval of data $URL. Product skip!"
		echo "getDataException: $URL" >> productError.urls
		continue
	}
	echo "OK!"

	# extract filename, type and date of the product to process
	product_filename=`basename $product_file`
	product_type=`echo $product_filename | cut -c6-8`
	product_date=`echo $product_filename | cut -c11-18`

	case $product_type in
		QKM)
			echo "shells/importQKM.tcl $destinationURL $UNIQUE_TEMP_DIR/control $product_date $file $latMax $lonMin $latMin $lonMax"
			echo -n "processing $product_filename..."
			shells/importQKM.tcl $GAI_JOB_RESULTS_DIR $UNIQUE_TEMP_DIR/control $product_date $file $latMax $lonMin $latMin $lonMax
			proc_res=$?
			break
			;;
		HKM)
			echo "shells/importHKM.tcl $destinationURL $UNIQUE_TEMP_DIR/control $product_date $file $latMax $lonMin $latMin $lonMax"
			echo -n "processing $product_filename..."
			shells/importHKM.tcl $GAI_JOB_RESULTS_DIR $UNIQUE_TEMP_DIR/control $product_date $file $latMax $lonMin $latMin $lonMax
			proc_res=$?
			break
			;;
	esac
		
	
	# if one of my input file fail during processing, it is reported in stderr
	if [ $proc_res != 0 ]; then
		echo "FAILED!"
		[ "$stopOnError" == "true" ] && {
			exit 20
		}
		echo "Error during the processing of the file $product_filename. Product skip!"
		echo "processDataException: $URL" >> productError.urls
		continue
	else
		echo "OK!"
		let NbrImportedProduct = $NbrImportedProduct + 1
	fi
 
done

# report the output done for the next process
echo importedList > importedList.${GAI_TASK_ID}
find $destinationURL/* -type f -name "Remap_Modis_*.dat" >> importedList.${GAI_TASK_ID}

echo "NbrImportedProduct${NbrImportedProduct}" >> NbrImportedProduct
echo $NbrImportedProduct >> NbrImportedProduct

# clean my stuff
rm -rf $UNIQUE_TEMP_DIR

exit 0
