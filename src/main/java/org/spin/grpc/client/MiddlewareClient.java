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

import org.spin.proto.common.Entity;
import org.spin.proto.common.KeyValue;
import org.spin.proto.common.Value;
import org.spin.proto.common.ValueType;
import org.spin.proto.service.CreateEntityRequest;
import org.spin.proto.service.MiddlewareServiceGrpc;
import org.spin.proto.service.MiddlewareServiceGrpc.MiddlewareServiceBlockingStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class MiddlewareClient {
	public static final int MOVIE_CONTROLLER_SERVICE_PORT = 50051;
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost",  
                   MOVIE_CONTROLLER_SERVICE_PORT)
                .usePlaintext()
                .build();
        MiddlewareServiceBlockingStub 
                 client = MiddlewareServiceGrpc
                 .newBlockingStub(channel);
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
