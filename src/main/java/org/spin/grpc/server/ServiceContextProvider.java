/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
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
package org.spin.grpc.server;

import org.compiere.util.ContextProvider;
import org.spin.authentication.AuthorizationServerInterceptor;

import java.util.Properties;



/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * This class allows define a context provider based on gRPC Context
 */
public class ServiceContextProvider implements ContextProvider {

	/**
	 * Get server context proxy
	 */
	public Properties getContext() {
		Properties context = (Properties) AuthorizationServerInterceptor.SESSION_CONTEXT.get();
		if(context == null) {
			context = new Properties();
		}
		return context;
	}

	/**
	 * Backend not implemented here
	 */
	public void showURL(String url, String title) {
		
	} 
}
