/************************************************************************************
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
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
package org.spin.base.workflow;

import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;
import org.spin.grpc.service.Service;
import org.spin.grpc.service.ValueManager;
import org.spin.proto.service.RunBusinessProcessResponse;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class WorkflowUtil {
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(WorkflowUtil.class);



	/**
	 * 	Check Status Change
	 *	@param tableName table name
	 *	@param recordId record
	 *	@param documentStatus current doc status
	 *	@return true if status not changed
	 */
	public static boolean checkStatus(String tableName, int recordId, String documentStatus) {
		final String sql = "SELECT 2 FROM " + tableName 
			+ " WHERE " + tableName + "_ID=" + recordId
			+ " AND DocStatus='" + documentStatus + "'"
		;
		int result = DB.getSQLValue(null, sql);
		return result == 2;
	}



	public static RunBusinessProcessResponse.Builder startWorkflow(String tableName, int recordId, String documentAction) {
		Properties context = Env.getCtx();
		if (Util.isEmpty(tableName, true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		MTable table = MTable.get(context, tableName);
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		RunBusinessProcessResponse.Builder response = RunBusinessProcessResponse.newBuilder()
			.setResultTableName(
				ValueManager.validateNull(tableName)
			)
		;
		try {
			if (!table.isDocument()) {
				throw new AdempiereException("@NotValid@ @AD_Table_ID@ @IsDocument@@");
			}

			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}

			PO entity = Service.getEntity(table.getTableName(), recordId, null);
			if (entity == null || entity.get_ID() <= 0) {
				throw new AdempiereException("@Error@ @PO@ @NotFound@");
			}
			//	Validate as document
			if (!DocAction.class.isAssignableFrom(entity.getClass())) {
				throw new AdempiereException("@Invalid@ @Document@");
			}

			entity.set_ValueOfColumn(I_C_Order.COLUMNNAME_DocAction, documentAction);
			entity.saveEx();
			//	Process It
			//	Get WF from Table
			MColumn column = table.getColumn(I_C_Order.COLUMNNAME_DocAction);
			if(column != null) {
				MProcess process = MProcess.get(context, column.getAD_Process_ID());
				if(process.getAD_Workflow_ID() > 0) {
					MWorkflow workFlow = MWorkflow.get (context, process.getAD_Workflow_ID());
					String name = process.get_Translation(I_AD_Process.COLUMNNAME_Name);
					ProcessInfo processInfo = new ProcessInfo(name, process.getAD_Process_ID(), table.getAD_Table_ID(), entity.get_ID());
					processInfo.setAD_User_ID (Env.getAD_User_ID(context));
					processInfo.setAD_Client_ID(Env.getAD_Client_ID(context));
					processInfo.setInterfaceType(ProcessInfo.INTERFACE_TYPE_NOT_SET);
					if(processInfo.getAD_PInstance_ID() <= 0) {
						MPInstance instance = null;
						//	Set to null for reload
						//	BR [ 380 ]
						processInfo.setParameter(null);
						try {
							instance = new MPInstance(
								context,
								processInfo.getAD_Process_ID(),
								processInfo.getRecord_ID()
							);
							instance.setName(name);
							instance.saveEx();
							//	Set Instance
							processInfo.setAD_PInstance_ID(instance.getAD_PInstance_ID());
						} catch (Exception e) { 
							processInfo.setSummary (e.getLocalizedMessage()); 
							processInfo.setError (true); 
							log.warning(processInfo.toString()); 
							processInfo.getSummary();
							throw new AdempiereException(e);
						}
					}
					if (processInfo.isBatch()) {
						workFlow.start(processInfo);		//	may return null
					} else {
						workFlow.startWait(processInfo);	//	may return null
					}
					String summary = processInfo.getSummary();
					response.setSummary(
						ValueManager.validateNull(summary)
					);
					response.setIsError(processInfo.isError());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.severe(e.getLocalizedMessage());
			String summary = e.getLocalizedMessage();
			if (Util.isEmpty(summary, true)) {
				summary = e.getLocalizedMessage();
			}
			response.setSummary(
				ValueManager.validateNull(summary)
			);
			response.setIsError(true);
		}

		return response;
	}

}
