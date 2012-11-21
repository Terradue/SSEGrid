package com.terradue.ogf.saga.impl.job;

import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;

/**
 * Implementation of the JobDescription interface. This implementation must be
 * used for submission to a GRID middleware using Globus 4.0.x and PBS job
 * scheduler This class is a workaround to the current javaSAGA implementation
 * of the API because the Globus adaptor does not read the correct attribute
 * representing the queue for the job scheduler.
 * 
 * @author emathot
 * 
 * 
 * 
 */
public class JobDescriptionImpl extends
		org.ogf.saga.impl.job.JobDescriptionImpl implements JobDescription {

	//JobDescriptionAttributes attributes;
	JobDescriptionAttributes attributes;

	public JobDescriptionImpl() {
		super();
		attributes = new JobDescriptionAttributes();
		//attributes = this.attributes;
	}

	/**
	 * This constructor creates an exact clone of the JobDescription provided in
	 * argument
	 * 
	 * @param orig
	 *            is the original JobDescription created generally by JobFactory
	 * 
	 */
	public JobDescriptionImpl(JobDescriptionImpl orig) {
		super();
		// attributes = new JobDescriptionAttributes(orig.attributes);
		attributes = new JobDescriptionAttributes();
		String[] orig_attr;
		try {
			orig_attr = orig.listAttributes();
			for (int i = 0; i < orig_attr.length; i++) {
				if (orig.isVectorAttribute(orig_attr[i])) {
					// System.out.println(orig_attr[i]+" : "+orig.getVectorAttribute(orig_attr[i]));
					if (orig.getVectorAttribute(orig_attr[i]) == null)
						continue;
					attributes.setVectorAttribute(orig_attr[i],
							orig.getVectorAttribute(orig_attr[i]));
					if(super.isWritableAttribute(orig_attr[i])){
						super.setVectorAttribute(orig_attr[i],
							orig.getVectorAttribute(orig_attr[i]));
					}
				} else {
					// System.out.println(orig_attr[i]+" : "+orig.getAttribute(orig_attr[i]));
					if (orig.getAttribute(orig_attr[i]) == "")
						continue;
					attributes.setAttribute(orig_attr[i],
							orig.getAttribute(orig_attr[i]));
					if(super.isWritableAttribute(orig_attr[i])){
						super.setAttribute(orig_attr[i],
							orig.getAttribute(orig_attr[i]));
					}
				}
			}
		} catch (Exception e) {
			System.out
					.println("Error during transformation of jobDescription: "
							+ e.getMessage());
		}
		attributes = this.attributes;
	}

	public Object clone() throws CloneNotSupportedException {
		JobDescriptionImpl o = (JobDescriptionImpl) super.clone();
		o.attributes = new JobDescriptionAttributes(attributes);
		return o;
	}

	public String[] findAttributes(String... patterns)
			throws NotImplementedException, BadParameterException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		return attributes.findAttributes(patterns);
	}

	public String getAttribute(String key) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, IncorrectStateException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		return attributes.getAttribute(key);
	}

	public String[] getVectorAttribute(String key)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			IncorrectStateException, DoesNotExistException, TimeoutException,
			NoSuccessException {
		return attributes.getVectorAttribute(key);
	}

	public boolean existsAttribute(String key) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		return attributes.existsAttribute(key);
	}

	public boolean isReadOnlyAttribute(String key)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		return attributes.isReadOnlyAttribute(key);
	}

	public boolean isRemovableAttribute(String key)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		return attributes.isRemovableAttribute(key);
	}

	public boolean isVectorAttribute(String key)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		return attributes.isVectorAttribute(key);
	}

	public boolean isWritableAttribute(String key)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		return attributes.isWritableAttribute(key);
	}

	public String[] listAttributes() throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, TimeoutException, NoSuccessException {
		return attributes.listAttributes();
	}

	public void removeAttribute(String key) throws NotImplementedException,
			AuthenticationFailedException, AuthorizationFailedException,
			PermissionDeniedException, DoesNotExistException, TimeoutException,
			NoSuccessException {
		attributes.removeAttribute(key);
		try{
			if(super.isRemovableAttribute(key)){
				super.removeAttribute(key);
			}
		}
		catch (DoesNotExistException dne){}
	}

	public void setAttribute(String key, String value)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			IncorrectStateException, BadParameterException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		attributes.setAttribute(key, value);
		try {
			if(super.isWritableAttribute(key)){
				super.setAttribute(key,value);
			}
		}
		catch (DoesNotExistException dne){}
	}

	public void setVectorAttribute(String key, String[] values)
			throws NotImplementedException, AuthenticationFailedException,
			AuthorizationFailedException, PermissionDeniedException,
			IncorrectStateException, BadParameterException,
			DoesNotExistException, TimeoutException, NoSuccessException {
		attributes.setVectorAttribute(key, values);
		try{
			if(super.isVectorAttribute(key) && super.isWritableAttribute(key)){
				super.setVectorAttribute(key,values);
			}
		}
		catch (DoesNotExistException dne){}
	}

}
