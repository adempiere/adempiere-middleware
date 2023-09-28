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

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import java.util.concurrent.Executor;

import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Status;

public class TokenManager extends CallCredentials {

	public static final String BEARER_TYPE = "Bearer";

    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
    public static final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");
    
    private String value;

    public TokenManager(String value) {
        this.value = value;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier metadataApplier) {
        executor.execute(() -> {
            try {
                Metadata headers = new Metadata();
                headers.put(AUTHORIZATION_METADATA_KEY, String.format("%s %s", BEARER_TYPE, value));
                metadataApplier.apply(headers);
            } catch (Throwable e) {
                metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }

    @Override
    public void thisUsesUnstableApi() {
        // noop
    }
    
    public static String getTokenWithoutType(String token) {
		if (token == null || token.trim().length() == 0) {
			return "";
		}
		if (token.startsWith(BEARER_TYPE)) {
			return token.substring(BEARER_TYPE.length()).trim();
		}
		return token;
	}
}
