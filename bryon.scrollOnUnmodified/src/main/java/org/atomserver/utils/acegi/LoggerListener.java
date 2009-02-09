/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.atomserver.utils.acegi;


import org.acegisecurity.event.authentication.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ClassUtils;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class LoggerListener implements ApplicationListener {
    //~ Static fields/initializers =====================================================================================

    private static final Log logger = LogFactory.getLog(LoggerListener.class);
    
    private boolean logAuthenticationSuccessEvents = true;
    
    //~ Methods ========================================================================================================

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AbstractAuthenticationEvent) {
            AbstractAuthenticationEvent authEvent = (AbstractAuthenticationEvent) event;
            
            if (!logAuthenticationSuccessEvents && authEvent instanceof AuthenticationSuccessEvent) {
            	return;
            }

            if (logger.isWarnEnabled()) {
                String message = "Authentication event " + ClassUtils.getShortName(authEvent.getClass()) + ": "
                    + authEvent.getAuthentication().getName() + "; details: "
                    + authEvent.getAuthentication().getDetails();

                if (event instanceof AbstractAuthenticationFailureEvent) {
                    message = message + "; exception: "
                        + ((AbstractAuthenticationFailureEvent) event).getException().getMessage();
                }

                logger.warn(message);
            }
        }
    }

	public boolean isLogAuthenticationSuccessEvents() {
		return logAuthenticationSuccessEvents;
	}

	public void setLogAuthenticationSuccessEvents(
			boolean logAuthenticationSuccessEvents) {
		this.logAuthenticationSuccessEvents = logAuthenticationSuccessEvents;
	}
}

