package com.terradue.ssegrid.demo;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.monitoring.Callback;
import org.ogf.saga.monitoring.Metric;
import org.ogf.saga.monitoring.Monitorable;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import org.w3c.dom.Document;


import com.terradue.ogf.saga.impl.job.JobImpl;
import com.terradue.ogf.saga.impl.job.JobImpl;
import com.terradue.ogf.saga.impl.job.JobServiceImpl;
import com.terradue.ssegrid.sagaext.JobServiceAssistant;
import com.terradue.ssegrid.sagaext.MyProxyClient;
import com.terradue.ssegrid.sagaext.ProcessingRegistry;


public class TestJobGT4 implements Callback {

    public static void main(String[] args) {

        // Make sure that the SAGA engine picks the javagat adaptor for
        // JobService.
        System.setProperty("JobService.adaptor.name", "javaGAT");

        String server = //"https://ce1.intern.vgt.vito.be:8443";
        				//"https://ify-ce03.terradue.com:8443";
        				//"http://maps.terradue.com/ssegrid/examples/ResourceManager/VITOGrid_GLUE.xml";
        				"http://storage.terradue.com/ssegrid/T2Grid_GLUE.xml";
      
        if (args.length > 1) {
            System.err.println("Usage: java demo.job.TestJob [<serverURL>]");
            System.exit(1);
        } else if (args.length == 1) {
            server = args[0];
        }
        
        try {
            Session session = SessionFactory.createSession(true);
    
            URL serverURL = URLFactory.createURL(server);/*f.toURL().toString()*/
            	//"file:////Users/fbarchetta/dev/SAGA4SSEGrid.new/examples/ResourceManager/VITOGrid_GLUE.xml");
            
            // Create a preferences context for JavaGAT.
            // The "preferences" context is special: it is extensible.
            
            Context context = ContextFactory.createContext("preferences");

            // Make sure that javaGAT picks SGE.
            // TODO: Remove preferences context. How to do this then?
            context.setAttribute("ResourceBroker.adaptor.name", "wsgt4new");

            context.setAttribute("wsgt4new.factory.type", "PBS");
            //context.setAttribute("File.adaptor.name", "Local,GridFTP");

            //MyProxyClient.delegateProxyFromMyProxyServer("ify-ce03.terradue.com", 7512, "emathot", "myproxy", 604800, context);
            /*            
            String[] attrs = context.listAttributes();
            for (int i=0;i<attrs.length;i++){
            	System.out.println(attrs[i] + ":" + context.getAttribute(attrs[i]));
            }*/
            //System.out.println("*********");
            
            //session.addContext(context);
            // Create the JobService.
            //JobService js = JobFactory.createJobService(serverURL);
			
            JobServiceImpl js = (JobServiceImpl)com.terradue.ogf.saga.impl.job.JobFactory.createJobService(serverURL);
            // Create a job: /bin/hostname executed on 10 nodes.
			ProcessingRegistry pr = new ProcessingRegistry(true);

			// prepare Map with all persistent substitution variable
			java.util.Map<String, String> WPSSubstitutionVar = new java.util.HashMap<String, String>();
			
			WPSSubstitutionVar.put("WPS_JOB_INPUTS_DIR",
					"/Users/fbarchetta/dev/SAGA4SSEGrid.new/examples/dep_proc/");
			//js.addSubstitutionVariables(WPSSubstitutionVar);
			// Registration of a processing described by a JSDL template
			com.terradue.ogf.saga.impl.job.JobDescription jd = (com.terradue.ogf.saga.impl.job.JobDescription)com.terradue.ogf.saga.impl.job.JobFactory
											.createJobDescription(pr.getJSDLFromProc("ImportMODIS"));
			jd.setAttribute(JobDescription.INPUT, "");
            //JobDescription jd = JobFactory.createJobDescription();
            jd.setAttribute(JobDescription.EXECUTABLE, "/bin/hostname");
			jd.setAttribute(JobDescription.NUMBEROFPROCESSES, "2");
            jd.setAttribute("Queue", "infinite");
            jd.setAttribute(JobDescription.OUTPUT, "hostname.out");
            jd.setAttribute(JobDescription.ERROR, "stderr");
            jd.setVectorAttribute(JobDescription.FILETRANSFER,
                    new String[] { "hostname.out < hostname.out" , "stderr < stderr"});
            
            // Create the job, run it, and wait for it.
            JobImpl job = js.createJob(jd);
           /*JobImpl job2 = js.createJob(jd);
            JobImpl job3 = js.createJob(jd);*/
		/*	String [] map = jd.listAttributes();
			for(int i = 0; i < map.length;i++){
				if (jd.isVectorAttribute(map[i])){
					for (int j = 0; j<jd.getVectorAttribute(map[i]).length; j++){
						System.out.println("attrV " + map[i] + " = " +  jd.getVectorAttribute(map[i])[j]);
					}
				}else{
					System.out.println("attr " + map[i] + " = " +  jd.getAttribute(map[i]));
				}
			}*/    
			JobServiceAssistant jsa = new JobServiceAssistant(js);
			jsa.addSubstitutionVariables(WPSSubstitutionVar);
			java.util.Map WPS_INPUT = new java.util.HashMap<String, String>();
			WPS_INPUT.put("WPS_INPUT_roiTopLeftLat", "72");
			WPS_INPUT.put("WPS_INPUT_roiTopLeftLon", "-15");
			WPS_INPUT.put("WPS_INPUT_roiBottomRightLat", "28");
			WPS_INPUT.put("WPS_INPUT_roiBottomRightLon", "60");
			WPS_INPUT.put("WPS_INPUT_stopOnError", "true");
			WPS_INPUT.put("WPS_INPUT_numberOfTasks", "3");
			jsa.substituteSimpleInputs(jd, WPS_INPUT);
			java.net.URL inputURLList = new java.net.URL("http://storage.terradue.com/ssegrid/importMODIS/InputURLList.xml");
			URLConnection inputURLListuc = inputURLList.openConnection();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document inputXML = db.parse(inputURLListuc.getInputStream());
			java.util.Map inputXMLMap = new java.util.HashMap<String, org.w3c.dom.Document>();
			inputXMLMap.put("InputURLs", inputXML);
			JobImpl jobs = ((JobServiceImpl) js).createNJob(jd);
			//jobs.jobArray = new Job[3];
			job.setTaskID("1");			
			/*jobs.jobArray[0] = job;
			job2.setTaskID("2");			
			jobs.jobArray[1] = job2;
			job3.setTaskID("3");			
			jobs.jobArray[2] = job3;
			jsa.writeComplexInputs(job, inputXMLMap);
			*/
            job.addCallback(Job.JOB_STATE, new TestJobGT4());
            job.addCallback(Job.JOB_STATEDETAIL, new TestJobGT4());
            Session mysession = job.getSession();
           /* context = mysession.listContexts()[0];
            String[] attris = context.listAttributes();
            for (int i=0;i<attris.length;i++){
            	System.out.println(attris[i] + ":" + context.getAttribute(attris[i]));
            }*/            
            System.out.println("GO!");
            job.run();
            System.out.println("submitted!");
            job.waitFor();
        } catch (Throwable e) {
            System.out.println("Got exception " + e);
            e.printStackTrace();
            //e.getCause().printStackTrace();
        }
    }

    // Callback monitors job.
    public boolean cb(Monitorable m, Metric metric, Context ctxt) {
        try {
            String value = metric.getAttribute(Metric.VALUE);
            String name = metric.getAttribute(Metric.NAME);
            System.out.println("Callback called for metric "
                    + name + ", value = " + value);
        } catch (Throwable e) {
            System.err.println("error" + e);
            e.printStackTrace(System.err);
        }
        // Keep the callback.
        return true;
    }
}
