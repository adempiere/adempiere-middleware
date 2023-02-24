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
package org.spin.grpc.service;

import java.io.IOException;

import org.spin.grpc.controller.MiddlewareServiceImplementation;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class MiddlewareServer {
	public static final int MOVIE_CONTROLLER_SERVICE_PORT = 50051;
    public static void main(String[] args) 
            throws IOException, InterruptedException {
        Server server =     
          ServerBuilder.forPort(MOVIE_CONTROLLER_SERVICE_PORT)
                .addService(new MiddlewareServiceImplementation())
                .build();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            System.out.println("Successfully stopped the server");
        }));
        server.awaitTermination();
    }
}
