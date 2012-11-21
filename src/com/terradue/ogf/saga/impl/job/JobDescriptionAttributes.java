package com.terradue.ogf.saga.impl.job;


import org.ogf.saga.impl.attributes.AttributeType;
import org.ogf.saga.impl.attributes.AttributesImpl;
//import org.ogf.saga.impl.attributes.AttributesImpl.AttributeInfo;

public class JobDescriptionAttributes extends AttributesImpl {

    public JobDescriptionAttributes() {
    	addAttribute(JobDescription.APPLICATIONNAME, AttributeType.STRING, false,
                false, false, false);
    	addAttribute(JobDescription.APPLICATIONVERSION, AttributeType.STRING, false,
                false, false, false);
    	addAttribute(JobDescription.JOBNAME, AttributeType.STRING, false,
                false, false, false);
    	addAttribute(JobDescription.JOBDESCRIPTION, AttributeType.STRING, false,
                false, false, false);
        addAttribute(JobDescription.EXECUTABLE, AttributeType.STRING, false,
                false, false, false);
        addAttribute(JobDescription.ARGUMENTS, AttributeType.STRING, true,
                false, false, false);
        addAttribute(JobDescription.SPMDVARIATION, AttributeType.STRING, false,
                false, false, false);
        
        addAttribute(JobDescription.TOTALCPUCOUNT, AttributeType.INT, false,
                false, false, false);
        setDefaultValue(JobDescription.TOTALCPUCOUNT, "1");
        
        addAttribute(JobDescription.NUMBEROFPROCESSES, AttributeType.INT,
                false, false, false, false);
        setDefaultValue(JobDescription.NUMBEROFPROCESSES, "1");
        
        addAttribute(JobDescription.PROCESSESPERHOST, AttributeType.INT, false,
                false, false, false);
        setDefaultValue(JobDescription.PROCESSESPERHOST, "1");
        
        addAttribute(JobDescription.THREADSPERPROCESS, AttributeType.INT,
                false, false, false, false);
        setDefaultValue(JobDescription.THREADSPERPROCESS, "1");
        
        addAttribute(JobDescription.ENVIRONMENT, AttributeType.STRING, true,
                false, false, false);
        
        addAttribute(JobDescription.WORKINGDIRECTORY, AttributeType.STRING,
                false, false, false, false);
        setDefaultValue(JobDescription.WORKINGDIRECTORY, ".");

        addAttribute(JobDescription.INTERACTIVE, AttributeType.BOOL, false,
                false, false, false);
        setDefaultValue(JobDescription.INTERACTIVE, "false");
        
        addAttribute(JobDescription.INPUT, AttributeType.STRING, false, false,
                false, false);
        addAttribute(JobDescription.OUTPUT, AttributeType.STRING, false, false,
                false, false);
        addAttribute(JobDescription.ERROR, AttributeType.STRING, false, false,
                false, false);
        addAttribute(JobDescription.FILETRANSFER, AttributeType.STRING, true,
                false, false, false);
        
        addAttribute(JobDescription.CLEANUP, AttributeType.STRING, false,
                false, false, false);
        setDefaultValue(JobDescription.CLEANUP, "False");

        addAttribute(JobDescription.JOBSTARTTIME, AttributeType.INT, false,
                false, false, false);
        // odd: default value of JOBSTARTTIME is not an int  --mathijs
        
        addAttribute(JobDescription.WALLTIMELIMIT, AttributeType.INT, false,
                false, false, false);
        // odd: default value of WALLTIMELIMIT is not an int  --mathijs

        addAttribute(JobDescription.TOTALCPUTIME, AttributeType.INT, false,
                false, false, false);
        // odd: default value of TOTALCPUTIME is not an int  --mathijs

        addAttribute(JobDescription.TOTALPHYSICALMEMORY, AttributeType.FLOAT,
                false, false, false, false);
        // odd: default value of TOTALPHYSICALMEMORY is not a float  --mathijs

        addAttribute(JobDescription.CPUARCHITECTURE, AttributeType.STRING,
                false, false, false, false);
        addAttribute(JobDescription.OPERATINGSYSTEMTYPE, AttributeType.STRING,
                false, false, false, false);
        addAttribute(JobDescription.CANDIDATEHOSTS, AttributeType.STRING, true,
                false, false, false);
        addAttribute(JobDescription.ENDPOINT, AttributeType.STRING, false,
            false, false, false);
        addAttribute(JobDescription.JOBMANAGER, AttributeType.STRING, false,
            false, false, false);
        addAttribute(JobDescription.CANDIDATEHOSTS, AttributeType.STRING, true,
            false, false, false);
        addAttribute(JobDescription.QUEUE, AttributeType.STRING, false, false,
                false, false);
//        addAttribute("machine.queue", AttributeType.STRING, false, false,
//                false, false);
        addAttribute(JobDescription.JOBPROJECT, AttributeType.STRING, true,
                false, false, false);
        addAttribute(JobDescription.JOBCONTACT, AttributeType.STRING, true,
                false, false, false);
        addAttribute(JobDescription.JOBUID, AttributeType.STRING, false,
            false, false, false);
        addAttribute(JobDescription.SPMD_POSIX, AttributeType.STRING, false,
                false, false, false);
    }

    public JobDescriptionAttributes(JobDescriptionAttributes orig) {
        super(orig);
    }
}
