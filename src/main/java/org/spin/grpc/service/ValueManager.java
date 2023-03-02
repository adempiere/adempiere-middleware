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
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.grpc.service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.model.PO;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.compiere.util.NamePair;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.spin.proto.service.Decimal;
import org.spin.proto.service.KeyValue;
import org.spin.proto.service.Value;
import org.spin.proto.service.ValueType;

/**
 * Class for handle Values from and to client
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ValueManager {
	
	/**	Date format	*/
	private static final String TIME_FORMAT = "yyyy-MM-dd hh:mm:ss";
	private static final String DATE_FORMAT = "yyyy-MM-dd";
	
	/**
	 * Get Value 
	 * @param value
	 * @return
	 */
	public static Value.Builder getValueFromObject(Object value) {
		Value.Builder builderValue = Value.newBuilder();
		if(value == null) {
			return builderValue;
		}
		//	Validate value
		if(value instanceof BigDecimal) {
			return getValueFromDecimal((BigDecimal) value);
		} else if (value instanceof Integer) {
			return getValueFromInteger((Integer)value);
		} else if (value instanceof String) {
			return getValueFromString((String) value);
		} else if (value instanceof Boolean) {
			return getValueFromBoolean((Boolean) value);
		} else if(value instanceof Timestamp) {
			return getValueFromDate((Timestamp) value);
		}
		//	
		return builderValue;
	}
	
	/**
	 * Get value from Integer
	 * @param value
	 * @return
	 */
	public static Value.Builder getValueFromInteger(Integer value) {
		Value.Builder convertedValue = Value.newBuilder().setValueType(ValueType.INTEGER);
		if(value != null) {
			convertedValue.setIntValue((Integer)value);
		}
		//	default
		return convertedValue;
	}
	/**
	 * Get value from Int
	 * @param value
	 * @return
	 */
	public static Value.Builder getValueFromInt(int value) {
		Value.Builder convertedValue = Value.newBuilder().setValueType(ValueType.INTEGER);
		convertedValue.setIntValue(Integer.valueOf(value));
		// default
		return convertedValue;
	}
	
	/**
	 * Get value from a string
	 * @param value
	 * @return
	 */
	public static Value.Builder getValueFromString(String value) {
		return Value.newBuilder().setStringValue(validateNull(value)).setValueType(ValueType.STRING);
	}

	/**
	 * Get value from a boolean value
	 * @param value
	 * @return
	 */
	public static Value.Builder getValueFromBoolean(boolean value) {
		return Value.newBuilder().setBooleanValue(value).setValueType(ValueType.BOOLEAN);
	}
	/**
	 * Get value from a Boolean value
	 * @param value
	 * @return
	 */
	public static Value.Builder getValueFromBoolean(Boolean value) {
		return Value.newBuilder().setBooleanValue(value.booleanValue()).setValueType(ValueType.BOOLEAN);
	}
	/**
	 * Get value from a String Boolean value
	 * @param value
	 * @return
	 */
	public static Value.Builder getValueFromBoolean(String value) {
		return Value.newBuilder()
			.setBooleanValue(stringToBoolean(value))
			.setValueType(ValueType.BOOLEAN);
	}

	/**
	 * Get value from a date
	 * @param value
	 * @return
	 */
	public static Value.Builder getValueFromDate(Timestamp value) {
		if (value == null) {
			return Value.newBuilder().setValueType(ValueType.DATE);
		}
		return Value.newBuilder()
			.setLongValue(
				getLongFromTimestamp(value)
			)
			.setValueType(ValueType.DATE);
	}
	
	/**
	 * Get value from big decimal
	 * @param value
	 * @return
	 */
	public static Value.Builder getValueFromDecimal(BigDecimal value) {
		return Value.newBuilder()
			.setValueType(ValueType.DECIMAL)
			.setDecimalValue(
				getDecimalFromBigDecimal(value)
			);
	}
	
	/**
	 * Get decimal from big decimal
	 * @param value
	 * @return
	 */
	public static Decimal.Builder getDecimalFromBigDecimal(BigDecimal value) {
		if (value == null) {
			return Decimal.newBuilder();
		}
		return Decimal.newBuilder()
			.setDecimalValue(value.toPlainString())
			.setScale(value.scale());
	}
	
	/**
	 * Get Decimal from Value
	 * @param value
	 * @return
	 */
	public static BigDecimal getDecimalFromValue(Value value) {
		if (Util.isEmpty(value.getDecimalValue().getDecimalValue(), true)) {
			if (value.getValueType() == ValueType.INTEGER) {
				return BigDecimal.valueOf(value.getIntValue());
			}
			if (value.getValueType() == ValueType.STRING) {
				return getBigDecimalFromString(value.getStringValue());
			}
			return null;
		}
		// Value.Decimal.DecimalValue
		return getBigDecimalFromDecimal(value.getDecimalValue());
	}
	
	/**
	 * Get BigDecimal object from decimal
	 * @param decimalValue
	 * @return
	 */
	public static BigDecimal getBigDecimalFromDecimal(Decimal decimalValue) {
		if (decimalValue == null || Util.isEmpty(decimalValue.getDecimalValue(), true)) {
			return null;
		}
		return new BigDecimal(decimalValue.getDecimalValue())
			.setScale(decimalValue.getScale());
	}
	
	/**
	 * Get Date from a value
	 * @param value
	 * @return
	 */
	public static Timestamp getDateFromValue(Value value) {
		if(value.getLongValue() > 0) {
			return new Timestamp(value.getLongValue());
		}
		return null;
	}
	
	/**
	 * Get String from a value
	 * @param value
	 * @param uppercase
	 * @return
	 */
	public static String getStringFromValue(Value value, boolean uppercase) {
		String stringValue = value.getStringValue();
		if(Util.isEmpty(stringValue, true)) {
			return null;
		}
		//	To Upper case
		if(uppercase) {
			stringValue = stringValue.toUpperCase();
		}
		return stringValue;
	}
	
	/**
	 * Get String from a value
	 * @param value
	 * @return
	 */
	public static String getStringFromValue(Value value) {
		return getStringFromValue(value, false);
	}
	
	/**
	 * Get integer from a value
	 * @param value
	 * @return
	 */
	public static int getIntegerFromValue(Value value) {
		return value.getIntValue();
	}
	
	/**
	 * Get Boolean from a value
	 * @param value
	 * @return
	 */
	public static boolean getBooleanFromValue(Value value) {
		if (!Util.isEmpty(value.getStringValue(), true)) {
			return "Y".equals(value.getStringValue())
				|| "Yes".equals(value.getStringValue())
				|| "true".equals(value.getStringValue());
		}

		return value.getBooleanValue();
	}
	
	/**
	 * Get Value from reference
	 * @param value
	 * @param referenceId reference of value
	 * @return
	 */
	public static Value.Builder getValueFromReference(Object value, int referenceId) {
		Value.Builder builderValue = Value.newBuilder();
		if(value == null) {
			return builderValue;
		}
		//	Validate values
		if(isLookup(referenceId)
				|| DisplayType.isID(referenceId)) {
			return getValueFromObject(value);
		} else if(DisplayType.Integer == referenceId) {
			if(value instanceof Integer) {
				return getValueFromInteger((Integer) value);
			} else if(value instanceof BigDecimal) {
				return getValueFromInteger(((BigDecimal) value).intValue());
			} else {
				return getValueFromInteger(null);
			}
		} else if(DisplayType.isNumeric(referenceId)) {
			return getValueFromDecimal((BigDecimal) value);
		} else if(DisplayType.YesNo == referenceId) {
			if (value instanceof String) {
				return getValueFromBoolean((String) value);
			}
			return getValueFromBoolean((Boolean) value);
		} else if(DisplayType.isDate(referenceId)) {
			return getValueFromDate((Timestamp) value);
		} else if(DisplayType.isText(referenceId)) {
			return getValueFromString((String) value);
		} else if (DisplayType.Button == referenceId) {
			if (value instanceof Integer) {
				return getValueFromInteger((Integer) value);
			} else if(value instanceof BigDecimal) {
				return getValueFromInteger(((BigDecimal) value).intValue());
			} else if (value instanceof String) {
				return getValueFromString((String) value);
			}
			return getValueFromObject(value); 
		}
		//
		return builderValue;
	}

	public static String getDisplayedValueFromReference(Object value, String columnName, int displayTypeId, int referenceValueId) {
		String displayedValue = null;

		if (value == null) {
			return displayedValue;
		}
		if (DisplayType.isText (displayTypeId)) {
			;
		} else if (displayTypeId == DisplayType.YesNo) {
			displayedValue = booleanToString(value.toString(), true);
		} else if (displayTypeId == DisplayType.Amount) {
			DecimalFormat amountFormat = DisplayType.getNumberFormat(DisplayType.Amount, Env.getLanguage(Env.getCtx()));
			displayedValue = amountFormat.format (new BigDecimal(value.toString()));
		} else if (displayTypeId == DisplayType.Integer) {
			DecimalFormat intFormat = DisplayType.getNumberFormat(DisplayType.Integer, Env.getLanguage(Env.getCtx()));
			displayedValue = intFormat.format(Integer.valueOf(value.toString()));
		} else if (DisplayType.isNumeric(displayTypeId)) {
			DecimalFormat numberFormat = DisplayType.getNumberFormat(DisplayType.Number, Env.getLanguage(Env.getCtx()));
			if (I_C_Order.COLUMNNAME_ProcessedOn.equals(columnName)) {
				if (value.toString().indexOf(".") > 0) {
					value = value.toString().substring(0, value.toString().indexOf("."));
				}
				displayedValue = TimeUtil.formatElapsed(System.currentTimeMillis() - new BigDecimal(value.toString()).longValue());
			} else {
				displayedValue = numberFormat.format(new BigDecimal(value.toString()));
			}
		} else if (displayTypeId == DisplayType.Date) {
			SimpleDateFormat dateFormat = DisplayType.getDateFormat(DisplayType.DateTime, Env.getLanguage(Env.getCtx()));
			displayedValue = dateFormat.format(Timestamp.valueOf(value.toString()));
		} else if (displayTypeId == DisplayType.DateTime) {
			SimpleDateFormat dateTimeFormat = DisplayType.getDateFormat(DisplayType.DateTime, Env.getLanguage(Env.getCtx()));
			displayedValue = dateTimeFormat.format (Timestamp.valueOf(value.toString()));
		} else if (DisplayType.isLookup(displayTypeId) && displayTypeId != DisplayType.Button && displayTypeId != DisplayType.List) {
			Language language = Env.getLanguage(Env.getCtx());
			MLookupInfo lookupInfo = MLookupFactory.getLookupInfo(Env.getCtx(), 0, 0, displayTypeId, language, columnName, referenceValueId, false, null, false);
			MLookup lookup = new MLookup(lookupInfo, 0);
			NamePair pp = lookup.get(value);
			if (pp != null) {
				displayedValue = pp.getName();
			}
		} else if((DisplayType.Button == displayTypeId || DisplayType.List == displayTypeId) && referenceValueId != 0) {
			MLookupInfo lookupInfo = MLookupFactory.getLookup_List(Env.getLanguage(Env.getCtx()), referenceValueId);

			MLookup lookup = new MLookup(lookupInfo, 0);
			if (value != null) {
				Object key = value; 
				NamePair pp = lookup.get(key);
				if (pp != null) {
					displayedValue = pp.getName();
				}
			}
		} else if (DisplayType.isLOB(displayTypeId)) {
			;
		}
		return displayedValue;
	}


	/**
	 * Convert Selection values from gRPC to ADempiere values
	 * @param values
	 * @return
	 */
	public static Map<String, Object> convertValuesToObjects(List<KeyValue> values) {
		Map<String, Object> convertedValues = new HashMap<>();
		if (values == null || values.size() <= 0) {
			return convertedValues;
		}
		for(KeyValue value : values) {
			convertedValues.put(value.getKey(), getObjectFromValue(value.getValue()));
		}
		//	
		return convertedValues;
	}
	
	/**
	 * Default get value from type
	 * @param valueToConvert
	 * @return
	 */
	public static Object getObjectFromValue(Value valueToConvert) {
		return getObjectFromValue(valueToConvert, false);
	}
	
	/**
	 * Get value from parameter type
	 * @param value
	 * @return
	 */
	public static Object getObjectFromValue(Value value, boolean uppercase) {
		if(value.getValueType().equals(ValueType.BOOLEAN)) {
			return value.getBooleanValue();
		} else if(value.getValueType().equals(ValueType.DECIMAL)) {
			return getDecimalFromValue(value);
		} else if(value.getValueType().equals(ValueType.INTEGER)) {
			return value.getIntValue();
		} else if(value.getValueType().equals(ValueType.STRING)) {
			return getStringFromValue(value, uppercase);
		} else if(value.getValueType().equals(ValueType.DATE)) {
			return getDateFromValue(value);
		}
		return null;
	}
	
	/**
	 * Get Object from value based on reference
	 * @param value
	 * @param referenceId
	 * @return
	 */
	public static Object getObjectFromReference(Value value, int referenceId) {
		if(value == null
				|| value.getValueType().equals(ValueType.UNKNOWN)) {
			return null;
		}
		//	Validate values
		if(isLookup(referenceId)
				|| DisplayType.isID(referenceId)) {
			return getObjectFromValue(value);
		} else if(DisplayType.Integer == referenceId) {
			return getIntegerFromValue(value);
		} else if(DisplayType.isNumeric(referenceId)) {
			return getDecimalFromValue(value);
		} else if(DisplayType.YesNo == referenceId) {
			return getBooleanFromValue(value);
		} else if(DisplayType.isDate(referenceId)) {
			return getDateFromValue(value);
		} else if(DisplayType.isText(referenceId)) {
			return getStringFromValue(value);
		}
		//	
		return null;
	}
	
	/**
	 * Is lookup include location
	 * @param displayType
	 * @return
	 */
	public static boolean isLookup(int displayType) {
		return DisplayType.isLookup(displayType)
				|| DisplayType.Account == displayType
				|| DisplayType.Location == displayType
				|| DisplayType.Locator == displayType
				|| DisplayType.PAttribute == displayType;
	}
	
	/**
	 * Convert null on ""
	 * @param value
	 * @return
	 */
	public static String validateNull(String value) {
		if(value == null) {
			value = "";
		}
		//	
		return value;
	}
	
	/**
	 * Get translation if is necessary
	 * @param object
	 * @param columnName
	 * @return
	 */
	public static String getTranslation(PO object, String columnName) {
		if(object == null) {
			return null;
		}
		if(Language.isBaseLanguage(Env.getAD_Language(Env.getCtx()))) {
			return object.get_ValueAsString(columnName);
		}
		//	
		return object.get_Translation(columnName);
	}
	
	/**
	 * Validate if is numeric
	 * @param value
	 * @return
	 */
	public static boolean isNumeric(String value) {
		if(Util.isEmpty(value, true)) {
			return false;
		}
		//	
		return value.matches("[+-]?\\d*(\\.\\d+)?");
	}
	
	/**
	 * Get Int value from String
	 * @param value
	 * @return
	 */
	public static int getIntegerFromString(String value) {
		Integer integerValue = null;
		try {
			integerValue = Integer.parseInt(value);
		} catch (Exception e) {
			
		}
		if(integerValue == null) {
			return 0;
		}
		return integerValue;
	}
	
	
	/**
	 * Validate if is boolean
	 * @param value
	 * @return
	 */
	public static boolean isBoolean(String value) {
		if (Util.isEmpty(value, true)) {
			return false;
		}
		//	
		return value.equals("Y")
			|| value.equals("N")
			|| value.equals("Yes")
			|| value.equals("No")
			|| value.equals("true")
			|| value.equals("false")
		;
	}
	
	/**
	 * Validate Date
	 * @param value
	 * @return
	 */
	public static boolean isDate(String value) {
		return getDateFromString(value) != null;
	}
	
	/**
	 * Is BigDecimal
	 * @param value
	 * @return
	 */
	public static boolean isBigDecimal(String value) {
		return getBigDecimalFromString(value) != null;
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public static BigDecimal getBigDecimalFromString(String value) {
		BigDecimal numberValue = null;
		if (Util.isEmpty(value, true)) {
			return null;
		}
		//	
		try {
			numberValue = new BigDecimal(value);
		} catch (Exception e) {
			
		}
		return numberValue;
	}
	
	/**
	 * Get Date from String
	 * @param value
	 * @return
	 */
	public static Timestamp getDateFromString(String value) {
		if(Util.isEmpty(value)) {
			return null;
		}
		Date date = null;
		try {
			date = DisplayType.getTimestampFormat_Default().parse(value);
		} catch (ParseException e) {
			
		}
		//	Convert
		if(date != null) {
			return new Timestamp(date.getTime());
		}
		return null;
	}

	public static Timestamp getTimestampFromLong(long value) {
		if (value > 0) {
			return new Timestamp(value);
		}
		return null;
	}

	/**
	 * Get long from Timestamp
	 * @param value
	 * @return
	 */
	public static long getLongFromTimestamp(Timestamp value) {
		if (value == null) {
			return 0L;
		}
		return value.getTime();
	}
	
	/**
	 * Set Parameter for Statement from object
	 * @param pstmt
	 * @param value
	 * @param index
	 * @throws SQLException
	 */
	public static void setParameterFromObject(PreparedStatement pstmt, Object value, int index) throws SQLException {
		if(value instanceof Integer) {
			pstmt.setInt(index, (Integer) value);
		} else if(value instanceof Double) {
			pstmt.setDouble(index, (Double) value);
		} else if(value instanceof Long) {
			pstmt.setLong(index, (Long) value);
		} else if(value instanceof BigDecimal) {
			pstmt.setBigDecimal(index, (BigDecimal) value);
		} else if(value instanceof String) {
			pstmt.setString(index, (String) value);
		} else if(value instanceof Timestamp) {
			pstmt.setTimestamp(index, (Timestamp) value);
		} else if(value instanceof Boolean) {
			pstmt.setString(index, ((Boolean) value)? "Y": "N");
		}
	}
	
	/**
	 * Set Parameter for Statement from value
	 * @param pstmt
	 * @param value
	 * @param index
	 * @throws SQLException
	 */
	public static void setParameterFromValue(PreparedStatement pstmt, Value value, int index) throws SQLException {
		if(value.getValueType().equals(ValueType.INTEGER)) {
			pstmt.setInt(index, ValueManager.getIntegerFromValue(value));
		} else if(value.getValueType().equals(ValueType.DECIMAL)) {
			pstmt.setBigDecimal(index, ValueManager.getDecimalFromValue(value));
		} else if(value.getValueType().equals(ValueType.STRING)) {
			pstmt.setString(index, ValueManager.getStringFromValue(value));
		} else if(value.getValueType().equals(ValueType.DATE)) {
			pstmt.setTimestamp(index, ValueManager.getDateFromValue(value));
		}
	}
	
	/**
	 * Convert string to dates
	 * @param date
	 * @return
	 */
	public static Timestamp convertStringToDate(String date) {
		if (Util.isEmpty(date, true)) {
			return null;
		}
		String format = DATE_FORMAT;
		if(date.length() == TIME_FORMAT.length()) {
			format = TIME_FORMAT;
		} else if(date.length() != DATE_FORMAT.length()) {
			throw new AdempiereException("Invalid date format, please use some like this: \"" + DATE_FORMAT + "\" or \"" + TIME_FORMAT + "\"");
		}
		SimpleDateFormat dateConverter = new SimpleDateFormat(format);
		try {
			Date validFromParameter = dateConverter.parse(date);
			return new Timestamp(validFromParameter.getTime());
		} catch (Exception e) {
			throw new AdempiereException(e);
		}
	}
	
	/**
	 * Convert Timestamp to String
	 * @param date
	 * @return
	 */
	public static String convertDateToString(Timestamp date) {
		if(date == null) {
			return null;
		}
		return new SimpleDateFormat(TIME_FORMAT).format(date);
	}

	public static boolean stringToBoolean(String value) {
		if (value != null && ("Y".equals(value) || "Yes".equals(value) || "true".equals(value))) {
			return true;
		}
		return false;
	}

	public static String booleanToString(String value) {
		return booleanToString(stringToBoolean(value), false);
	}
	public static String booleanToString(String value, boolean translated) {
		return booleanToString(stringToBoolean(value), translated);
	}
	public static String booleanToString(boolean value) {
		return booleanToString(value, false);
	}
	public static String booleanToString(boolean value, boolean translated) {
		String convertedValue = "N";
		if (value) {
			convertedValue = "Y";
		}
		if (translated) {
			return Msg.getMsg(Env.getCtx(), convertedValue);
		}
		return convertedValue;
	}

}
