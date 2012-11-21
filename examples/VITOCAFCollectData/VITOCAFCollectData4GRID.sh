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
    local res="$?"
    local msg=""

    echo "exit trapped with code $res"
   
    echo "exitCode" > exitMessage
    echo $res >> exitMessage
    echo "exitText" >> exitMessage
        
    case "$res" in
        5)
                msg="There was a problem during Application unpacking"
                ;;
        6)
                msg="Cannot find mono framework. Processing aborted!"
                ;;
        7)
                msg="Mono framework found but not running correctly. Processing aborted!" 
                ;;
        8)
                cat ${GAI_JOB_WORKING_DIR}/list.error >>exitMessage 2>/dev/null
                msg="Data query returned an error. Processing aborted!"
                ;;
        9)
                msg="No results for the search criteria. Processing aborted!"
                ;;
        10)
                msg="Error during the retrieval of data $ticketNumber:$fileName from CAF. Processing aborted!"
                ;;
        11)     msg="The query returned no product, please extend your search"
                ;;
        12)     msg="Impossible to fetch cached data at $productURL. Processing aborted!"
                ;;
        16)     msg="Impossible to fetch new data from CAF at $productURL. Processing aborted!"
                ;;
        15)     msg="Error returned by CAF Job [ID $jobID] of ticket $ticketNumber. Last status is $collectStatus. Processing aborted"
                ;;
        20)
                msg="Error during the processing of the file $product_filename. Processing aborted!"
                ;;
        100)
                msg="Processing aborted at system request"
                ;; 
        esac

    if [ "$res" != 0 ]; then
        echo "$msg" | tee -a exitMessage >&2
    else
        echo "Processing completed successfully!" | tee -a exitMessage
    fi

        # clean my stuff
        rm -rf $UNIQUE_TEMP_DIR
        rm -rf mono bin lib

    exit $res
}

# Function to test that mono framework is available and running correctly
function isMonoOK()
{

        echo -n "Checking framework... "

        # get mono path
        local mono_bin=`which mono`

        [ -z "$mono_bin" ] && {
                echo "FAILED!"
                exit 6;
        }

        local mono_response="`$mono_bin | grep "Mono"`"

        [ -z "$mono_response" ] && {
                echo "FAILED!"
                exit 7;
        }

        echo "OK"

}

# set the trap
trap handleTerm TERM
trap cleanExit EXIT

echo "==== This is processing MODIS ===="

echo "Environment variable set by the system:"
echo "GAI_JOB_WORKING_DIR : $GAI_JOB_WORKING_DIR"
echo "GAI_JOB_RESULTS_DIR : $GAI_JOB_RESULTS_DIR"
echo "GAI_JOB_TEMP_DIR : $GAI_JOB_TEMP_DIR"
echo "GAI_JOB_UID : $GAI_JOB_UID"
echo "GAI_TASK_ID : $GAI_TASK_ID"
echo "GAI_TOTAL_TASKS : $GAI_TOTAL_TASKS"
echo "GAI_CE_HOSTNAME: $GAI_CE_HOSTNAME"

echo "Here is the content of the working dir before the application setup"

ls -l

# preparing a tenporary directory which is job unique
[ -z "${GAI_JOB_TEMP_DIR}" ] && {
	echo "[ERROR] Please setup GAI_JOB_TEMP_DIR environment variable"
}
UNIQUE_TEMP_DIR=${GAI_JOB_TEMP_DIR}/${GAI_JOB_UID}
echo "Creating the temporary directory for this task at $UNIQUE_TEMP_DIR"
mkdir -p $UNIQUE_TEMP_DIR

# Here is an important part controlled with a lock file
# in order that it is only the first job that access this part
# preforms the operations of application setup here
# BEGIN lock part

# This locking system is kept here even if the job is intended to be single

# locking the application setup for max 2 minutes
# is time's up, another task tries to perform the setup 
lockfile -10 -r-1 -l 120 $GAI_JOB_WORKING_DIR/install.lock
# if the application package is still there, the installation
# setup has not been performed
if [ -e Application.tgz ]; then
        # if the application tar is still there 
    # the first job must expand it and remove it
        echo -n "Unpacking Application... "
        tar xvzf Application.tgz >>Application.unpack.log
        if [ $? != 0 ]; then
                echo "FAILED!"
                echo "Please check stderr for more information" 
                rm -f $GAI_JOB_WORKING_DIR/install.lock
                exit 5;
        else
                echo "OK!"
                rm -f $GAI_JOB_WORKING_DIR/install.lock
        fi
        rm -f Application.tgz
fi
# END lock part

# I read the arguments
platformShortName="$1"
productType="$2"
latMax=$3
lonMin=$4
latMin=$5
lonMax=$6
startDate="$7"
stopDate="$8"
stopOnError="$9"
shift
disableCache="$9"
shift
destinationURL="$9"

# system check-up
export PATH=$GAI_JOB_WORKING_DIR/mono/bin:$GAI_JOB_WORKING_DIR/bin:$PATH
isMonoOK

echo "stopOnError:$stopOnError"

# read CAF configuration for current CE
if [ -f "${GAI_JOB_WORKING_DIR}/etc/caf-${GAI_CE_HOSTNAME}.conf" ]; then
        . ${GAI_JOB_WORKING_DIR}/etc/caf-${GAI_CE_HOSTNAME}.conf
else
        . ${GAI_JOB_WORKING_DIR}/etc/caf-default.conf
fi

# prepare the results directory
[ -z "$destinationURL" ] && {
        destinationURL=${GAI_JOB_RESULTS_DIR}
}

VITOCAFdestURL="file://math19.intern.vgt.vito.be/linas/ssegrid/CAFCACHE"
VITOCAFLocalPath="/EODATA/data/CAFCACHE"
echo "Files will be publish by VITO CAF to $destinationURL";

# Some init variabes
. VITOCAF.ENV

NbrImportedProduct=0

# query the CAF catalogue for the available product for the selected product type,
# inside the specified bounding box and between the start and stop date
mono ${GAI_JOB_WORKING_DIR}/bin/VITOCAFDataManager.exe list \
        -a "${VITOCAF_ACCESS_POINT}" \
        -q "(*/platformShortName='$platformShortName')
        & (*/productType='$productType') 
        & (*/startdate>='$startDate') & (*/enddate<='$stopDate')
        & (*/boundingBox intersect {bbox}'[$lonMin,$latMax,$lonMax,$latMin]')" \
        >${GAI_JOB_WORKING_DIR}/queryResults 2>${GAI_JOB_WORKING_DIR}/list.error

listret="`wc -l list.error | awk '{print $1}'`"

[ $listret -gt 0 ] && exit 8

nbrResults="`wc -l queryResults | awk '{print $1}'`"

[ $nbrResults -gt 0 ] || exit 11

[ $nbrResults -eq 1 ] && {
        nores=`grep "No results" queryResults`
        [ -n "$nores" ] && exit 9
}


touch jobInProgress ErrorTickets

# read every result of the query
while read ticketNumber fileName
do

        echo "[$ticketNumber] Looking for file $fileName in cache... "
        if [ -d "$VITOCAFLocalPath/$ticketNumber" ] && [ "$disableCache" != "true" ]; then
                lockfile -10 -r-1 -l 240 $VITOCAFLocalPath/$ticketNumber/.lock
                rm -f $VITOCAFLocalPath/$ticketNumber/.lock
                if [ -e "$VITOCAFLocalPath/$ticketNumber/$fileName" ]; then
                        echo "[$ticketNumber] Cache found $VITOCAFLocalPath/$ticketNumber/$fileName";
                        echo "[$ticketNumber] No request to CAF."

                        productURL="$VITOCAFLocalPath/$ticketNumber/$fileName"

                        # modify access time of the ticket
                        touch "$VITOCAFLocalPath/$ticketNumber"

                         # retrieve the data file referenced by $productURL to the directory $GAI_JOB_RESULTS_DIR/
                        echo -n "Copying $productURL to "
                        findthenfetch -c -b "file://$VITOCAFLocalPath/$ticketNumber/" -O $destinationURL/ "$filename" 2>/dev/null
                        # check the data retrieval status
                        if [ $? != 0 ]; then
                                echo "FAILED!"
                                [ "$stopOnError" == "true" ] && exit 12
                                echo "Error during the retrieval of $productURL. Product skip!"
                                echo "FetchDataException: $productURL" >> ErrorTickets
                                continue;
                        else
                                echo "OK!"
                                continue;
                        fi
                fi
        fi

        alreadyRequest="`cat jobInProgress | awk '{print $2}' | grep $ticketNumber | head -n1`"

        if [ "$alreadyRequest" == "$ticketNumber" ]; then
                echo "[$ticketNumber] No cache found but same ticket already in request to CAF. File $fileName should be part of the download."
                echo "0 $ticketNumber $fileName" >>jobInProgress
                continue;
        fi 

        mkdir "$VITOCAFLocalPath/$ticketNumber"
        chmod g+w  "$VITOCAFLocalPath/$ticketNumber"
        # implement lockfile in order to not have a deadlock between 2 similar ticket retrieval
        lockfile -10 -r-1 -l 240 $VITOCAFLocalPath/$ticketNumber/.lock

        # request push for ticket and keep the job IDs
        echo "[$ticketNumber] No cache found. Request CAF for $fileName"
        mono ${GAI_JOB_WORKING_DIR}/bin/VITOCAFDataManager.exe get \
        -a "${VITOCAF_ACCESS_POINT}" \
        -q "{$ticketNumber}" \
        -d ${VITOCAFdestURL}/$ticketNumber/ \
         | grep 'Job ID' | awk '{print $4}' | xargs -i echo "{} $ticketNumber $fileName" >>jobInProgress

done <${GAI_JOB_WORKING_DIR}/queryResults

while read jobID ticketNumber fileName
do

        if [ "$jobID" != "0" ]; then
                # poll each job ticket until finish
                echo -n "[$ticketNumber] Waiting for $fileName [Job ID $jobID]... "
                mono ${GAI_JOB_WORKING_DIR}/bin/VITOCAFDataManager.exe poll \
                -a "${VITOCAF_ACCESS_POINT}" \
                -loop 30 \
                -t $jobID >${UNIQUE_TEMP_DIR}/$jobID.log
                collectStatus=`tail -n1 ${UNIQUE_TEMP_DIR}/$jobID.log`
                rm -f $VITOCAFLocalPath/$ticketNumber/.lock
        else
                collectStatus=0;
        fi

        if [ "$collectStatus" != "0" ]; then
                echo "FAILED!"
                cat ${UNIQUE_TEMP_DIR}/$jobID.log
                if [ "$stopOnError" == "true" ]; then exit 15; fi
                echo "Error returned by CAF Job [ID $jobID] of ticket $ticketNumber. Last CAF status is $collectStatus. Product skip!"
                echo "VITOCAFException: $ticketNumber:$fileName" >> ErrorTickets
                continue
        else
                echo "OK!"
        fi

        # build the URL to fetch the data
        productURL="$VITOCAFLocalPath/$ticketNumber/$fileName"

        # retrieve the data file referenced by $productURL to the directory $GAI_JOB_RESULTS_DIR/
        echo -n "[$ticketNumber] Copying $productURL to "
        findthenfetch -c -b "file://$VITOCAFLocalPath/$ticketNumber/" -O $destinationURL/ "$filename" 2>/dev/null

        # check the data retrieval status
        [ $? != 0 ] && {
                echo "FAILED!"
                [ "$stopOnError" == "true" ] && exit 16
                echo "Error during the retrieval of $productURL. Product skip!"
                echo "FetchDataException: $productURL" >> ErrorTickets
                continue
        }
        echo "OK!"
done<${GAI_JOB_WORKING_DIR}/jobInProgress

# report the output done for the next process
echo importedList >importedList
find $destinationURL/ -type f -follow | xargs -i echo "file://{}"  >>importedList


exit 0