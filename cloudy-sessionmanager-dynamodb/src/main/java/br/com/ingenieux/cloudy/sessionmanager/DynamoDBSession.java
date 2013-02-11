package br.com.ingenieux.cloudy.sessionmanager;

import java.security.Principal;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBSession extends StandardSession {
	private static final long serialVersionUID = 5269149923517338254L;

	public DynamoDBSession(Manager manager) {
		super(manager);
	}

	@Override
	public void setAttribute(String name, Object value) {
		this.setAttribute(name, value, true);
	}

	@Override
	public void setAttribute(String name, Object value, boolean notify) {
		super.setAttribute(name, value, notify);
		
		// TODO: Append Attribute
	}

	@Override
	public void setPrincipal(Principal principal) {
		super.setPrincipal(principal);
	}

	void setPrincipalInternal(Principal principal) {
		super.setPrincipal(principal);
	}

	@Override
	public void removeAttribute(String name) {
		this.removeAttribute(name, true);
	}

	@Override
	public void removeAttribute(String name, boolean notify) {
		super.removeAttribute(name, notify);
		
		// TODO: Remove Attribute
	}

	@Override
	public void setValid(boolean isValid) {
		super.setValid(isValid);
	}

	@Override
	public boolean isValid() {
		Logger log = LoggerFactory.getLogger(getClass());
		if (!this.isValid) {
			log.debug(getIdInternal() + " isValid is false...");
			return false;
		}
		if (ACTIVITY_CHECK && accessCount.get() > 0) {
			return true;
		}
		if (maxInactiveInterval >= 0) {
			long timeNow = System.currentTimeMillis();
			int timeIdle = (int) ((timeNow - thisAccessedTime) / 1000L);
			if (timeIdle >= maxInactiveInterval) {
				log.debug(String.format("%s timeIdle (%s) >= maxInactiveInterval (%s)",
						getIdInternal(),
						timeIdle,
						maxInactiveInterval));
				expire(true);
			}
		}

		return (this.isValid);
	}

	@Override
	public String toString() {
		return "DynamoDBSession[" + getIdInternal() + "]";
	}

	protected DynamoDBStore getStore() {
		Manager mgr = getManager();
		if (mgr instanceof DynamoDBManager) {
			return ((DynamoDBManager) mgr).getStore();
		}
		return null;
	}
}