# Project: bash_debug
# Outline: bash_debug is a bash library of tools and functions to support the integration
#          of third-party programs in bash scripts
#
#          the following functions are defined:
#          - bashd_log          output time-tagged lnd formatted og events to stderr
#                               alternatively the name of the standard log function to use may be provided in argument
#                               e.g. #. bash_debug.sh myLogUtility
#                               it shall comply to a two-argument function template <log-function> <message-class> <message>
#          - mypid and myppid   return the pid (resp. parent pid) of the calling process
#          - smartkill          process tree signaling utility
#          - swapstreams        swap stdout and stderr streams
#          - xcmd               control a command or process execution through the analysis of output streams
#          - watchdog           terminate a process or command after a timeout
#          - xtrace_begin       trace a bash script execution through XTRACE functionality
#          - trace              trace and control a bash script command execution
#
# Dependencies:
#          - the trace function requires bash version 3.2 or higher
#          - most functions require the advanced bash command reference beyond the POSIX sh standard
#            hence the command (set +o posix) is triggered when necessary on function execution
#
# Change log:
# version 2.0
#       * corrected the mypid function spurious behaviour when executed in heavy load
#       * the above correction solves in particular the spurious hanging on fifo I/O
#         of the xcmd and watchdog functions
# version 1.0
#       * intial version




BASHD_VERSION=2.0

set +o posix

type -t "$1" >/dev/null && BASHD_LOG="$1"
[ "`type -t \"$BASHD_LOG\"`" != function ] && [ -z "`which \"$BASHD_LOG\" 2>/dev/null | grep -v '^alias'`" ] && BASHD_LOG=

export BASHD_LOG="${BASHD_LOG:-bashd_log}"


if [ "$BASHD_LOG" == bashd_log ] ; then
function bashd_log {
# Default log function used by all other functions
# Usage: log <log-type> <log-message>
	declare mtype=$1; shift 1
	[ -z "$mtype" ] && return 1
	declare now=`date +%Y-%m-%dT%T`
	printf "$now %-8s %s\n" "$mtype" "$*" 1>&2
}
fi

#test log function
! "$BASHD_LOG" INFO "bash_debug.sh: initialising bash_debug version $BASHD_VERSION on [$BASH $BASH_VERSION]" \
&& echo bash_debug.sh: initialise failed 1>&2 && return 1

[ -n "$1" ] && [ "$BASHD_LOG" != "$1" ] && "$BASHD_LOG" WARNING "bash_debug.sh: defined log function '$1' invalid, using default instead"

# if log function is an executable file, make sure its path is absolute
[ -z "${BASHD_LOG/*\/*}" ] && [ ${BASHD_LOG:0:1} != "/" ] && BASHD_LOG="$PWD/${BASHD_LOG#./}"

"$BASHD_LOG" INFO "bash_debug.sh: log function '$BASHD_LOG' defined in BASHD_LOG and used for message logging"

# export log function
[ "`type -t \"$BASHD_LOG\"`" == function ] && export -f "$BASHD_LOG"



if [ "`\ps --version 2>/dev/null | cut -c-6`" != "procps" ]; then
"$BASHD_LOG" WARNING "bash_debug.sh: 'ps' command is not 'procps', disabling 'mypid' and 'smartkill' function"
else


function mypid {
# mypid echoes in stdout the PID of the shell or subshell process from which it is run
    {
        declare pid=
        while [ -z "$pid" ]; do
           sleep 999 &
           declare sleep_pid=$!
           pid=`ps --no-headers -o ppid $sleep_pid`;
           kill $sleep_pid;
	   wait $sleep_pid
           [ -n "$pid" ] && [ "$1" == "-p" ] && pid=`ps --no-headers -o ppid $pid`;
        done
        echo $pid 1>&101
    } 101>&1 >/dev/null 2>&1;
    return 0
}
export -f mypid

function myppid () { mypid -p; }
export -f myppid
"$BASHD_LOG" INFO "bash_debug.sh: defining function 'mypid' and 'myppid'"


export BASHD_PID_LOG=/tmp/bashd_pids.`date +%s`.$$
#"$BASHD_LOG" DEBUG "bash_debug.sh: pid log is $BASHD_PID_LOG"

function bashd_logpid () {
	return 0
	declare pid=`myppid`
	"$BASHD_LOG" DEBUG "PID $pid - $*"  2>>"$BASHD_PID_LOG"
}
export -f bashd_logpid

function disable_posix ()
{
   declare posix_mode="`set -o | grep posix | cut -f2`"
   [ "$posix_mode" == off ] && return 0
   set +o posix 2>/dev/null || { "$BASHD_LOG" ERROR "failed disabling bash POSIX compatibility mode, function '${FUNCNAME[1]}' cannot execute"; return 1; }
   "$BASHD_LOG" WARNING "disabling bash POSIX compatibility mode for '${FUNCNAME[1]}' function execution"
   return 0
}
export -f disable_posix

function smartkill () 
{
#make sure we don't have POSIX compatibility mode set
disable_posix || return 255
{
(
declare USAGE=`cat <<:usage
$FUNCNAME kills the process tree under the given process identifiers. The processes are signaled from
the leaf of the tree to the root. This order can be inversed using the -f (forward) option. Other options
allow further customisations.

Usage:
$FUNCNAME [ options ] <pid1> [<pid2> ... <pidN>]

Options:
       -h               Displays command usage
       -s <signal>      Defines the signal to use (defaulting to TERM)
       -p <pause>       Defines the delay (in seconds) to wait for between the signalling of two 
                        successive generations of processes (default 1 second)
       -k <kill-delay>  Send a KILL signal to the processes if still running after a given delay (in seconds) 
       -f               Proceed from root of the process tree to the leaves
       -c               Proceed only on all child processes of the given PIDs
       -x <child-trees> Do not kill the subtree(s) under the given pid(s) if found in the parent tree.
                        This option may be used more than once
       -y <pids>        Do not kill the given pid(s) if found in the parent tree.
                        This option may be used more than once
       -z <child-pids>  Do not kill the children of the the given pid(s) if found in the parent tree.
                        This option may be used more than once
       -g [<min>:]<max> Proceed only on generations from min to max included. Generations are counted from 0.
                        If only max is provided, all generations up to the given generation are considered
       -i               Make the job immune to TERM INT HUP ABRT or QUIT signals
       -v               Log kill actions and progress via BASHD_LOG

Exit Codes:
       0 on success, if all process trees could be terminated
       1 if at least one of the root processes could not be terminated
       2 if at least one of the child process could not be terminated
       252 when invalid options are provided
       253 when no PID was provided as argument
       254 when an invalid signal specification is defined (through -s option)
       255 on internal error

:usage
`;

    # find first who we are to avoid killing ourselves
    declare mypid=`myppid`;
    [ $? -ne 0 ] && { "$BASHD_LOG" ERROR "$FUNCNAME: system error - could not get self pid" >&102 ; exit 255; }

    declare cmd_line="$FUNCNAME $@";

    declare verbose=false;
    declare child_only=false;
    declare forward_kill=false;
    declare -i sig_pause=1;
    declare abort=false;
    declare -i kill_delay=;
    declare sig=TERM;
    declare -i -a black_list=($mypid);
    declare -i -a grey_list=();
    declare -i -a white_list=();
    declare -a smartkill_opt=(-x $mypid);
    declare immune=false;
    declare min_gen=0;
    declare max_gen=;

    unset OPTIND;
    declare opt;
    while getopts ":s:k:p:x:y:z:g:fcvih" opt; do
        case $opt in 
            s)  # check if it is a valid signal spec
                ! kill -l "$OPTARG" >/dev/null 2>&1 \
                && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid signal spec '$OPTARG'" 2>&102 ; exit 254; }
                smartkill_opt=("${smartkill_opt[@]}" -$opt "$OPTARG")
                sig="$OPTARG";
            ;;
            k)
                kill_delay=$OPTARG;
                [ $kill_delay == "$OPTARG" ] && [ $kill_delay -ge 0 ] && abort=true \
                && smartkill_opt=("${smartkill_opt[@]}" -$opt $kill_delay)
            ;;
            f)
                forward_kill=true;
                smartkill_opt=("${smartkill_opt[@]}" -$opt)
            ;;
            p)
                sig_pause=$OPTARG;
                smartkill_opt=("${smartkill_opt[@]}" -$opt $sig_pause)
            ;;
            v)
                verbose=true;
                smartkill_opt=("${smartkill_opt[@]}" -$opt)
            ;;
            c)
                child_only=true
            ;;
            x)
                black_list=(${black_list[@]} $OPTARG);
                smartkill_opt=("${smartkill_opt[@]}" -$opt "$OPTARG")
            ;;
            y)
                grey_list=(${grey_list[@]} $OPTARG);
                smartkill_opt=("${smartkill_opt[@]}" -$opt "$OPTARG")
            ;;
            z)
                white_list=(${white_list[@]} $OPTARG);
                smartkill_opt=("${smartkill_opt[@]}" -$opt "$OPTARG")
            ;;
            g)
                if [ "$OPTARG" -eq "$OPTARG" ]; then
                    max_gen=$OPTARG; min_gen=0
                else 
                    min_gen="${OPTARG%%:*}"
                    max_gen="${OPTARG##*:}"
                    [ "$min_gen" -eq "$min_gen" ] || min_gen=0
                    [ -z "$max_gen" ] || [ "$max_gen" -eq "$max_gen" ] || min_gen=0
                    [ "$OPTARG" != "$min_gen:$max_gen" ] \
                    && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid generation specification '$OPTARG'" 2>&102; exit 252; }
                fi
                if [ -z "$max_gen" ]; then
                    smartkill_opt=("${smartkill_opt[@]}" -$opt $((min_gen-1)):)
                else 
                    smartkill_opt=("${smartkill_opt[@]}" -$opt $((min_gen-1)):$((max_gen-1)))
                fi;
            ;;
            i)
                immune=true;
                smartkill_opt=("${smartkill_opt[@]}" -$opt)
            ;;
            h)
                echo "$USAGE" >&101;
                exit 0
            ;;
            '?' | ':')
                "$BASHD_LOG" ERROR "$FUNCNAME: option '$OPTARG' is invalid" 2>&102 ; exit 252
            ;;
        esac;
    done;


    [ $# -eq $(($OPTIND-1)) ] \
    && { "$BASHD_LOG" ERROR "$FUNCNAME: no PID argument provided [$FUNCNAME $@]" 2>&102 && echo "$USAGE" >&102; exit 253; }

    shift $(( OPTIND - 1 ));

    $child_only && grey_list=(${grey_list[@]} "$@");


    $immune && trap '"$BASHD_LOG" WARNING "smartkill: trapped termination signal while processing '\''$cmd_line'\'' [pid $mypid] - ignoring" 2>&102' TERM INT HUP ABRT QUIT;

    declare -i status=0;
    declare i=;
    for (( i=1 ; i<=$# ; i++ )); do
        declare -i black_pid=;
        declare is_black=false;
        for black_pid in "${black_list[@]}"; do
            [ ${!i} -eq $black_pid ] && is_black=true && break;
        done;
        $is_black && continue;

        # look for children making sure we don't include ourselves and our parents
        declare -i pid;
        declare -i ppid;
        declare -a children=();
        declare -i white_pid=;
        declare is_white=false;
        for white_pid in "${white_list[@]}";
        do
            [ ${!i} -eq $white_pid ] && is_white=true && break;
        done;
        $is_white || [ "$max_gen" -le 0 ] \
        || children=(`\ps --no-headers -e -o ppid,pid | while read ppid pid; do [ $ppid -eq ${!i} ] && echo $pid; done `);

        # backward kill case
        if ! $forward_kill && [ ${#children[@]} -gt 0 ] ; then
	        smartkill "${smartkill_opt[@]}" "${children[@]}" 2>&102 || [ $status -ne 0 ] || status=2;
		sleep $sig_pause;
	fi;

        # kill the process waiting for $sig_pause seconds to let it do its own cleaning
        # before triggering smartkill of the next process generation
        # abort process with KILL after the configured delay if the initial signal did not terminate it

        if [ $min_gen -gt 0 ] ; then
            is_black=true
        else

            for black_pid in "${grey_list[@]}"; do
                [ ${!i} -eq $black_pid ] && is_black=true && break;
            done;
        fi

        if ! $is_black && declare pname=`ps --no-headers -o cmd ${!i} 2>/dev/null` && kill -$sig ${!i} 2>/dev/null; then
            $verbose && "$BASHD_LOG" INFO "$FUNCNAME: signal $sig sent to process ${!i} '$pname'" 2>&102;
            sleep $sig_pause;
            if $abort && ps ${!i} >/dev/null 2>&1; then
                sleep $kill_delay;
                kill -KILL ${!i} 2>/dev/null \
                && $verbose \
                && "$BASHD_LOG" WARNING "$FUNCNAME: signal $sig did not terminate process ${!i} '$pname', aborting it" 2>&102;
            fi;
            ps ${!i} >/dev/null && status=1 \
            $verbose && "$BASHD_LOG" WARNING "$FUNCNAME: process ${!i} '$pname' still running" 2>&102;
        fi;

        # forward kill case
        if $forward_kill && [ ${#children[@]} -gt 0 ] ; then
            smartkill "${smartkill_opt[@]}" "${children[@]}" 2>&102 || [ $status -ne 0 ] || status=2;
        fi
    done;
    exit $status
)
} 101>&1 102>&2 >/dev/null 2>&1;
} 
export -f smartkill
"$BASHD_LOG" INFO "bash_debug.sh: defining function 'smartkill'"

fi




function swapstreams {
#Usage: swapstreams <statement>
#Execute statement while swaping its stderr and stdout streams
"$@" 3>&1 1>&2 2>&3 3>&-
}
export -f swapstreams
"$BASHD_LOG" INFO "bash_debug.sh: defining function 'swapstreams'"

function backtrap {
    local trap_mem="$1"; shift
    [ $# -eq 0 ] && set - TERM HUP
    trap - "$@"
    while [ $# -gt 0 ]; do eval `echo "$trap_mem" | grep "$1"'$'`; shift; done
}
export -f backtrap

if [ -z "`type -t smartkill`" ]; then
"$BASHD_LOG" WARNING "bash_debug.sh: 'smartkill' is not available, disabling 'xcmd' and 'watchdog' functions"
else


function xcmd () 
{ 
bashd_logpid xcmd main $*
declare opts=":hon:Xxw:lB:C:s:peIWEGbf:F:i:z:d:v:D:";

declare USAGE=`cat <<:usage
$FUNCNAME traps the exit status and stderr messages of an executable statement and forwards them to an awk parser.
stdout is unchanged. The -o option lets the function act on stdout instead of sterr, with stderr respectively unchanged.
Alternatively, $FUNCNAME can be attached to the lastly created background process, or to a list of independent processes.
By default $FUNCNAME logging level is "INFO" and can be modified using the -W, -D or -E
Advanced filtering options allow to precisely define stderr text matching filters and associated actions. Also, the
command return status can be artificially altered by the filter actions.
The -e option (echo) can be used to echo the stderr back, default is to not echo.

Usage: $FUNCNAME [ general-options ] [ filter-options ] [<shell-command> [<cmd-arg> ...] ] [<pid1> <pid2> ... ]

Arguments:
       They define the shell command with its arguments to be triggered.
       If the shell command is eval, the next arguments will be compound as a single statement and
       will be executed through eval.
       If process identifiers are provided, $FUNCNAME waits for the sequence of processes to complete, reporting their
       exit statuses.
       If no argument is provided, the last activated background process of the shell (\\$!) is assumed.

General Options:
       -h            Displays command usage
       -x            Log command status on exit
       -X            Log command status on error only (when exit status is not null)
       -w <tag>      Use the given tag instead of the error tag when reporting a non null command status
       -l            Log when command is triggered
       -B <actions>  Defines begin actions to be run on triggering the shell command
       -C <actions>  Defines completion actions triggered when the shell command has completed
       -s <n>        Set the command status to the status returned by the <n>th pipeline item of an eval
                     statement with several pipelined commands (this option has no effect on non eval commands).
                     By default the status of the left most pipeline item with non null status is returned.
       -p            When calling \\$BASHD_LOG, prefix the line by the name of the shell command
       -n <cmd-var>  sets in the array variable <cmd-var> the statement that would be executed and exits

Filter options: 
       Basic Output Filtering options:
       -o            Operates on stdout instead of stderr. stderr is kept unchanged.
       -e            Echoes back all lines to stderr (resp. to stdout when the -o option is used). Echoing is
                     activated by default unless a filter is defined.
       -I            The BASHD_LOG function with INFO tag is triggered by default
       -W            Idem above with WARNING tag
       -E            Idem above with ERROR tag
       -G            Idem above with DEBUG tag
       -b            Skip blank lines

       Advanced Output Filtering Options:
       -f <filter>   Defines an awk-based <filter> associating a condition and an action under the form 'condition { action }'
                     a typical condition is a regex maching pattern defined within slash characters /regex/
		     predefined actions are info(), warn(), debug(), error() and LOG(type)
                     All predefined function use the stderr line as default argument. different text may be passed
		     to the functions, such as e.g. info("my text is"\\$0) to prefix the log message by a given string.
		     Several filters may be so defined and will be processed in the same order they are being defined.
		     Example of a simple filter: '/[Ee]rror/ {error()}' triggers an error in the log when the keywords
		     'Error' or 'error' are found in the line.
       -F <filter>   Idem above but the action will terminate the line filtering such that subsequent filters
                     will not be executed.
                     i.e. -F 'condition { action }' is equivalent to -f 'condition { action ; next }'
       -i <awk-file> Get filter template definitions from the given awk file. This option may be used more than once.
       -z <skip>     Defines a skip condition, i.e. lines matching this condition are disregarded by all subsequently
                     defined filters. This option may be used more than once.
       -d <delim>    Defines the awk field delimiter (default is a blank character)
       -v <var>=<val> Defines an awk variable <var> with value <val>.
       -D <user-cmd> Declares an executable command that can then be triggered from within the filter action statements
                     e.g. -D notify_me -f '/error/ { notify_me("we got an error:"\\$0) }'
                     This option may be used more than once.
		     Declarations may be provided with customised calling sequences, whereby the string '{}' will be
		     replaced by the trapped line of text.
		     e.g. if notify_me accepts the line of text in its second argument and an error number as first
		     argument, the following declaration could be made: -D 'notify_me 100 "{}"'

       When no option but -o is provided, -Ilx is assumed

Exit Codes: 
       255 is returned in case of syntax error within the filter definitions or in evaluating the command through eval
           or if the $FUNCNAME process is terminated (with TERM, HUP, or INT)
       The exit code of the statement execution or the processes (when child of the calling shell) is returned otherwise
:usage
`;

{ 

declare trace_active=`trap | grep DEBUG | grep trace_trap`;
[ -n "$trace_active" ] && trace -s;

declare awk_functions='
BEGIN {
	ARGC=2
	AND=" "
	XCMD["pid"]=ARGV[3]
	XCMD["cmd"]=ARGV[4]
	XCMD["args"]=ARGV[5]
	XCMD["sq"]=ARGV[6]
	XCMD["dir"]=ARGV[7]
}
function kill(opts,pids    ,a) {
	if(cmd()!="wait") {opts=sprintf("%s -y %c%s%c",opts,XCMD["sq"],XCMD["pid"],XCMD["sq"])}
	if(pids=="") { pids=XCMD["pid"] }
	split(pids,a,AND AND "*")
	for(pids in a) {
		pids=a[pids]+0
		if(pids==0) { pids=XCMD["pid"] } else { if(pids<=0) continue }
		opts=sprintf("%s %s",opts,pids)
	}
	eval("smartkill " opts)
}
function watchdog(exit_status,delay,opts,pids    ,a) {
	delay+=0
	if(exit_status!="") {
		opts=sprintf("%s -T %cecho %s >>\"%s/ctrlK\"%c",opts,XCMD["sq"],exit_status,XCMD["dir"],XCMD["sq"])
	}
	if(cmd()!="wait") {opts=sprintf("%s -y %c%s%c",opts,XCMD["sq"],XCMD["pid"],XCMD["sq"])}
	if(pids=="") { pids=XCMD["pid"] }
	split(pids,a,AND AND "*")
	for(pids in a) {
		pids=a[pids]+0
		if(pids==0) { pids=XCMD["pid"] } else { if(pids<=0) continue }
		opts=sprintf("%s %s",opts,pids)
	}
	eval(sprintf("watchdog -t %d -i %s &",delay,opts))
}
function cmd() { return XCMD["cmd"] }
function args() { return XCMD["args"] }
function xstatus(s) {if(s!="") {XCMD["xstatus"]=s} return XCMD["xstatus"]}
function cstatus() {return XCMD["cstatus"]}
function pstatus() {return XCMD["pstatus"]}
function quit(s) { xstatus(s);exit}
function abort(s,killopts,pids) { xstatus(s);kill(killopts,pids) }
function eval(msg) { gsub(/\n/,"\\&nl;",msg);print msg | "cat >&100" }
function echo(msg) { eval("echo "quote(msg==""?$0:msg)" >&2") }
function copy(msg) { eval("echo "quote(msg==""?$0:msg)) }
function tsampling(name,seconds   ,t) {
	t=systime()
	if(LASTTIME[name]+seconds>t) {return 0}
	LASTTIME[name]=t
	return 1
}
function sampling(name,samples) { 
	LASTSAMPLE[name]++; if(LASTSAMPLE[name]==1) { return 1}
	if(LASTSAMPLE[name]>=samples){LASTSAMPLE[name]=0}
	return 0
}
function escape(txt) { gsub(XCMD["sq"],XCMD["sq"] "\\\\" XCMD["sq"] XCMD["sq"],txt);return txt }
function quote(txt) { return XCMD["sq"] escape(txt) XCMD["sq"] }
function LOG(level,msg) { eval(sprintf("\"$BASHD_LOG\" %s %s",quote(level),quote(prefix (msg==""?$0:msg) ))) }
function info(msg) { LOG("INFO",msg) }
function warn(msg) { LOG("WARNING",msg) }
function debug(msg) { LOG("DEBUG",msg) }
function error(msg) { LOG("ERROR",msg) }
END {
	last_line=$0
	$0="unknown"
	getline<ARGV[2]
	if($0~/^@/) {
		$0=substr($0,2)
		xstatus($0)
	}
	XCMD["cstatus"]=$0
	if(xstatus()=="") {
		xstatus(cstatus())
	}
	getline<ARGV[2]
	gsub(" ","|");XCMD["pstatus"]=$0; NUMPIPE=split($0,PIPESTATUS," ")
	$0=last_line
}
';

declare awk_filters=;
declare awk_separator=" ";
declare -a awk_variables=();
declare default_filter=;
declare filter_defined=false;
declare coproc_functions=;
declare doecho=false;
declare doprefix=false;
declare log_prefix=;
declare exit_tag="ERROR";
declare -i pipe_item=0;
declare report_start=false;
declare report_exit=;
declare cmd_var=;
declare wait_complete=true;

declare line=;
declare nl="
";

declare -i to_stdout=102;
declare -i to_stderr=101;

declare opt;
unset OPTIND;
while getopts "$opts" opt; do
    case $opt in 
        h) echo "$USAGE" 1>&$to_stderr; return 0 ;;
        o) to_stdout=101; to_stderr=102 ;;
        n) ! ( eval $OPTARG'=()' ) \
           && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid variable name '$OPTARG'" 2>&$to_stdout ; return 255; }
            cmd_var=$OPTARG ;;
        X) report_exit='xstatus()' ;;
        x) report_exit=1 ;;
        w) exit_tag="$OPTARG";
            [ -z "$report_exit" ] && report_exit='xstatus()' ;;
        l) report_start=true ;;
        B) awk_filters=`printf 'BEGIN { %s }\n%s' "$OPTARG" "$awk_filters"` ;;
        C) awk_filters=`printf '%s\nEND { %s }' "$awk_filters" "$OPTARG"` ;;
        s) pipe_item=$OPTARG ;;
        p) doprefix=true ;;
        e) doecho=true ;;
        I) default_filter="info" ;;
        W) default_filter="warn" ;;
        E) default_filter="error" ;;
        G) default_filter="debug" ;;
        b) awk_filters=`printf '%s\n/^[ \t]*$/ {next}' "$awk_filters"` ;;
        f) awk_filters=`printf '%s\n%s' "$awk_filters" "$OPTARG"`; filter_defined=true ;;
        F) awk_filters=`printf '%s\n%s' "$awk_filters" "${OPTARG%\}};next}"`; filter_defined=true ;;
        i) awk_filters=`printf '%s\n' "$awk_filters"``cat "$OPTARG"`; filter_defined=true ;;
        z) awk_filters=`printf '%s\n%s {next}' "$awk_filters" "$OPTARG"` ;;
        d) awk_separator="$OPTARG" ;;
        v)
            if echo | gawk -v "$OPTARG" 'BEGIN {}'; then
                awk_variables=("${awk_variables[@]}" "-v" "$OPTARG");
                [ "${OPTARG%%=*}" == prefix ] && eval "log_$OPTARG";
            else
                "$BASHD_LOG" ERROR "$FUNCNAME: variable assignment '$OPTARG' is invalid" 2>&$to_stdout 
                return 255;
            fi
        ;;
        D)
            declare new_f=$OPTARG;
            declare -a new_f_a=($new_f);

            # add "{}" as default argument if missing
            [ -z "${new_f_a[1]}" ] && new_f="$new_f \"{}\"";
            # and replace {} by $1 to accept the first argument
            new_f="${new_f//{\}/\$1}";

            # define the execution statement in the awk coprocessor
            coproc_functions=`printf 'function XCMD_%s { %s ;}\n%s' "${new_f_a[0]}" "$new_f" "$coproc"`;

            # define associated awk function and add in awk program
            awk_functions=`printf '%s\nfunction %s(msg) { eval("XCMD_%s " quote(msg==""?$0:msg)) }' \
                           "$awk_functions" "${new_f_a[0]}" "${new_f_a[0]}"`
        ;;
        '?' | ':')
            "$BASHD_LOG" ERROR "$FUNCNAME: option '$OPTARG' is invalid" 2>&$to_stdout 
            return 255
        ;;
#        q) wait_complete=false ;;
    esac;
done;

#make sure we don't have POSIX compatibility mode set
disable_posix 2>&$to_stdout || return 255

declare -i xcmd_pid=0
declare trap=`trap`
trap 'backtrap "$trap"; [ $xcmd_pid -gt 0 ] && kill $xcmd_pid ' TERM HUP 

#set default behaviour without any option
if [ $OPTIND -eq 1 ] || ( [ $OPTIND -eq 2 ] && [ "$1" == "-o" ] ); then
    default_filter="info";
    doprefix=true;
    report_exit=1;
    report_start=true;
fi;

shift $(( $OPTIND - 1 ));

[ -n "$cmd_var" ] && eval $cmd_var'=("$@")' && return 0;

$filter_defined || [ -n "$default_filter" ] || doecho=true;

[ -n "$default_filter" ] && awk_filters=`printf '%s\n{%s()}' "$awk_filters" "$default_filter"`;

$doecho && awk_filters=`printf '{ echo() }\n%s' "$awk_filters"`;

# test first the awk for syntax errors
#echo "END {exit}$nl$awk_functions$nl$awk_filters$nl" >~/awk
gawk "${awk_variables[@]}" --source "END {exit}$nl" \
     --source "$awk_functions$nl" --source "$awk_filters$nl" /dev/null /dev/null \
     100>/dev/null 2>&1 >/dev/null \
| while read line; do "$BASHD_LOG" ERROR "$FUNCNAME: $line" 2>&$to_stdout; done;
[ ${PIPESTATUS[0]} -ne 0 ] \
&& { "$BASHD_LOG" ERROR "$FUNCNAME: syntax errors in filter definitions" 2>&$to_stdout ; return 255; }


if [ -n "$report_exit" ]; then
    report_exit=`printf '
if(%s) {
        LOG(xstatus()?"%s":"INFO",
		sprintf("exiting with code %%s - command {%%s %%s} returned %%s",
			xstatus(), cmd(), args(), cstatus() (cstatus()!=pstatus()?sprintf(" (%%s)",pstatus()):"")))
}' "$report_exit" "$exit_tag"`;
fi;

awk_filters=`printf '%s\nEND { \n%s\nexit xstatus() }' "$awk_filters" "$report_exit"`;

$report_start && awk_filters='BEGIN {info(sprintf("command {%s %s} starting",cmd(),args()))} '"$awk_filters";


# analyse the command and arguments
declare cmd=;
declare args=;
declare -i watchdog_pid=0;
if [ $# -eq 0 ]; then
    [ -z "$!" ] && { "$BASHD_LOG" ERROR "$FUNCNAME: no process to connect to" 2>&$to_stdout ; return 253; }
    cmd=wait;
    args="$!";
elif [ "$1" == "watchdog" ]; then
    shift;
    watchdog -n args "$@";
    if [ ${#args[@]} -eq 0 ]; then
        cmd="wait"
	[ -z "$!" ] && { "$BASHD_LOG" ERROR "$FUNCNAME: no process to connect to" 2>&$to_stdout ; return 253; }
	args[1]=$!
	set - "$@" $!
    elif [ "${args[0]}" -eq "${args[0]}" ]; then
        cmd="wait";
    else
        cmd="${args[0]}";
        unset args[0];
    fi;
    args="${args[*]}";
    set - watchdog -l 103 -S xstatus "$@";

elif [ "$1" == eval ]; then
    cmd=eval; shift;
    "$BASH" -n -c "$*" 2>&1 >/dev/null | while read line; do
        "$BASHD_LOG" ERROR "$FUNCNAME: $line" 2>&$to_stdout;
    done;
    [ ${PIPESTATUS[0]} -ne 0 ] && return 255;
    args="$*";
    set - "$cmd" "$@";
elif [ "$1" -gt 0 ]; then
    cmd=wait;
    args="$*";
else
    cmd="$1"; shift;
    args="$*";
    set - "$cmd" "$@";
fi;

#get a temporary unique filename to put in the status code to transfer to the awk process
declare stat_file=/tmp/$FUNCNAME$$`date +%s``mypid`;


$doprefix && [ -z "$log_prefix" ] && log_prefix="$FUNCNAME $cmd: ";

# check all process identifiers are numbers in case of wait
if [ "$cmd" == wait ]; then
    declare pid;
    for pid in $args; do
        ! [ "$pid" -gt 0 ] \
        && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid process identifier '$pid'" 2>&$to_stdout ; return 255; }
    done;

    # then if we have a watchdog on processes, trigger it
    if [ "$1" == "watchdog" ] ; then
        # create the watchdog in the background on those processes
        # and notify its specific status in ctrlK if one was defined
        # and if watchdog was triggered
        (
	"$@" 103>&$to_stdout; 
        declare last_item=${xstatus[${#xstatus[@]}-1]};
        if [ -z "${last_item%%*Timeout}" ]; then
            status="${last_item%Timeout}";
            [ -n "$status" ] && echo $status >>"$stat_file/ctrlK";
        fi;
        ) &
        watchdog_pid=$!;
    fi

fi;

# prepare the ground for processing
rm -rf "$stat_file";
mkdir "$stat_file";

bashd_logpid xcmd main stat_file=$stat_file

# creates named pipes:
# ctrl pipe will get the control info (pid and status)
# and out will get the command stderr (or stdout)

mkfifo "$stat_file/ctrl" "$stat_file/out" "$stat_file/pid" \
|| { "$BASHD_LOG" ERROR "$FUNCNAME: mkfifo failed" 2>&$to_stdout ; return 255; }

(
bashd_logpid xcmd monitor $stat_file
    # the awk filter on the out and ctrl named pipes 


    declare mypid=`myppid`

    # in case we are killed
    function terminate {
	"$BASHD_LOG" WARNING "${log_prefix}trapped termination signal, killing processes with SIGTERM" 2>&$to_stdout 
	"$BASHD_LOG" WARNING "${log_prefix}exit status may be irrelevant" 2>&$to_stdout 
        smartkill -vfc $cmd_pid 2>&$to_stdout
    }

    trap 'rm -rf "$stat_file"' EXIT
    trap 'rm -rf "$stat_file"; smartkill -c -p 0 $cmd_pid $mypid; exit 255' INT;
    trap 'terminate' TERM HUP;

    # get the pid of the command from the pid file 
    declare cmd_pid=;
    "$BASHD_LOG" DEBUG "receive_cmd_pid begin" 2>>"$stat_file.log"
    while ! read cmd_pid <"$stat_file/pid"; do
        [ -p "$stat_file/ctrl" ] || exit 255
        "$BASHD_LOG" WARNING "${log_prefix}open error on fifo '$stat_file/pid' for reading pid of command {$cmd $args}, still trying" 2>&$to_stdout
        sleep 1
    done
    "$BASHD_LOG" DEBUG "receive_cmd_pid=$cmd_pid" 2>>"$stat_file.log"

    function get_status {
        "$BASHD_LOG" DEBUG "receive_status begin" 2>>"$stat_file.log"
        declare -a xstatus=()
        while true; do
           read -a xstatus < "$stat_file/ctrl" 
           [ ${#xstatus[*]} -gt 0 ] && break
           [ -p "$stat_file/ctrl" ] || { echo 255; echo 255; return 255; }
           "$BASHD_LOG" WARNING "${log_prefix}open error on fifo '$stat_file/ctrl' for reading status of command {$cmd $args}, still trying" 2>&$to_stdout
           sleep 1
	done

        # recap status and communicate to stdout
        "$BASHD_LOG" DEBUG "receive_status=${xstatus[@]}" 2>>"$stat_file.log"
        declare status=

        # attempt to find a watchdog exit status in last item
        declare last_item=${xstatus[${#xstatus[@]}-1]};
        if [ -z "${last_item%%*Timeout}" ]; then
            status="${last_item%Timeout}";
            unset xstatus[${#xstatus[@]}-1];
        fi;

        if [ -n "$status" ]; then
            # do nothing, watchdog status has priority
            status="@$status";
        elif [ -f "$stat_file/ctrlK" ]; then
            # a watchdog has provided its status
            read status <"$stat_file/ctrlK";
            status="@$status"
        elif [ $pipe_item -gt 0 ] && [ $pipe_item -le ${#xstatus[@]} ]; then
            # pipe element is explicitly requested
            pipe_item=$(( pipe_item - 1 ));
            status=${xstatus[$pipe_item]};
        else
            #get the first non 0 status
            for status in ${xstatus[@]}; do [ $status -ne 0 ] && break; done;
        fi;
        echo $status
        echo ${xstatus[*]}
        return 0
    }

    # put the awk defs in a file
    echo "$awk_functions$nl$awk_filters$nl" >"$stat_file/awk";

    # execute the defined filter on the out stream
    # providing the pid of the command for the kill and watchdog awk functions
    {
    while ! cat "$stat_file/out"; do
        [ -p "$stat_file/out" ] || exit 255
        "$BASHD_LOG" WARNING "${log_prefix}open error on fifo '$stat_file/out' for reading output of command {$cmd $args}, still trying" 2>&$to_stdout
        sleep 1
    done | gawk -F "$awk_separator" -vprefix="$log_prefix" \
            "${awk_variables[@]}" \
            -f "$stat_file/awk" \
            - <(get_status) \
            "$cmd_pid" "$cmd" "$args" "'" "$stat_file" \
    100>&1 1>&3 | (
	mypid=`myppid`
        trap 'smartkill -c $mypid' TERM;
        eval $coproc_functions;
        declare line;
        while read line; do
            line="${line//&nl;/\n}";
            eval "${line}";
        done
    )
    declare -i status=${PIPESTATUS[1]}
    } 1>&$to_stderr 3>&1 2>&$to_stdout

    rm -rf "$stat_file"

    # return status
    exit $status
) >/dev/null 2>&1 &
xcmd_pid=$!;

if [ "$cmd" != wait ]; then

    # this section will execute the command and communicate the stderr (or stdout)
    # to the out stream, its pid and then the command status to the ctrl stream
    # stream 103 will get back the potential watchdog messages and route them where need be

    # execute with retry on pipe opening failures
    (
bashd_logpid xcmd exec container $*
    # send out the pid
    "$BASHD_LOG" DEBUG "send_pid" 2>>"$stat_file.log"
    while ! mypid >"$stat_file/pid"; do 
        [ -p "$stat_file/pid" ] || break
        "$BASHD_LOG" WARNING "${log_prefix}open error on fifo '$stat_file/pid' for writing pid of command {$cmd $args}, still trying" 2>&$to_stdout
        sleep 1
    done

bashd_logpid xcmd exec container pid sent
    # declare the status array to get back the complex command status
    declare -a xstatus=();

    while true; do
        {
        if [ "$1" == "eval" ]; then

            shift;
            # an eval statement so execute it and get back the PIPESTATUS array in xstatus

            # check for a trailing semicolumn
            declare semicolon=;
            "$BASH" -n -c "$*;" && semicolon=";";

            xstatus=(`eval "$* $semicolon "'echo ${PIPESTATUS[@]} >&100' 100>&1 1>&$to_stdout 2>&$to_stderr`);
        else
            xstatus=()
            "$@" 1>&$to_stdout 2>&$to_stderr;
        fi
        declare -i status=$?
        [ ${#xstatus[@]} -eq 0 ] && xstatus=($status)
        break
	} 103>&$to_stdout 102>&$to_stderr 101>"$stat_file/out" >/dev/null 2>&1
        [ -p "$stat_file/out" ] || break
        "$BASHD_LOG" WARNING "${log_prefix}open error on fifo '$stat_file/out' for writing output of command {$cmd $args}, still trying" 2>&$to_stdout
        sleep 1
    done

bashd_logpid xcmd exec container sending status ${xstatus[@]}
    #communicate status and exit code to the awk program via the control file
    "$BASHD_LOG" DEBUG "send_status=${xstatus[@]}" 2>>"$stat_file.log"

    while true; do
        [ -p "$stat_file/ctrl" ] || break
        echo ${xstatus[@]} >"$stat_file/ctrl" && break
        "$BASHD_LOG" WARNING "${log_prefix}open error on fifo '$stat_file/ctrl' for writing status of command {$cmd $args}, still trying" 2>&$to_stdout
        sleep 1
    done
    "$BASHD_LOG" DEBUG "send_status end" 2>>"$stat_file.log"
bashd_logpid xcmd exec container status sent

    )
#    ) <&0 &

else

    # send out the pids
    "$BASHD_LOG" DEBUG "send_pid(wait)" 2>>"$stat_file.log"
    while ! echo $args >"$stat_file/pid"; do
        [ -p "$stat_file/pid" ] || break
        "$BASHD_LOG" WARNING "${log_prefix}open error on fifo '$stat_file/pid' for writing wait pids {$args}, still trying" 2>&$to_stdout
        sleep 1
    done

    # case of wait processes 
    declare -a xstatus=()
    declare pid;
    declare -a wait_pid=();
    declare -i mypid=`myppid`;

    declare -i count=0;
    for pid in $args; do
        #check if the pid is a direct child of our shell
        if [ $((`ps --no-headers -o ppid $pid`)) -eq $mypid ]; then
            wait_pid[$count]=true;
        else
            wait_pid[$count]=false;
        fi;
        count=$((count+1));
    done;

    count=0;
    while true; do
        { 
        for pid in $args; do
            echo "waiting for process#$((count+1)) ($pid) to complete" 1>&100;
    
            # we try to wait anyway in case it is a direct child but it already completed
            wait $pid;
            xstatus[$count]=$?;
            [ ${xstatus[$count]} -ne 127 ] && wait_pid[$count]=true;
    
            # loop in anycase until the process finishes with 1 second retries
            while ps --pid $pid; do sleep 1; done;

            # set zero status to the wait of foreign processes
            ${wait_pid[$count]} || xstatus[$count]=0;

            echo "process#$((count+1)) ($pid) ended"`${wait_pid[$count]} && echo " with status ${xstatus[$count]}"` 1>&100;

            count=$((count+1));
        done;
        break;
        } 100>"$stat_file/out";
        [ -p "$stat_file/out" ] || break
        "$BASHD_LOG" WARNING "${log_prefix}open error on fifo '$stat_file/out' for writing output of command {$cmd $args}, still trying" 2>&$to_stdout
        sleep 1
    done

    # be sure a potential watchdog command has finished
    [ $watchdog_pid -gt 0 ] && wait $watchdog_pid

    #communicate status and exit code to the awk program via the control file
    "$BASHD_LOG" DEBUG "send_status(wait)=${xstatus[@]}" 2>>"$stat_file.log"

    while true; do
        [ -p "$stat_file/ctrl" ] || break
        { echo ${xstatus[@]} && break; } >"$stat_file/ctrl"
        "$BASHD_LOG" WARNING "${log_prefix}open error on fifo '$stat_file/ctrl' for writing status of command {$cmd $args}, still trying" 2>&$to_stdout
        sleep 1
    done
    "$BASHD_LOG" DEBUG "send_status(wait) end" 2>>"$stat_file.log"

fi;

# now get the status of the filter

wait $xcmd_pid >/dev/null 2>&1;
declare status=$?;

#restore environment
backtrap "$trap"
[ -n "$trace_active" ] && trace -r

#echo "stat_file=$stat_file" >&$to_stdout
#cat "$stat_file.log" >&$to_stdout

} 101>&1 102>&2 >/dev/null 2>&1;

# return status of command
rm -f "$stat_file.log"
return $status
}
export -f xcmd
"$BASHD_LOG" INFO "bash_debug.sh: defining function 'xcmd'"


function watchdog () { 
bashd_logpid watchdog main $*
declare USAGE=`cat <<:usage
$FUNCNAME executes a shell command and terminates it (using smartkill) after a given timeout if it has not completed.
Alternatively, $FUNCNAME can be attached to the lastly created background process, or to a list of independent processes.

Watchdog log messages issued by $BASHD_LOG are routed to file descriptor 9, or to standard error if stream descriptor 9
is undefined.

Usage:
$FUNCNAME [-h] [-t <timeout>] [-s <sigspec>] [-k <kill-delay>] [-T <trap-statement>] -S [<status-var>] [-W]
          [smartkill-options] [<shell-command> [<cmd-arg> ...]] | [ <pid1> <pid2> ... ]

Arguments:
       If a shell command is provided it is triggered on the command line while the timer is started. 
       If the shell command is eval, the next arguments will be compound into a single statement and
       will be executed through eval.
       If process identifiers are provided a watchdog process is activated on those processes.
       If no argument is provided, the watchdog is activated on the last activated background process of the shell (\\$!).


Options:
       -h               Displays command usage
       -t <timeout>     Sets the timeout (in seconds), default is 60 seconds
       -s <sigspec>     Defines the signal type to kill the process, default is SIGTERM
       -k <kill-delay>  Defines the delay (in seconds, defaulting to 1 minute) after which a KILL signal
                        will be sent to the processes if still running after the nominal signal was sent.
                        a negative delay disables this option
       -T <trap-stmt>   Executes the given statement (with eval) before the living processes are signalled following
                        the watchdog timeout. The array variable 'watchpid' s available to the statement and contains
                        the list of processes still running. If the trap exits with a status different from zero the
                        processes will not be signalled.
       -S <status-var>  Return in the array variable <status-var> the exit status of every input process.
                        For processes not direct child of the current shell, the exit status will be 1 when
                        the watchdog has been triggered and 0 otherwise.
                        In case of an eval statement, the array will contain the PIPESTATUS array composing the
                        exit statuses of all pipelined commands in the overall statement.
			In case the watchdog was triggered the string 'Timeout' possibly prefixed by the exit code
                        defined through the -w option is appended.
       -w <status-code> Defines the exit code to return in case the watchdog was triggered. By default the status
                        returned by the input shell command or process is returned.
       -l <fd>          Defines the file descriptor where to route log messages (defaulting to standard error fd=2)
       -n <cmd-var>     Sets in the array variable <cmd-var> the statement that would be executed and exits

       smartkill options:
       -p <pause>       Defines the delay (in seconds, default is 1) to wait for between the signalling of two 
                        successive generations of processes
       -f               Proceed from root of the process tree to the leaves
       -c               Proceed only on all child processes of the given process(es)
       -x <child-trees> Do not kill the subtree(s) under the given pid(s) if found in the parent tree.
                        This option may be used more than once
       -y <pids>        Do not kill the given pid(s) if found in the parent tree.
                        This option may be used more than once
       -z <child-pids>  Do not kill the children of the the given pid(s) if found in the parent tree.
                        This option may be used more than once
       -g [<min>:]<max> Proceed only on generations from min to max included. Generations are counted from 0.
                        If only max is provided, all generations up to the given generation are considered
       -i               Make the smartkill jobs immune to TERM INT HUP ABRT or QUIT signals

Exit Codes: 
       When the arguments define a shell-command, the exit code of the command execution is returned. If a specific
       exit code is defined through the -w option, this code is returned instead in case the watchdog was triggered.
       255 is returned in case of syntax error in interpreting the given eval command

       When the arguments define a process-list:
       - Case of a single process child of the current shell: the exit code of that process is returned. If a specific
         exit code is defined through the -w option, this code is returned instead in case the watchdog was triggered.
       - Case of one or several processes foreign to the current shell: 0 is returned unless the watchdog was triggered 
         in which case 1 or the exit code defined through the -w option is returned.
       255 is returned in case the process list is invalid
:usage
`;
declare -i olog=2;
declare cmd_var=;

declare opts=":t:s:k:p:x:y:z:g:S:T:l:n:w:fciWh";
declare opt=;
unset OPTIND;
while getopts "$opts" opt; do
    if [ "$opt" == l ]; then
        ( ! [ "$OPTARG" -gt 0 ] 2>/dev/null || [ ! -h "/dev/fd/$OPTARG" ] ) \
        && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid file descriptor '$OPTARG'" ; return 255; }
        olog=$OPTARG;
    elif [ "$opt" == n ]; then
        ! ( eval $OPTARG'=()' 2>/dev/null ) \
        && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid variable name '$OPTARG'" 2>&$olog ; return 255; }
        cmd_var=$OPTARG;
    fi;
done;

{ 
[ -n "$cmd_var" ] && shift $((OPTIND-1)) && eval $cmd_var'=("$@")' && return 0;

declare timeout=60;
declare kill_delay=10;
declare -a smartkill_opt=();
declare status_var=;
declare trap_function=;
declare exit_code=;

unset OPTIND;
while getopts "$opts" opt; do
    case $opt in 
        t) timeout=$OPTARG ;;
        k) kill_delay=$OPTARG ;;
        s) # check if it is a valid signal spec
           ! kill -l "$OPTARG" >/dev/null 2>&1 \
           && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid signal spec '$OPTARG'" 2>&103 ; return 255; }
           smartkill_opt=("${smartkill_opt[@]}" -$opt "$OPTARG")
        ;;
        p | x | y | z | g) smartkill_opt=("${smartkill_opt[@]}" -$opt "$OPTARG") ;;
        f | c | i) smartkill_opt=("${smartkill_opt[@]}" -$opt) ;;
        S) ! ( eval $OPTARG'=()' ) \
           && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid variable name '$OPTARG'" 2>&103 ; return 255; }
           status_var="$status_var $OPTARG" ;;
        T) trap_function="$OPTARG" ;;
        w) exit_code=$OPTARG ;;
        h) echo "$USAGE" 1>&101; return 0 ;;
        '?' | ':')
           "$BASHD_LOG" ERROR "$FUNCNAME: option '$OPTARG' is invalid" 2>&103 
           return 255
        ;;
    esac;
done;
shift $(($OPTIND - 1));

#make sure we don't have POSIX compatibility mode set
disable_posix 2>&103 || return 255

declare -i mypid=`myppid`;
declare watch_pids=;

if [ $# -eq 0 ]; then
    [ -z "$!" ] && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid process identifier \\$! (null)" 2>&103 ; return 255; }
    watch_pids="$!";
elif [ "$1" -eq "$1" ]; then
    declare pid;
    for pid in $* ; do
        ! [ "$pid" -gt 0 ] && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid process identifier '$pid'" 2>&103 ; return 255; }
        [ "$pid" -eq 1 ] && { "$BASHD_LOG" ERROR "$FUNCNAME: invalid use of watchdog on process 1" 2>&103 ; return 255; }
    done;
    watch_pids="$*"
elif [ "$1" == "eval" ]; then
    shift;
    # check command syntax running a bash -n on it
    xcmd -v prefix="$FUNCNAME: eval statement syntax error: " -E "$BASH" -n -c "$*" 2>&103 || return 255;
    set - eval "$@"
    # we will execute it later with eval to have access to the PIPESTATUS
fi;

declare pid_file=/tmp/$FUNCNAME$$`date +%s``mypid`

[ -z "$watch_pids" ] && ! mkfifo "$pid_file" && { "$BASHD_LOG" WARNING "$FUNCNAME: system error - mkfifo failed" 2>&103; return 255 ; }

(
bashd_logpid watchdog killer
    # the watchdog process
    declare -i mypid=`myppid`;

    # in case of eval statements, we get its pid through a pipe
    declare doexec=false;
    if [ -p "$pid_file" ]; then
        doexec=true
        while ! read watch_pids <"$pid_file" ; do
            [ -p "$pid_file" ] || break
            "$BASHD_LOG" WARNING "$FUNCNAME: open error on fifo '$pid_file' for reading pid, still trying" 2>&103
            sleep 1
        done
        rm -f "$pid_file"
    fi

    # exit if our watch processes are no more
    [ -z "$watch_pids" ] && exit 0;

    declare pid=;
    declare timer_end=;

    # define two termination modes
    trap '[ -n "$pid" ] && kill $pid ' TERM ALRM;
    trap '[ -n "$pid" ] && kill -HUP $pid ' HUP INT ABRT;
#    trap 'smartkill -c `myppid`' EXIT;

    declare -i timer_status=0

    sleep $timeout & pid=$!;
    # wait for timeout
    wait $pid || timer_status=$?
    # from now on trap the termination signals to let the cleaning proceed in case watch_pids is released first
    trap : INT TERM HUP EXIT;

    if [ $timer_status -eq 129 ] ; then
        timer_end="${timer_end:-HUP}";
    elif [ $timer_status -gt 0 ]; then
        timer_end="${timer_end:- Interrupted}";
    fi;

    # look for remaining watch processes
    declare exe=;
    declare -ai watchpid=();
    for pid in $watch_pids; do
        [ "$pid" -eq 1 ] && { "$BASHD_LOG" ERROR "$FUNCNAME: system error - got 1 as watch pid" 2>&103 ; exit 0; }
        ps=`ps --no-headers -o cmd $pid` && watchpid=($watchpid $pid) \
        && exe=`if $doexec; then echo "$*"; else echo "$ps" ; fi \
               | gawk -v exe="$exe" -v pid=$pid -v sq="'" '{printf("%s%c%s%c (pid %d)",exe==""?"":exe" & ",sq,$0,sq,pid)}'`
    done;
    [ ${#watchpid[@]} -eq 0 ] && exit 0;

    declare plural=;
    [ ${#watchpid[@]} -gt 1 ] && plural="es";

    [ "$timer_end" == HUP ] \
    && { "$BASHD_LOG" WARNING "$FUNCNAME: aborted! leaving process$plural $exe to completion" 2>&103 ; exit 0; }

    "$BASHD_LOG" WARNING "$FUNCNAME: timeout! [$timeout seconds$timer_end] - process$plural $exe still running" 2>&103;

    if [ -n "$trap_function" ]; then
#        kill -STOP ${watchpid[@]};
        ( eval "$trap_function" 1>&101 2>&102 );
        declare -i trap_status=$?;
#        kill -CONT ${watchpid[@]};
        [ $trap_status -ne 0 ] \
        && { "$BASHD_LOG" WARNING "$FUNCNAME: kill action cancelled [$trap_status] - leaving process$plural $exe to completion" 2>&103; exit 0; }
    fi;

    "$BASHD_LOG" WARNING "$FUNCNAME: killing process$plural $exe" 2>&103;
    smartkill "${smartkill_opt[@]}" -k $kill_delay -x $mypid `$doexec && echo -y $watch_pids` -v ${watchpid[@]} 2>&103;

    case $? in 
        2) "$BASHD_LOG" WARNING "$FUNCNAME: some child of process$plural of $exe did not terminate" ;;
        1) "$BASHD_LOG" WARNING "$FUNCNAME: process$plural $exe did not terminate" ;;
        0) "$BASHD_LOG" INFO "$FUNCNAME: process$plural $exe aborted" ;;
    esac 2>&103;

    # inform with code 1 that watchdog was triggered
    exit 1
) &
declare -i watchdog_pid=$!;

declare -a status=();

if [ -z "$watch_pids" ]; then

    if [ "$1" == "eval" ]; then
        shift
        # an eval statement so execute it and get back the PIPESTATUS array in status

        # check for a trailing semicolumn
        declare semicolon=;
        "$BASH" -n -c "$*;" && semicolon=";";

        status=(`bashd_logpid watchdog exec eval $*; while ! mypid >"$pid_file" ; do : ; done ; eval $* $semicolon 'echo ${PIPESTATUS[@]} >&100' 100>&1 1>&101 2>&102`);
        declare s=$?; [ ${#status[@]} -eq 0 ] && status=($s);

    else
        (bashd_logpid watchdog exec $*; while ! mypid >"$pid_file"; do : ; done ; "$@") 1>&101 2>&102
        status=($?)
    fi;
fi;

declare -i pid;
declare -a wait_pid=();

declare -i count=0;
for pid in $watch_pids; do
    #check if the pid is a direct child of our shell
    if [ $((`ps --no-headers -o ppid $pid`)) -eq $mypid ]; then
        wait_pid[$count]=true;
    else
        wait_pid[$count]=false;
    fi;
    count=$((count+1));
done;

count=0;
for pid in $watch_pids; do

    # we try to wait anyway in case it is a direct child but it already completed
    wait $pid;
    status[$count]=$?;
    [ ${status[$count]} -ne 127 ] && wait_pid[$count]=true;

    # loop in anycase until the process finishes with 1 second retries
    while ps $pid; do sleep 1; done;

    count=$((count+1));

done;

# release the watchdog cleanly in the background (making sure the sleep process is killed)
# but leaving the smartkill to completion
while ps $watchdog_pid; do kill $watchdog_pid && ps $watchdog_pid && ps $watchdog_pid && sleep 1; done &

# and immediately probe its status
wait $watchdog_pid
declare watchdog_status=$?;
# assume it was not triggered if we get that it was terminated 
# (as that means it was killed when exiting)
# exception to this is if its pid is non existent (wait returns 127)
if [ $watchdog_status -eq 127 ]; then
   watchdog_status=1
elif [ $watchdog_status -gt 1 ] ; then
   watchdog_status=0
fi

wait

# set status in array to all foreign processes
for (( count=0 ; count<${#wait_pid[@]} ; count++ )); do
    ${wait_pid[$count]} || status[$count]=$watchdog_status;
done;

if [ $watchdog_status -ne 0 ]; then
    [ -n "$exit_code" ] && watchdog_status=$exit_code;
    status=(${status[@]} `[ -n $exit_code ] && echo $exit_code`Timeout);
fi;

# set the status variable(s)
for status_var in $status_var; do eval $status_var'=('${status[@]}')'; done;

if [ $count -le 1 ] && ( [ -z "$exit_code" ] || [ $watchdog_status -eq 0 ] ); then
    [ -n "$watch_pids" ] && return ${status[0]};
    [ $watchdog_status -eq 0 ] && return ${status[${#status[@]}-1]}
    return ${status[${#status[@]}-2]};
fi;

return ${watchdog_status%%[^0-9]*}

} 103>&$olog 101>&1 102>&2 >/dev/null 2>&1
}
export -f watchdog
"$BASHD_LOG" INFO "bash_debug.sh: defining function 'watchdog'"
fi



# xtrace calls (set -x behaviour)
# code portions to trace shall be embedded within 'xtrace_begin' and 'xtrace_end' statements
# xtrace messages will be prefixed as defined by the BASHD_XTRACE_PS4 variable
# if unset, default format will be <date-iso> XTRACE   [<script>/<function>#<lineno>] '
# where date-iso is formatted as YYYY-MM-DD'T'hh:mm:ss

function XTRACE_BEGIN {
	BASHD_XTRACE_LEVEL=$(( BASHD_XTRACE_LEVEL + 1 ))
}
export -f XTRACE_BEGIN

function xtrace_begin {
	BASHD_XTRACE_PS4_COPY="$PS4"
	BASHD_XTRACE_LEVEL=0
	BASHD_XTRACE_ENV=`shopt -po expand_aliases`
	PS4="$BASHD_XTRACE_PS4"
	[ -z "$PS4" ] && PS4='\001`date +%Y-%m-%dT%T` XTRACE   [$0/${FUNCNAME[0]}#${LINENO}] '
	shopt -s expand_aliases
	alias xtrace_begin='XTRACE_BEGIN 2>/dev/null'
	alias xtrace_end='XTRACE_END 2>/dev/null'
	set -x
	XTRACE_BEGIN 2>/dev/null
}
export -f xtrace_begin
"$BASHD_LOG" INFO "bash_debug.sh: defining function 'xtrace_begin'"

function XTRACE_END {
	set +x
	BASHD_XTRACE_LEVEL=$(( BASHD_XTRACE_LEVEL - 1 ))
	if [ $BASHD_XTRACE_LEVEL -lt 0 ]
	then
		"$BASHD_LOG" WARNING $*
		BASHD_XTRACE_LEVEL=0
	elif [ $BASHD_XTRACE_LEVEL == 0 ]
	then
		alias xtrace_end='XTRACE_END NOTRACE!'
		unalias xtrace_begin
		$BASHD_XTRACE_ENV
		PS4="$BASHD_XTRACE_PS4_COPY"
	else
		set -x
	fi
}
export -f XTRACE_END



#trap function applicable to trace (cf. next function 'trace')
function trace_trap {
	declare -ai status_array=( "$?" "${PIPESTATUS[@]}" )
	# remove trace
	declare xtrace=`set +o | grep xtrace`;

	declare cmd="$BASH_COMMAND"
	declare called_by="${FUNCNAME[1]}"
#	echo "DBG $0 STATUS=${status[@]} CMD=$cmd caller=$called_by/${FUNCNAME[@]}" 1>&2
	declare -i status=
	if [ ${#status_array[@]} -gt 1 ] && [ ${status_array[0]} -ne ${status_array[1]} ]; then
		status=0
	else
		status=${status_array[0]}
	fi

	case "$called_by" in
#	"") return 0;;
	"${FUNCNAME[0]}") return 0;;
	"trace") return 0;;
	"xcmd") return 0;;
	"bashd_log") return 0;;
	"$BASHD_LOG") return 0;;
	"smartkill") return 0;;
	"watchdog") return 0;;
	esac

	[ -n "$called_by" ] || called_by="main" 

#	set | grep '^BASHD_' 1>&2

	declare ac="${cmd%% *}"
#	echo AC=$ac $cmd

	# we do not monitor ourselves
	[ "$ac" == "trace" ] && return 0
	( [ "$ac" == "xcmd" ] || [ "$ac" == "watchdog" ] ) && ac=`declare -a c=(); $ac -n c $cmd; echo ${c[0]}`

	declare -i trace_item
	for (( trace_item=0; trace_item < ${#BASHD_TRACE_OPT[@]}; trace_item++ ))
	do
		# look in "BASHD_TRACE_CMD
		set -f; declare -a commands=(${BASHD_TRACE_CMD[$trace_item]}); set +f
#		echo "CMDS=$commands" 1>&2
		for dc in "${commands[@]}"
		do
			declare func="${dc##*@}"
			if [ "$func" != "$dc" ]; then
				[ -n "${called_by%%$func}" ] && continue
				dc="${dc%@*}"
			fi

			if [ "${dc:0:1}" == "#" ]; then
				[ "`type -t $ac 2>/dev/null`" != "${dc:1}" ] && continue
			elif [ -n "${ac%%$dc}" ]; then
				continue
			fi

			declare options="X${BASHD_TRACE_OPT[$trace_item]}"
			declare verbose=false; [ -z "${options%%*v*}" ] && verbose=true
			[ $status -eq 0 ] && ! $verbose && return 0

			# found it so print trace message
			cmd=`printf "$cmd" | gawk '{while(getline line) {$0=$0"["line"]"}; print}'`

			declare ecmd=false
			[ -z "${options%%*n*}" ] && [ $status -gt 0 ] && ecmd=true
			$ecmd || ( [ -z "${options%%*N*}" ] && ( ( $verbose && [ $status -eq 0 ] ) || ! $verbose ) ) && ecmd=true
			if $ecmd; then 
				declare assign=`echo "$cmd" | egrep '^[a-zA-Z_][a-zA-Z_0-9]*(\[.**\])*='`
				if [ -z "$assign" ]
				then
					declare mcmd=`echo "$cmd" | gawk '{gsub(/[<>&]/,"\\\\\\\\" "&"); print ": "$0}'`
					ecmd=`(PS4= ; set -x; eval "$mcmd" ; set +x ) 2>&1 | head -2 | tail -1 | cut -c3-`
				else
					ecmd=`(PS4= ; set -x; eval $cmd ; set +x ) 2>&1 | head -2 | tail -1`
				fi
				ecmd="TRACE above command expanded: {$ecmd}"
			else
				ecmd=""
			fi

			declare msg="TRACE[$0/$called_by#${BASH_LINENO[0]}] {${cmd[@]}}"
			if [ $status -gt 0 ]
			then
				msg="$msg returned $status"
				declare exit_f="${BASHD_TRACE_EXIT[$trace_item]}"
				if [ -n "$exit_f" ]
				then
					msg="$msg, calling $exit_f()"
				fi
				declare errtype="ERROR"; [ -z "${options%%*w*}" ] && errtype=WARNING
				"$BASHD_LOG" $errtype "$msg"

				[ -n "$ecmd" ] && "$BASHD_LOG" INFO "$ecmd"

				if [ -n "$exit_f" ]
				then
					$exit_f $status "$0" ${BASH_LINENO[0]} $called_by "$cmd"
					status=$?
				fi
				if [ -z "${options%%*a*}" ]
				then
					"$BASHD_LOG" WARNING "TRACE[$0/$called_by#${BASH_LINENO[0]}] aborting ($status)" 
					exit $status
				fi
			else
				"$BASHD_LOG" INFO "$msg starting..."
				[ -n "$ecmd" ] && "$BASHD_LOG" INFO "$ecmd"
			fi

			$xtrace; return 0
		done
	done
	$xtrace; return 0
}
export -f trace_trap

# trace definition function
# execute trace -h for usage notes
function trace {
declare USAGE=`cat << :usage
The $FUNCNAME function traces the execution of bash scripts, calling the log function to display/log the trace messages.

Usage: $FUNCNAME [ -h | -i | -s | -r | [ -q ] <trace-rule1> <trace-rule2> ... ]

Options and Arguments:

 -h               Displays command usage
 -i               Displays the currently defined trace rules
 -s               Suspend trace
 -r               Resume trace
 -q               Cancels currently defined trace rules. -q option may be followed by new trace rule definitions.
                  When -q is omitted before trace-rule definitions, the given trace rules are pre-pended
                  to the currently active ones.

 <trace-rule>:    Defined as [ <trace-options> ] [ <cmd-pattern1>  <cmd-pattern2> ... ]
                  Trace rules specify the trace (defined by <trace-options>) to be applied to commands matching
                  given command patterns. when no command pattern is given, trace is applied to all commands
                  (equivalent to specifying <cmd-pattern>='*')
                  For each command executed, the first matching trace rule is searched for in the order trace
                  rules are defined on the command line.

 <trace-options>:
  -d              Generate trace output only when commands return non-zero status (default behaviour)
  -v              In addition to the above, generate trace output at the time commands are invoked
  -n | -N         Displays the command expanded arguments (may not be always exact) after the trace message.
                  When -n is used, command expansion is displayed only for commands returning non-zero status.
                  When -N is used together with -v, command expansion is displayed when the commands are invoked.
  -w              Trace messages generated on commands returning non-zero are qualified with WARNING instead
                  of ERROR
  -x <exit-cmd>   The command <exit-cmd> will be triggered when commands return a non-zero status
  -a              The command shell will exit after commands return non-zero. In case an exit command is
                  defined (through the -x option), exit will be commanded only if the exit function returns
                  non-zero

 <cmd-pattern>:   Defined as <cmd-name-pattern>[@<function-block-pattern>]
                  The command name is matched against <cmd-name-pattern>.
                  Optionally, the pattern suffix '@<function-name-pattern>' can be used to further restrict
                  the applicable commands according to the function block from which the commands are triggered.
                  The main execution bash block (outside of any function definitions) is referred to as 'main'.
                  All patterns implement the basic shell filename wildcard characters (*, ?, etc)
                  When <cmd-name-pattern> is prefixed by a '#', it is interpreted as a type of command, like the
                  'type -t' bash builtin function would return (e.g. known command types are builtin, file, alias, etc)


Usage Examples:

  1) #trace  , or  #trace -d '*'  , or #trace '*'
  Default trace is activated for all commands, generating trace output for all commands returning non-zero

  2) #trace -v mycommand -d  , or #trace -d; trace -v mycommand
  All commands are traced for errors. Also triggering of command 'mycommand' is traced.

  3) #trace 'prefix_*@main'
  Only commands named as "prefix_*" and executed from the main block are traced for errors

  4) #trace '*@list_*'
  all command triggered from any function named as "list_*" are traced for errors

  5) #trace -x error_handler -a mycommand
  Errors of command 'mycommand' are trapped. On error, the command 'error_handler' is called and if it returns non-zero,
  the shell is ended.

  6) #trace -q
  Stop trace

  7) #trace -q -v
  Trace is redefined for verbose trace on all commands

  8) #trace -v '#file'
  Trace all file based commands for errors (excluding e.g. shell builtin functions)

:usage
`
	if [ "$1" == "-h" ]; then
		echo "$USAGE"
		return 0
	elif [ "$1" == "-i" ]; then
		declare -i trace_item
		for (( trace_item=0; trace_item < ${#BASHD_TRACE_OPT[@]}; trace_item++ ))
		do
			declare opt="${BASHD_TRACE_OPT[$trace_item]}"
			[ -z "$opt" ] && opt="d"
			declare exit_f="${BASHD_TRACE_EXIT[$trace_item]}"
			[ -n "$exit_f" ] && opt="${opt}x $exit_f"
			printf "%s\n" "-$opt${BASHD_TRACE_CMD[$trace_item]}"
		done
		return 0
	elif [ "$1" == "-s" ]; then
		trap "" DEBUG
		trap "" ERR
		return 0
	elif [ "$1" == "-r" ]; then
		[ ${#BASHD_TRACE_OPT[@]} -gt 0 ] && trap ':' ERR && trap "trace_trap" DEBUG
		return 0
	elif [ "$1" == "-q" ]; then
		shopt -u extdebug
		set +o history
		set +o functrace
		trace -s
		unset BASHD_TRACE_OPT BASHD_TRACE_EXIT BASHD_TRACE_CMD
		export -n BASHD_TRACE_OPT BASHD_TRACE_EXIT BASHD_TRACE_CMD
		shift
		[ $# -eq 0 ] && return 0
	fi

	declare cur_opts=`trace -i`
	unset BASHD_TRACE_OPT BASHD_TRACE_EXIT BASHD_TRACE_CMD
	export -n BASHD_TRACE_OPT BASHD_TRACE_EXIT BASHD_TRACE_CMD
	if [ -n "$cur_opts" ] && [ -n "`trap | grep 'DEBUG$' | grep trace_trap`" ]; then
		set -f
		if [ $# -gt 0 ]; then
			trace "$@" " " $cur_opts
		else
			trace $cur_opts -d *
		fi
		declare status=$?
		set +f
		return $status
	fi

	if [ ${BASH_VERSINFO[0]} -lt 3 ] || ( [ ${BASH_VERSINFO[0]} -eq 3 ] && [ ${BASH_VERSINFO[1]} -lt 2 ] ); then
		echo "trace requires bash version 3.2 or higher, trace capability is disabled" 1>&2
		return 1
	fi


	declare num_items=0
	while [ -n "$1" ]
	do
		declare options=""
		declare exit_f=""
		declare cmd=""

		unset OPTIND

		declare nopt=""

		declare opt
		while getopts ":x:vnNwad" opt; do
		case $opt in
		x) exit_f="$OPTARG";;
		n) nopt="n";;
		N) nopt="N";;
		d) options="";;
		'?' | ':') "$BASHD_LOG" ERROR "$FUNCNAME: option '$OPTARG' is invalid" ; return 255;;
		*) options="$options$opt";;
		esac
		done

		options="$options$nopt"

		shift $(($OPTIND - 1))

		while [ -n "$1" ]
		do
			declare c=$1; shift
			[ "$c" == " " ] && continue
			[ "${c:0:1}" == "-" ] && break
			cmd="$cmd $c"
		done
		if [ -z "$cmd" ]; then cmd=' *'; fi

		BASHD_TRACE_OPT[$num_items]="$options"
		BASHD_TRACE_EXIT[$num_items]="$exit_f"
		BASHD_TRACE_CMD[$num_items]="$cmd"
#		echo "[$num_items] OPT=$options EXIT=$exit_f CMD=$cmd"
		num_items=$(( num_items + 1 ))
	done
	[ $num_items -eq 0 ] && BASHD_TRACE_OPT[0]="" && BASHD_TRACE_EXIT[0]="" && BASHD_TRACE_CMD[0]=" *"
	export BASHD_TRACE_OPT BASHD_TRACE_EXIT BASHD_TRACE_CMD

	shopt -s extdebug
	set -o history
	set -o functrace
	set -o errtrace
	trace -r
	return 0
}
export -f trace
"$BASHD_LOG" INFO "bash_debug.sh: defining function 'trace'"
( [ ${BASH_VERSINFO[0]} -lt 3 ] || ( [ ${BASH_VERSINFO[0]} -eq 3 ] && [ ${BASH_VERSINFO[1]} -lt 2 ] ) ) \
&& "$BASHD_LOG" WARNING "bash_debug.sh: function 'trace' requires bash version 3.2 or higher, trace capability will be disabled"

"$BASHD_LOG" INFO "bash_debug.sh: end of initialisations"

