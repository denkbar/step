/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.core.deployment;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.digest.DigestUtils;

import step.core.access.Preferences;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSetting;

@Singleton
@Path("admin")
public class AdminServices extends AbstractServices {
	
	protected ControllerSettingAccessor controllerSettingsAccessor;

	private static final String MAINTENANCE_MESSAGE_KEY = "maintenance_message";
	
	@PostConstruct
	public void init() {
		controllerSettingsAccessor = new ControllerSettingAccessor(controller.getContext().getMongoClientSession());
	}

	@POST
	@Secured(right="user-write")
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/user")
	public void save(User user) {
		UserAccessor userAccessor = getContext().getUserAccessor();

		User previousUser = userAccessor.get(user.getId());
		if(previousUser == null) {
			// previousUser is null => we're creating a new user
			// initializing password
			resetPwd(user);
		}
		
		userAccessor.save(user);
	}

	@DELETE
	@Secured(right="user-write")
	@Path("/user/{id}")
	public void remove(@PathParam("id") String username) {
		getContext().getUserAccessor().remove(username);
	}
	
	@GET
	@Secured(right="user-read")
	@Path("/user/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public User getUser(@PathParam("id") String username) {
		return getContext().getUserAccessor().getByUsername(username);
	}
	
	@GET
	@Secured(right="user-read")
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	public List<User> getUserList() {
		return getContext().getUserAccessor().getAllUsers();
	}
	
	private static String INITIAL_PWD = "init";
	
	public static class ChangePwdRequest {
		
		private String oldPwd;
		
		private String newPwd;

		public ChangePwdRequest() {
			super();
		}

		public String getOldPwd() {
			return oldPwd;
		}

		public void setOldPwd(String oldPwd) {
			this.oldPwd = oldPwd;
		}

		public String getNewPwd() {
			return newPwd;
		}

		public void setNewPwd(String newPwd) {
			this.newPwd = newPwd;
		}
	}
	
	@GET
	@Secured
	@Path("/maintenance/message")
	public String getMaintenanceMessage() {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_MESSAGE_KEY);
		return setting!=null?setting.getValue():null;
	}
	
	@POST
	@Secured(right="admin")
	@Path("/maintenance/message")
	public void setMaintenanceMessage(String message) {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_MESSAGE_KEY);
		if(setting == null) {
			setting = new ControllerSetting();
			setting.setKey(MAINTENANCE_MESSAGE_KEY);
		}
		setting.setValue(message);
		controllerSettingsAccessor.save(setting);
	}
	
	@POST
	@Secured
	@Path("/myaccount/changepwd")
	public void resetMyPassword(@Context ContainerRequestContext crc, ChangePwdRequest request) {
		Session session = (Session) crc.getProperty("session");
		User user = getContext().getUserAccessor().getByUsername(session.username);
		if(user!=null) {
			user.setPassword(encryptPwd(request.getNewPwd()));
			getContext().getUserAccessor().save(user);			
		}
	}
	
	@GET
	@Secured
	@Path("/myaccount/preferences")
	public Preferences getPreferences(@Context ContainerRequestContext crc) {
		Session session = (Session) crc.getProperty("session");
		if(session!=null) {
			User user = getContext().getUserAccessor().getByUsername(session.username);
			if(user!=null) {
				return user.getPreferences();			
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	@POST
	@Secured
	@Path("/myaccount/preferences/{id}")
	public void putPreference(@Context ContainerRequestContext crc, @PathParam("id") String preferenceName, Object value) {
		Session session = (Session) crc.getProperty("session");
		if(session!=null) {
			User user = getContext().getUserAccessor().getByUsername(session.username);
			if(user!=null) {
				if(user.getPreferences()==null) {
					Preferences prefs = new Preferences();
					user.setPreferences(prefs);
				}
				user.getPreferences().put(preferenceName, value);
				getContext().getUserAccessor().save(user);			
			}			
		}
	}
	
	@POST
	@Secured
	@Path("/myaccount/preferences")
	public void putPreference(@Context ContainerRequestContext crc, Preferences preferences) {
		Session session = (Session) crc.getProperty("session");
		if(session!=null) {
			User user = getContext().getUserAccessor().getByUsername(session.username);
			if(user!=null) {
				user.setPreferences(preferences);
				getContext().getUserAccessor().save(user);			
			}			
		}
	}
	
	@POST
	@Secured(right="user-write")
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/user/{id}/resetpwd")
	public void resetPassword(@PathParam("id") String username) {
		User user = getContext().getUserAccessor().getByUsername(username);
		resetPwd(user);
		getContext().getUserAccessor().save(user);
	}

	private void resetPwd(User user) {
		user.setPassword(encryptPwd(INITIAL_PWD));
	}

	private String encryptPwd(String pwd) {
		return DigestUtils.sha512Hex(pwd);
	}}
