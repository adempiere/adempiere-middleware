/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.grpc.server;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MCountry;
import org.compiere.model.MLanguage;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.grpc.service.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Class for handle Context
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ContextManager {
	
	/**	Language */
	private static CCache<String, String> languageCache = new CCache<String, String>("Language-gRPC-Service", 30, 0);	//	no time-out

	public static Properties setContextWithAttributesFromObjectMap(int windowNo, Properties context, Map<String, Object> attributes) {
		return setContextWithAttributes(windowNo, context, attributes, true);
	}
	
	public static Properties setContextWithAttributesFromStruct(int windowNo, Properties context, Struct attributes) {
		return setContextWithAttributesFromValuesMap(windowNo, context, attributes.getFieldsMap());
	}
	
	public static Properties setContextWithAttributesFromValuesMap(int windowNo, Properties context, Map<String, Value> attributes) {
		return setContextWithAttributes(windowNo, context, ValueManager.convertValuesMapToObjects(attributes), true);
	}

	/**
	 * Set context with attributes
	 * @param windowNo
	 * @param context
	 * @param attributes
	 * @return {Properties} context with new values
	 */
	public static Properties setContextWithAttributes(int windowNo, Properties context, Map<String, Object> attributes, boolean isClearWindow) {
		if (isClearWindow) {
			Env.clearWinContext(windowNo);
		}
		if (attributes == null || attributes.size() <= 0) {
			return context;
		}

		//	Fill context
		attributes.entrySet().forEach(attribute -> {
			setWindowContextByObject(context, windowNo, attribute.getKey(), attribute.getValue());
		});

		return context;
	}


	/**
	 * Set context on window by object value
	 * @param windowNo
	 * @param context
	 * @param windowNo
	 * @param key
	 * @param value
	 * @return {Properties} context with new values
	 */
	public static void setWindowContextByObject(Properties context, int windowNo, String key, Object value) {
		if (value instanceof Integer) {
			Env.setContext(context, windowNo, key, (Integer) value);
		} else if (value instanceof BigDecimal) {
			String currentValue = null;
			if (value != null) {
				currentValue = value.toString();
			}
			Env.setContext(context, windowNo, key, currentValue);
		} else if (value instanceof Timestamp) {
			Env.setContext(context, windowNo, key, (Timestamp) value);
		} else if (value instanceof Boolean) {
			Env.setContext(context, windowNo, key, (Boolean) value);
		} else if (value instanceof String) {
			Env.setContext(context, windowNo, key, (String) value);
		}
	}

	/**
	 * Set context on tab by object value
	 * @param windowNo
	 * @param context
	 * @param windowNo
	 * @param tabNo
	 * @param key
	 * @param value
	 * @return {Properties} context with new values
	 */
	public static void setTabContextByObject(Properties context, int windowNo, int tabNo, String key, Object value) {
		if (value instanceof Integer) {
			Integer currentValue = (Integer) value;
			Env.setContext(context, windowNo, tabNo, key, currentValue.toString());
		}else if (value instanceof BigDecimal) {
			String currentValue = null;
			if (value != null) {
				currentValue = value.toString();
			}
			Env.setContext(context, windowNo, tabNo, key, currentValue);
		} else if (value instanceof Timestamp) {
			Timestamp currentValue = (Timestamp) value;
			Env.setContext(context, windowNo, tabNo, key, currentValue.toString());
		} else if (value instanceof Boolean) {
			Env.setContext(context, windowNo, tabNo, key, (Boolean) value);
		} else if (value instanceof String) {
			Env.setContext(context, windowNo, tabNo, key, (String) value);
		}
	}


	/**
	 * Get Default Country
	 * @return
	 */
	public static MCountry getDefaultCountry() {
		MClient client = MClient.get (Env.getCtx());
		MLanguage language = MLanguage.get(Env.getCtx(), client.getAD_Language());
		MCountry country = MCountry.get(Env.getCtx(), language.getCountryCode());
		//	Verify
		if(country != null) {
			return country;
		}
		//	Default
		return MCountry.getDefault(Env.getCtx());
	}
	
	/**
	 * Get Default from language
	 * @param language
	 * @return
	 */
	public static String getDefaultLanguage(String language) {
		MClient client = MClient.get(Env.getCtx());
		String clientLanguage = client.getAD_Language();
		if(!Util.isEmpty(clientLanguage)
				&& Util.isEmpty(language)) {
			return clientLanguage;
		}
		String defaultLanguage = language;
		if(Util.isEmpty(language)) {
			language = Language.AD_Language_en_US;
		}
		//	Using es / en instead es_VE / en_US
		//	get default
		if(language.length() == 2) {
			defaultLanguage = languageCache.get(language);
			if(!Util.isEmpty(defaultLanguage)) {
				return defaultLanguage;
			}
			defaultLanguage = DB.getSQLValueString(null, "SELECT AD_Language "
					+ "FROM AD_Language "
					+ "WHERE LanguageISO = ? "
					+ "AND (IsSystemLanguage = 'Y' OR IsBaseLanguage = 'Y')", language);
			//	Set language
			languageCache.put(language, defaultLanguage);
		}
		if(Util.isEmpty(defaultLanguage)) {
			defaultLanguage = Language.AD_Language_en_US;
		}
		//	Default return
		return defaultLanguage;
	}
}
