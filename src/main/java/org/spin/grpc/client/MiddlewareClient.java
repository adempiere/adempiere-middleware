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
package org.spin.grpc.client;

import java.security.Key;

import org.spin.authentication.BearerToken;
import org.spin.proto.common.Entity;
import org.spin.proto.common.KeyValue;
import org.spin.proto.common.Value;
import org.spin.proto.common.ValueType;
import org.spin.proto.service.CreateEntityRequest;
import org.spin.proto.service.MiddlewareServiceGrpc;
import org.spin.proto.service.MiddlewareServiceGrpc.MiddlewareServiceBlockingStub;
import org.spin.server.setup.SetupLoader;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class MiddlewareClient {
    public static void main(String[] args) throws Exception {
    	if (args == null) {
			throw new Exception("Arguments Not Found");
		}
		//
		if (args.length == 0) {
			throw new Exception("Arguments Must Be: [property file name]");
		}
		String setupFileName = args[0];
		if(setupFileName == null || setupFileName.trim().length() == 0) {
			throw new Exception("Setup File not found");
		}
		SetupLoader.loadSetup(setupFileName);
		//	Validate load
		SetupLoader.getInstance().validateLoad();
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(SetupLoader.getInstance().getServer().getHost(),  
                		SetupLoader.getInstance().getServer().getPort())
                .usePlaintext()
                .build();
        byte[] keyBytes = Decoders.BASE64.decode("ba54a050aaf4a9cfc619a31afbb03d212b5024a9957fa8b069a8c1b742de8c878846244f9c4b6834");
        Key key = Keys.hmacShaKeyFor(keyBytes);
        BearerToken token = new BearerToken(Jwts.builder()
                .setSubject("MiddlewareClient")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact());
        MiddlewareServiceBlockingStub 
                 client = MiddlewareServiceGrpc
                 .newBlockingStub(channel)
                 .withCallCredentials(token);
        try {
        	Entity entity = client.createEntity(CreateEntityRequest.newBuilder()
        			.setTableName("M_Product_Class")
        			//	Value
        			.addAttributes(KeyValue.newBuilder()
        					.setKey("Value")
        					.setValue(Value.newBuilder()
        							.setStringValue("gRPC-01")
        							.setValueType(ValueType.STRING)
        							.build())
        					.build())
        			//	Name
        			.addAttributes(KeyValue.newBuilder()
        					.setKey("Name")
        					.setValue(Value.newBuilder()
        							.setStringValue("Test for gRPC-01")
        							.setValueType(ValueType.STRING)
        							.build())
        					.build())
        			//	Description
        			.addAttributes(KeyValue.newBuilder()
        					.setKey("Description")
        					.setValue(Value.newBuilder()
        							.setStringValue("This is a test based on gRPC-01")
        							.setValueType(ValueType.STRING)
        							.build())
        					.build())
        			.build());
            System.out.println("Entity Created " + entity.getId() + " - " + entity.getValuesMap());
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
        }
    }
}
