/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.authentication;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

public class AuthorizationServerInterceptor implements ServerInterceptor {

	/**	Threaded key for context management	*/
	public static final Context.Key<Object> SESSION_CONTEXT = Context.key("session_context");
	/** Services/Methods allow request without Bearer token validation */
	private static List<String> ALLOW_REQUESTS_WITHOUT_TOKEN = Arrays.asList(
		""
	);
	
	@Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
		String callingMethod = serverCall.getMethodDescriptor().getFullMethodName();
		// Bypass to ingore Bearer validation
		if (ALLOW_REQUESTS_WITHOUT_TOKEN.contains(callingMethod)) {
			return Contexts.interceptCall(Context.current(), serverCall, metadata, serverCallHandler);
		}

		Status status;
		String validToken = metadata.get(TokenManager.AUTHORIZATION_METADATA_KEY);
		if (validToken == null || validToken.trim().length() <= 0) {
            status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
        } else if (!validToken.startsWith(TokenManager.BEARER_TYPE)) {
            status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
        } else {
            try {
            	Properties sessioncontext = SessionManager.getSessionFromToken(validToken);
            	Context context = Context.current().withValue(SESSION_CONTEXT, sessioncontext);
                return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
            } catch (Exception e) {
                status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
            }
        }

        serverCall.close(status, metadata);
        return new ServerCall.Listener<>() {
            // noop
        };
    }
}
