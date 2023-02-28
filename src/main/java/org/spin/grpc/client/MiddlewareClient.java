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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.compiere.util.CLogger;
import org.spin.authentication.BearerToken;
import org.spin.proto.common.Entity;
import org.spin.proto.common.KeyValue;
import org.spin.proto.common.Value;
import org.spin.proto.common.ValueType;
import org.spin.proto.service.CreateEntityRequest;
import org.spin.proto.service.DeleteEntityRequest;
import org.spin.proto.service.MiddlewareServiceGrpc;
import org.spin.proto.service.MiddlewareServiceGrpc.MiddlewareServiceBlockingStub;
import org.spin.proto.service.UpdateEntityRequest;
import org.spin.server.setup.SetupLoader;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class MiddlewareClient {
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(MiddlewareClient.class);
	
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
        byte[] keyBytes = Decoders.BASE64.decode(SetupLoader.getInstance().getServer().getAdempiere_token());
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
        	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        	long start = System.currentTimeMillis();
        	List<Integer> ids = new ArrayList<>();
        	log.warning("Started at: " + format.format(new Date(start)));
        	IntStream.range(0, 1500).forEach(index -> {
        		String uuid = UUID.randomUUID().toString();
        		Entity entity = client.createEntity(CreateEntityRequest.newBuilder()
            			.setTableName("M_Product_Class")
            			//	Value
            			.addAttributes(KeyValue.newBuilder()
            					.setKey("Value")
            					.setValue(Value.newBuilder()
            							.setStringValue(uuid)
            							.setValueType(ValueType.STRING)
            							.build())
            					.build())
            			//	Name
            			.addAttributes(KeyValue.newBuilder()
            					.setKey("Name")
            					.setValue(Value.newBuilder()
            							.setStringValue("Test for gRPC " + index)
            							.setValueType(ValueType.STRING)
            							.build())
            					.build())
            			//	Description
            			.addAttributes(KeyValue.newBuilder()
            					.setKey("Description")
            					.setValue(Value.newBuilder()
            							.setStringValue("This is a test based on gRPC " + index)
            							.setValueType(ValueType.STRING)
            							.build())
            					.build())
            			.addAttributes(KeyValue.newBuilder()
            					.setKey("IsDefault")
            					.setValue(Value.newBuilder()
            							.setBooleanValue(false)
            							.setValueType(ValueType.BOOLEAN)
            							.build())
            					.build())
            			.build());
        		ids.add(entity.getId());
                System.out.println("Entity Created " + entity.getId());
        	});
        	ids.forEach(id -> {
        		Entity entity = client.updateEntity(UpdateEntityRequest.newBuilder()
            			.setTableName("M_Product_Class")
            			.setId(id)
            			//	Value
            			.addAttributes(KeyValue.newBuilder()
            					.setKey("Value")
            					.setValue(Value.newBuilder()
            							.setStringValue("" + id)
            							.setValueType(ValueType.STRING)
            							.build())
            					.build())
            			//	Name
            			.addAttributes(KeyValue.newBuilder()
            					.setKey("Name")
            					.setValue(Value.newBuilder()
            							.setStringValue("Test for gRPC " + id)
            							.setValueType(ValueType.STRING)
            							.build())
            					.build())
            			//	Description
            			.addAttributes(KeyValue.newBuilder()
            					.setKey("Description")
            					.setValue(Value.newBuilder()
            							.setStringValue("This is a test based on gRPC " + id)
            							.setValueType(ValueType.STRING)
            							.build())
            					.build())
            			.addAttributes(KeyValue.newBuilder()
            					.setKey("IsDefault")
            					.setValue(Value.newBuilder()
            							.setBooleanValue(false)
            							.setValueType(ValueType.BOOLEAN)
            							.build())
            					.build())
            			.build());
        		ids.add(entity.getId());
                System.out.println("Update Created " + entity.getId());
        	});
        	ids.forEach(id -> {
        		client.deleteEntity(DeleteEntityRequest.newBuilder()
            			.setTableName("M_Product_Class")
            			.setId(id)
            			.build());
        		System.out.println("Entity Deleted " + id);
        	});
        	long end = System.currentTimeMillis();
        	log.warning("Started at: " + format.format(new Date(start)));
        	log.warning("Finish at: " + format.format(new Date(end)));
            System.out.println("Entities Created");
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
        }
    }
}
