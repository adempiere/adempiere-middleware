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
package org.spin.grpc.controller;

import org.compiere.util.CLogger;
import org.spin.grpc.service.Service;
import org.spin.proto.service.CreateEntityRequest;
import org.spin.proto.service.DeleteEntitiesBatchRequest;
import org.spin.proto.service.DeleteEntityRequest;
import org.spin.proto.service.Entity;
import org.spin.proto.service.GetEntityRequest;
import org.spin.proto.service.ListEntitiesRequest;
import org.spin.proto.service.ListEntitiesResponse;
import org.spin.proto.service.MiddlewareServiceGrpc.MiddlewareServiceImplBase;
import org.spin.proto.service.RunBusinessProcessRequest;
import org.spin.proto.service.RunBusinessProcessResponse;
import org.spin.proto.service.UpdateEntityRequest;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class Middleware extends MiddlewareServiceImplBase {
	
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(Middleware.class);
	
	@Override
	public void getEntity(GetEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			Entity.Builder entityValue = Service.getEntity(request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
    public void createEntity(CreateEntityRequest request, StreamObserver<Entity> responseObserver) {
    	try {
    		log.fine("createEntity: " + request);
    		responseObserver.onNext(Service.createEntity(request).build());
    		responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
    }
    
    @Override
    public void updateEntity(UpdateEntityRequest request, StreamObserver<Entity> responseObserver) {
    	try {
    		log.fine("updateEntity: " + request);
    		responseObserver.onNext(Service.updateEntity(request).build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
    }
    
    @Override
    public void deleteEntity(DeleteEntityRequest request, StreamObserver<Empty> responseObserver) {
    	try {
    		log.fine("deleteEntity: " + request);
    		responseObserver.onNext(Service.deleteEntity(request).build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
    }
    
    @Override
	public void deleteEntitiesBatch(DeleteEntitiesBatchRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder entityValue = Service.deleteEntities(request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listEntities(ListEntitiesRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			ListEntitiesResponse.Builder entityValueList = Service.listEntities(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
    
    @Override
    public void runBusinessProcess(RunBusinessProcessRequest request, StreamObserver<RunBusinessProcessResponse> responseObserver) {
    	try {
    		log.fine("runBusinessProcess: " + request);
    		responseObserver.onNext(Service.runProcess(request).build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
    }
}