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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_PInstance;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.X_AD_Table;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MReportView;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.compiere.util.MimeType;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.eevolution.services.dsl.ProcessBuilder;
import org.spin.base.workflow.WorkflowUtil;
import org.spin.proto.service.CreateEntityRequest;
import org.spin.proto.service.DeleteEntitiesBatchRequest;
import org.spin.proto.service.DeleteEntityRequest;
import org.spin.proto.service.Entity;
import org.spin.proto.service.GetEntityRequest;
import org.spin.proto.service.KeyValueSelection;
import org.spin.proto.service.ListEntitiesRequest;
import org.spin.proto.service.ListEntitiesResponse;
import org.spin.proto.service.ProcessInfoLog;
import org.spin.proto.service.ReportOutput;
import org.spin.proto.service.RunBusinessProcessRequest;
import org.spin.proto.service.RunBusinessProcessResponse;
import org.spin.proto.service.UpdateEntityRequest;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.Value;

public class Service {
	
	/** Table Allows Records with Zero Identifier */
	public static List<String> ALLOW_ZERO_ID = Arrays.asList(
		X_AD_Table.ACCESSLEVEL_All,
		X_AD_Table.ACCESSLEVEL_SystemPlusClient,
		X_AD_Table.ACCESSLEVEL_ClientPlusOrganization
	);
	
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
		MTable table = MTable.get(Env.getCtx(), request.getTableName());
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		PO entity = table.getPO(0, null);
		if(entity == null) {
			throw new AdempiereException("@Error@ PO is null");
		}
		Map<String, Value> attributes = request.getAttributes().getFieldsMap();
		attributes.keySet().forEach(key -> {
			Value attribute = attributes.get(key);
			Object value = ValueManager.getObjectFromValue(attribute);
			entity.set_ValueOfColumn(key, value);
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
		PO entity = getEntity(request.getTableName(), request.getId(), null);
		if(entity != null
				&& entity.get_ID() >= 0) {
			Map<String, Value> attributes = request.getAttributes().getFieldsMap();
			attributes.keySet().forEach(key -> {
				Value attribute = attributes.get(key);
				Object value = ValueManager.getObjectFromValue(attribute);
				entity.set_ValueOfColumn(key, value);
			});
			//	Save entity
			entity.saveEx();
		}
		//	Return
		return Converter.convertEntity(entity);
	}
	
	/**
	 * Convert a PO from query
	 * @param request
	 * @return
	 */
	public static Entity.Builder getEntity(GetEntityRequest request) {
		String tableName = request.getTableName();
		PO entity = null;
		if(request.getId() != 0) {
			entity = getEntity(tableName, request.getId(), null);
		}
//		else if(request.getFilters() != null) {
//			List<Object> parameters = new ArrayList<Object>();
//			String whereClause = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), parameters);
//			entity = RecordUtil.getEntity(Env.getCtx(), tableName, whereClause, parameters, null);
//		}
		//	Return
		return Converter.convertEntity(entity);
	}
	
	/**
	 * Convert Object to list
	 * @param request
	 * @return
	 */
	public static ListEntitiesResponse.Builder listEntities(ListEntitiesRequest request) {
//		StringBuffer whereClause = new StringBuffer();
//		List<Object> params = new ArrayList<>();
		//	For dynamic condition
//		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), params);
//		if(!Util.isEmpty(dynamicWhere)) {
//			if(whereClause.length() > 0) {
//				whereClause.append(" AND ");
//			}
//			//	Add
//			whereClause.append(dynamicWhere);
//		}
		//	TODO: Add support to this functionality with a distinct scope
		//	Add from reference
//		if(!Util.isEmpty(criteria.getReferenceUuid())) {
//			String referenceWhereClause = referenceWhereClauseCache.get(criteria.getReferenceUuid());
//			if(!Util.isEmpty(referenceWhereClause)) {
//				if(whereClause.length() > 0) {
//					whereClause.append(" AND ");
//				}
//				whereClause.append("(").append(referenceWhereClause).append(")");
//			}
//		}
		//	Get page and count
//		String nexPageToken = null;
//		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
//		int limit = LimitUtil.getPageSize(request.getPageSize());
//		int offset = (pageNumber - 1) * limit;
//		int count = 0;
//
		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();
//		Query query = new Query(context, request.getTableName(), whereClause.toString(), null)
//				.setParameters(params);
//		count = query.count();
//		if(!Util.isEmpty(request.getSortBy())) {
//			query.setOrderBy(SortingManager.newInstance(request.getSortBy()).getSotingAsSQL());
//		}
//		List<PO> entityList = query
//				.setLimit(limit, offset)
//				.<PO>list();
//		//	
//		for(PO entity : entityList) {
//			Entity.Builder valueObject = ConvertUtil.convertEntity(entity);
//			builder.addRecords(valueObject.build());
//		}
//		//	
//		builder.setRecordCount(count);
//		//	Set page token
//		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
//			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
//		}
//		//	Set netxt page
//		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		//	Return
		return builder;
	}
	
	/**
	 * Delete many entities
	 * @param context
	 * @param request
	 * @return
	 */
	public static Empty.Builder deleteEntities(DeleteEntitiesBatchRequest request) {
		Trx.run(transactionName -> {
			if(Util.isEmpty(request.getTableName())) {
				throw new AdempiereException("@AD_Table_ID@ @NotFound@");
			}
			List<Integer> ids = request.getIdsList();
			if (ids.size() > 0) {
				MTable table = MTable.get(Env.getCtx(), request.getTableName());
				ids.stream().forEach(id -> {
					PO entity = table.getPO(id, transactionName);
					if (entity != null && entity.get_ID() > 0) {
						entity.deleteEx(true);
					}
				});
			}
		});
		//	Return
		return Empty.newBuilder();
	}
	
	/**
	 * Delete a entity
	 * @param context
	 * @param request
	 * @return
	 */
	public static Empty.Builder deleteEntity(DeleteEntityRequest request) {
		if (request.getId() > 0) {
			PO entity = getEntity(request.getTableName(), request.getId(), null);
			if (entity != null && entity.get_ID() > 0) {
				entity.deleteEx(true);
			}
		}
		//	Return
		return Empty.newBuilder();
	}
	
	/**
	 * Evaluate if is valid identifier
	 * @param id
	 * @param accesLevel
	 * @return
	 */
	public static boolean isValidId(int id, String accesLevel) {
		if (id < 0) {
			return false;
		}

		if (id == 0 && !ALLOW_ZERO_ID.contains(accesLevel)) {
			return false;
		}

		return true;
	}
	
	/**
	 * Get PO entity from Table and record id
	 * @param tableName
	 * @param id
	 * @return
	 */
	public static PO getEntity(String tableName, int id, String transactionName) {
		if (Util.isEmpty(tableName, true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		//	Validate ID
		if (!isValidId(id, table.getAccessLevel())) {
			throw new AdempiereException("@FillMandatory@ @Record_ID@");
		}
		return MTable.get(Env.getCtx(), tableName).getPO(id, transactionName);
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
		if(process == null || process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@AD_Process_ID@ @NotFound@");
		}
		if(!MRole.getDefault().getProcessAccess(process.getAD_Process_ID())) {
			if (process.isReport()) {
				throw new AdempiereException("@AccessCannotReport@");
			}
			throw new AdempiereException("@AccessCannotProcess@");
		}

		int tableId = 0;
		PO entity = null;
		if(!Util.isEmpty(request.getTableName())) {
			MTable table = MTable.get(Env.getCtx(), request.getTableName());
			if(table != null && table.getAD_Table_ID() > 0) {
				tableId = table.getAD_Table_ID();
			}
			if(request.getRecordId() > 0) {
				entity = getEntity(request.getTableName(), request.getRecordId(), null);
			}
		}
		Map<String, Value> parameters = new HashMap<String, Value>();
		parameters.putAll(request.getParameters().getFieldsMap());
		//	Call process builder
		ProcessBuilder builder = ProcessBuilder.create(Env.getCtx())
				.process(process.getAD_Process_ID())
				.withRecordId(tableId, request.getId())
				.withoutPrintPreview()
				.withoutBatchMode()
				.withWindowNo(0)
				.withTitle(process.getName());
		//	Set Report Export Type
		if(process.isReport()) {
			String reportType = request.getReportType();
			if(Util.isEmpty(reportType, true)) {
				reportType = "pdf";
			}
			builder.withReportExportFormat(reportType);
		}
		//	Selection
		if(request.getSelectionsCount() > 0) {
			List<Integer> selectionKeys = new ArrayList<>();
			LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection = new LinkedHashMap<>();
			for(KeyValueSelection selectionKey : request.getSelectionsList()) {
				selectionKeys.add(selectionKey.getSelectionId());
				if(selectionKey.getValues().getFieldsCount() > 0) {
					selection.put(selectionKey.getSelectionId(), new LinkedHashMap<>(ValueManager.convertValuesMapToObjects(selectionKey.getValues().getFieldsMap())));
				}
			}
			builder.withSelectedRecordsIds(request.getTableSelectedId(), selectionKeys, selection);
		}
		//	Parameters
		String documentAction = null;
		//	Parameters
		if(request.getParameters().getFieldsCount() > 0) {
			for(Entry<String, Value> parameter : parameters.entrySet().stream().filter(parameterValue -> !parameterValue.getKey().endsWith("_To")).collect(Collectors.toList())) {
				Object value = ValueManager.getObjectFromValue(parameter.getValue());
				Optional<Entry<String, Value>> maybeToParameter = parameters.entrySet().stream().filter(parameterValue -> parameterValue.getKey().equals(parameter.getKey() + "_To")).findFirst();
				if(value != null) {
					if(maybeToParameter.isPresent()) {
						Object valueTo = ValueManager.getObjectFromValue(maybeToParameter.get().getValue());
						builder.withParameter(parameter.getKey(), value, valueTo);
					} else {
						builder.withParameter(parameter.getKey(), value);
					}
					//	For Document Action
					if(parameter.getKey().equals(I_C_Order.COLUMNNAME_DocAction)) {
						documentAction = (String) value;
					}
				}
			}
		}
		//	For Document
		if(process.getAD_Workflow_ID() > 0 && !Util.isEmpty(documentAction, true)
			&& entity != null && DocAction.class.isAssignableFrom(entity.getClass())) {
			return WorkflowUtil.startWorkflow(
				request.getTableName(),
				request.getRecordId(),
				documentAction
			);
		}
		//	Execute Process
		ProcessInfo result = null;
		try {
			result = builder.execute();
		} catch (Exception e) {
			result = builder.getProcessInfo();
			//	Set error message
			String summary = Msg.parseTranslation(Env.getCtx(), result.getSummary());
			if(Util.isEmpty(summary, true)) {
				result.setSummary(ValueManager.validateNull(e.getLocalizedMessage()));
			}
		}
		int reportViewReferenceId = 0;
		int printFormatReferenceId = request.getPrintFormatId();
		String tableName = null;
		//	Get process instance from identifier
		if(result.getAD_PInstance_ID() != 0) {
			MPInstance instance = new Query(Env.getCtx(), I_AD_PInstance.Table_Name, I_AD_PInstance.COLUMNNAME_AD_PInstance_ID + " = ?", null)
					.setParameters(result.getAD_PInstance_ID())
					.first();
			response.setId(instance.getAD_PInstance_ID());
			response.setLastRun(ValueManager.getTimestampFromDate(instance.getUpdated()));
			if(process.isReport()) {
				int printFormatId = 0;
				int reportViewId = 0;
				if(instance.getAD_PrintFormat_ID() != 0) {
					printFormatId = instance.getAD_PrintFormat_ID();
				} else if(process.getAD_PrintFormat_ID() != 0) {
					printFormatId = process.getAD_PrintFormat_ID();
				} else if(process.getAD_ReportView_ID() != 0) {
					reportViewId = process.getAD_ReportView_ID();
				}
				//	Get from report view or print format
				MPrintFormat printFormat = null;
				if(printFormatReferenceId > 0) {
					printFormat = MPrintFormat.get(Env.getCtx(), printFormatReferenceId, false);
					tableName = printFormat.getAD_Table().getTableName();
					if(printFormat.getAD_ReportView_ID() != 0) {
						reportViewReferenceId = printFormat.getAD_ReportView_ID();
					}
				} else if(printFormatId != 0) {
					printFormatReferenceId = printFormatId;
					printFormat = MPrintFormat.get(Env.getCtx(), printFormatReferenceId, false);
					tableName = printFormat.getAD_Table().getTableName();
					if(printFormat.getAD_ReportView_ID() != 0) {
						reportViewReferenceId = printFormat.getAD_ReportView_ID();
					}
				} else if(reportViewId != 0) {
					MReportView reportView = MReportView.get(Env.getCtx(), reportViewId);
					reportViewReferenceId = reportViewId;
					tableName = reportView.getAD_Table().getTableName();
					printFormat = MPrintFormat.get(Env.getCtx(), reportViewId, 0);
					if(printFormat != null) {
						printFormatReferenceId = printFormat.getAD_PrintFormat_ID();
					}
				}
			}
		}
		//	Validate print format
		if(printFormatReferenceId <= 0) {
			printFormatReferenceId = request.getPrintFormatId();
		}
		//	Validate report view
		if(reportViewReferenceId <= 0) {
			reportViewReferenceId = request.getReportViewId();
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
				ProcessInfoLog.Builder infoLogBuilder = Converter.convertProcessInfoLog(log);
				response.addLogs(infoLogBuilder.build());
			}
		}
		if(process.isReport()) {
			File reportFile = Optional.ofNullable(result.getReportAsFile()).orElse(result.getPDFReport());
			if(reportFile != null
					&& reportFile.exists()) {
				String validFileName = FileUtil.getValidFileName(reportFile.getName());
				ReportOutput.Builder output = ReportOutput.newBuilder();
				output.setFileName(ValueManager.validateNull(validFileName));
				output.setName(result.getTitle());
				output.setMimeType(ValueManager.validateNull(MimeType.getMimeType(validFileName)));
				output.setDescription(ValueManager.validateNull(process.getDescription()));
				//	Type
				String reportType = result.getReportType();
				if(Util.isEmpty(result.getReportType())) {
					reportType = result.getReportType();
				}
				if(!Util.isEmpty(FileUtil.getExtension(validFileName))
						&& !FileUtil.getExtension(validFileName).equals(reportType)) {
					reportType = FileUtil.getExtension(validFileName);
				}
				output.setReportType(request.getReportType());

				ByteString resultFile = ByteString.empty();
				try {
					resultFile = ByteString.readFrom(new FileInputStream(reportFile));
				} catch (IOException e) {
					e.printStackTrace();
					// log.severe(e.getLocalizedMessage());

					if (Util.isEmpty(response.getSummary(), true)) {
						response.setSummary(
							ValueManager.validateNull(
								e.getLocalizedMessage()
							)
						);
					}
				}
				if(reportType.endsWith("html") || reportType.endsWith("txt")) {
					output.setOutputBytes(resultFile);
				}
				output.setReportType(reportType);
				output.setOutputStream(resultFile);
				output.setReportViewId(reportViewReferenceId);
				output.setPrintFormatId(printFormatReferenceId);
				output.setTableName(ValueManager.validateNull(tableName));
				response.setOutput(output.build());
			}
		}
		return response;
	}
}
