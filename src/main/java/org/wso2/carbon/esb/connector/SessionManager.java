/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.esb.connector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * The Session Manager maintains the bind connection with smsc for reuse until a unbind is triggered.
 */
public class SessionManager {

    protected Log log = LogFactory.getLog(this.getClass());
    private Map<String, SMPPSession> smppSessionList;
    private static SessionManager sessionManager;

    private SessionManager() {
        smppSessionList = new HashMap();
    }

    public static synchronized SessionManager getInstance() {
        if (sessionManager == null)
            sessionManager = new SessionManager();
        return sessionManager;
    }

    private String getKey(String host, int port, String systemId) {
        return host + SMPPConstants.CONCT_CHAR + port + SMPPConstants.CONCT_CHAR + systemId;

    }

    /**
     * @param enquireLinkTimer Enquire Link Timer for bind properties.
     * @param transactionTimer Transaction  Timer for bind properties.
     * @param host host name or ip of the SMSC.
     * @param port connection port of the SMSC.
     * @param bindParameter the list of parameter needed for the SMSC connectivuty.
     * @throws IOException
     */
    public SMPPSession getSmppSession(int enquireLinkTimer, int transactionTimer, String host, int port,
                                      BindParameter bindParameter) throws IOException {
        SMPPSession smppSession = smppSessionList.get(getKey(host, port, bindParameter.getSystemId()));
        Boolean isSessionBound=false;
        if(smppSession != null){
            isSessionBound=smppSession.getSessionState().isBound();
        }
        if (!isSessionBound) {
            smppSession = new SMPPSession();
            smppSession.setEnquireLinkTimer(enquireLinkTimer);
            smppSession.setTransactionTimer(transactionTimer);
            smppSession.connectAndBind(host, port, bindParameter);
            if (log.isDebugEnabled()) {
                log.debug("A new session is Connected and bind to " + host);
            }
            smppSessionList.putIfAbsent(getKey(host, port, bindParameter.getSystemId()), smppSession);
        }
        return smppSession;
    }

    /**
     * @param host host name or ip of the SMSC.
     * @param port connection port of the SMSC.
     * @param systemId systemID used to bind the connection to SMSC.
     */
    public void unbind(String host, int port, String systemId) {
        SMPPSession smppSession = smppSessionList.get(getKey(host, port, systemId));
        if (smppSession != null) {
            if (log.isDebugEnabled()) {
                log.debug("Unbinding the connection with SMSC");
            }
            smppSession.unbindAndClose();
            smppSessionList.remove(getKey(host, port, systemId));
        } else {
            log.info("No active smpp session found for unbinding");
        }
    }
}
