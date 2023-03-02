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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_PInstance;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.eevolution.services.dsl.ProcessBuilder;
import org.spin.proto.service.Empty;
import org.spin.proto.service.Entity;
import org.spin.proto.service.KeyValue;
import org.spin.proto.service.KeyValueSelection;
import org.spin.proto.service.ProcessLog;
import org.spin.proto.service.Value;
import org.spin.proto.service.ValueType;
import org.spin.proto.service.CreateEntityRequest;
import org.spin.proto.service.DeleteEntityRequest;
import org.spin.proto.service.RunBusinessProcessRequest;
import org.spin.proto.service.RunBusinessProcessResponse;
import org.spin.proto.service.UpdateEntityRequest;

public class Service {
	/**
	 * Create Entity
	 * @param context
	 * @param request
	 * @return
	 */
	public static Entity.Builder createEntity(CreateEntityRequest request) {
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		PO entity = getEntity(request.getTableName(), 0);
		if(entity == null) {
			throw new AdempiereException("@Error@ PO is null");
		}
		request.getAttributesList().forEach(attribute -> {
			Object value = ValueManager.getObjectFromValue(attribute.getValue());;
			entity.set_ValueOfColumn(attribute.getKey(), value);
		});
		//	Save entity
		entity.saveEx();
		//	Return
		return Converter.convertEntity(entity);
	}
	
	
	/**
	 * Update Entity
	 * @param context
	 * @param request
	 * @return
	 */
	public static Entity.Builder updateEntity(UpdateEntityRequest request) {
		PO entity = getEntity(request.getTableName(), request.getId());
		if(entity != null
				&& entity.get_ID() >= 0) {
			request.getAttributesList().forEach(attribute -> {
				Object value = ValueManager.getObjectFromValue(attribute.getValue());
				entity.set_ValueOfColumn(attribute.getKey(), value);
			});
			//	Save entity
			entity.saveEx();
		}
		//	Return
		return Converter.convertEntity(entity);
	}
	
	/**
	 * Delete a entity
	 * @param context
	 * @param request
	 * @return
	 */
	public static Empty.Builder deleteEntity(DeleteEntityRequest request) {
		if (request.getId() > 0) {
			PO entity = getEntity(request.getTableName(), request.getId());
			if (entity != null && entity.get_ID() > 0) {
				entity.deleteEx(true);
			}
		}
		//	Return
		return Empty.newBuilder();
	}
	
	/**
	 * Get PO entity from Table and record id
	 * @param tableName
	 * @param id
	 * @return
	 */
	private static PO getEntity(String tableName, int id) {
		if(Util.isEmpty(tableName)) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		if(id < 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		return MTable.get(Env.getCtx(), tableName).getPO(id, null);
	}
	
	
	/**
	 * Run a process from request
	 * @param context
	 * @param request
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static RunBusinessProcessResponse.Builder runProcess(RunBusinessProcessRequest request) {
		//	Get Process definition
		MProcess process = new Query(Env.getCtx(), I_AD_Process.Table_Name, I_AD_Process.COLUMNNAME_Value + " = ?", null).setParameters(request.getProcessCode()).setOnlyActiveRecords(true).first();
		if(process == null
				|| process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@AD_Process_ID@ @NotFound@");
		}

		RunBusinessProcessResponse.Builder response = RunBusinessProcessResponse.newBuilder();
		;
		//	Set Report Export Type
		if(process.isReport()) {
			return response.setIsError(true).setSummary("Report now allowed for run from this service");	
		}

		int tableId = 0;
		if(!Util.isEmpty(request.getTableName())) {
			MTable table = MTable.get(Env.getCtx(), request.getTableName());
			if(table != null && table.getAD_Table_ID() > 0) {
				tableId = table.getAD_Table_ID();
			}
		}
		List<KeyValue> parametersList = new ArrayList<KeyValue>();
		parametersList.addAll(request.getParametersList());
		if(request.getId() != 0
				&& !Util.isEmpty(request.getTableName())) {
			// Add record as parameter
			Value.Builder value = Value.newBuilder()
				.setValueType(ValueType.INTEGER)
				.setIntValue(request.getId());
			KeyValue.Builder recordParameter = KeyValue.newBuilder()
				.setKey(request.getTableName() + "_ID")
				.setValue(value);
			// set as first position
			parametersList.add(0, recordParameter.build());
		}

		//	Call process builder
		ProcessBuilder builder = ProcessBuilder.create(Env.getCtx())
				.process(process.getAD_Process_ID())
				.withRecordId(tableId, request.getId())
				.withoutPrintPreview()
				.withoutBatchMode()
				.withWindowNo(0)
				.withTitle(process.getName());
		//	Selection
		if(request.getSelectionsCount() > 0) {
			List<Integer> selectionKeys = new ArrayList<>();
			LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection = new LinkedHashMap<>();
			for(KeyValueSelection selectionKey : request.getSelectionsList()) {
				selectionKeys.add(selectionKey.getSelectionId());
				if(selectionKey.getValuesCount() > 0) {
					selection.put(selectionKey.getSelectionId(), new LinkedHashMap<>(ValueManager.convertValuesToObjects(selectionKey.getValuesList())));
				}
			}
			builder.withSelectedRecordsIds(request.getTableSelectedId(), selectionKeys, selection);
		}
		//	Parameters
		if(request.getParametersCount() > 0) {
			parametersList.stream().filter(parameterValue -> !parameterValue.getKey().endsWith("_To")).collect(Collectors.toList()).forEach(parameter -> {
				Object value = ValueManager.getObjectFromValue(parameter.getValue());
				Optional<KeyValue> maybeToParameter = parametersList.stream().filter(parameterValue -> parameterValue.getKey().equals(parameter.getKey() + "_To")).findFirst();
				if(value != null) {
					if(maybeToParameter.isPresent()) {
						Object valueTo = ValueManager.getObjectFromValue(maybeToParameter.get().getValue());
						builder.withParameter(parameter.getKey(), value, valueTo);
					} else {
						builder.withParameter(parameter.getKey(), value);
					}
				}
			});
		}
		//	Execute Process
		ProcessInfo result = null;
		try {
			result = builder.execute();
		} catch (Exception e) {
			result = builder.getProcessInfo();
			//	Set error message
			if(Util.isEmpty(result.getSummary())) {
				result.setSummary(e.getLocalizedMessage());
			}
		}
		//	Get process instance from identifier
		if(result.getAD_PInstance_ID() != 0) {
			MPInstance instance = new Query(Env.getCtx(), I_AD_PInstance.Table_Name, I_AD_PInstance.COLUMNNAME_AD_PInstance_ID + " = ?", null)
					.setParameters(result.getAD_PInstance_ID())
					.first();
			response.setId(instance.getAD_PInstance_ID());
			response.setLastRun(ValueManager.convertDateToString(instance.getUpdated()));
		}
		//	
		response.setIsError(result.isError());
		if(!Util.isEmpty(result.getSummary())) {
			response.setSummary(Msg.parseTranslation(Env.getCtx(), result.getSummary()));
		}
		//	
		response.setResultTableName(ValueManager.validateNull(result.getResultTableName()));
		//	Convert Log
		if(result.getLogList() != null) {
			for(org.compiere.process.ProcessInfoLog log : result.getLogList()) {
				ProcessLog.Builder infoLogBuilder = Converter.convertProcessInfoLog(log);
				response.addLogs(infoLogBuilder.build());
			}
		}
		return response;
	}
}
