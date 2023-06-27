/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2015 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.spin.authentication;

import java.math.BigDecimal;
import java.security.Key;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSession;
import org.compiere.model.MSysConfig;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.eca52.util.JWTUtil;
import org.spin.model.MADToken;
import org.spin.model.MADTokenDefinition;
import org.spin.util.IThirdPartyAccessGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * A simple token generator for third party access
 * Note: You should generate your own token generator
 * @author Yamel Senih, ySenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class LoginTokenAccess implements IThirdPartyAccessGenerator {

	/**	Default Token	*/
	private MADToken token = null;
	/**	User Token value	*/
	private String userTokenValue = null;
	 
    public LoginTokenAccess() {
    	//	
    }
	
    @Override
	public String generateToken(String tokenType, int userId) {
		return generateToken(userId, 0);
	}

	
	@Override
	public boolean validateToken(String token, int userId) {
		return false;
	}
	
	@Override
	public MADToken getToken() {
		return token;
	}
	
	@Override
	public String getTokenValue() {
		return userTokenValue;
	}

	@Override
	public String generateToken(int userId, int roleId) {
		//	Validate user
		if(userId < 0) {
			throw new AdempiereException("@AD_User_ID@ @NotFound@");
		}
		//	Validate Role
		if(roleId < 0) {
			throw new AdempiereException("@AD_Role_ID@ @NotFound@");
		}
		MADToken token = new MADToken(Env.getCtx(), 0, null);
        token.setTokenType(MADTokenDefinition.TOKENTYPE_ThirdPartyAccess);
        if(token.getAD_TokenDefinition_ID() <= 0) {
        	throw new AdempiereException("@AD_TokenDefinition_ID@ @NotFound@");
        }
        MADTokenDefinition definition = MADTokenDefinition.getById(Env.getCtx(), token.getAD_TokenDefinition_ID(), null);
        //	Used for third Party Access
        MSession session = new MSession (Env.getCtx(), null);
        session.setAD_Org_ID(roleId);
        session.setAD_Role_ID(roleId);
        session.setDescription("Created from Token");
        session.saveEx();
        //	TODO: Generate from ADempiere
        String secretKey = MSysConfig.getValue(JWTUtil.ECA52_JWT_SECRET_KEY, session.getAD_Client_ID());
		if(Util.isEmpty(secretKey)) {
			throw new AdempiereException("@InternalError@");
		}
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        BearerToken generatedToken = new BearerToken(Jwts.builder()
                .setSubject(String.valueOf(Env.getAD_Org_ID(Env.getCtx())))
                .setId(String.valueOf(userId))
                .setAudience(String.valueOf(roleId))
                .setIssuer(String.valueOf(session.getAD_Session_ID()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact());
		// Digest computation
		userTokenValue = generatedToken.toString();
		if(Util.isEmpty(userTokenValue)) {
			throw new AdempiereException("@TokenValue@ @NotFound@");
		}
		//	Validate
        if(definition.isHasExpireDate()) {
        	BigDecimal expirationTime = Optional.ofNullable(definition.getExpirationTime()).orElse(new BigDecimal(5 * 60 * 1000));
        	token.setExpireDate(new Timestamp(System.currentTimeMillis() + expirationTime.longValue()));
        }
        token.setTokenValue(userTokenValue);
        token.setAD_User_ID(userId);
        token.setAD_Role_ID(roleId);
        token.saveEx();
        return userTokenValue;
	}

	@Override
	public boolean validateToken(String tokenValue) {
		//	Create ADempiere session, throw a error if it not exists
		try {
			Base64.Decoder decoder = Base64.getUrlDecoder();
			String[] chunks = tokenValue.split("\\.");
			if(chunks == null || chunks.length < 3) {
				throw new AdempiereException("@TokenValue@ @NotFound@");
			}
			String payload = new String(decoder.decode(chunks[1]));
			ObjectMapper mapper = new ObjectMapper();
			@SuppressWarnings("unchecked")
			Map<String, Object> dataAsMap = mapper.readValue(payload, Map.class);
			Object clientAsObject = dataAsMap.get("AD_Client_ID");
			if(clientAsObject == null
					|| !clientAsObject.getClass().isAssignableFrom(Integer.class)) {
				throw new AdempiereException("@Invalid@ @AD_Client_ID@");
			}
			String secretKey = MSysConfig.getValue(JWTUtil.ECA52_JWT_SECRET_KEY, (int) clientAsObject);
			if(Util.isEmpty(secretKey)) {
				throw new AdempiereException("@InternalError@");
			}
	        JwtParser parser = Jwts.parserBuilder().setSigningKey(secretKey).build();
	        Jws<Claims> claims = parser.parseClaimsJws(tokenValue);
	        token = new MADToken(Env.getCtx(), 0, null);
			String language = claims.getBody().get("AD_Language", String.class);
			token.setAD_User_ID(claims.getBody().get("AD_User_ID", Integer.class));
			token.setAD_Role_ID(claims.getBody().get("AD_Role_ID", Integer.class));
			token.setAD_Org_ID(claims.getBody().get("AD_Org_ID", Integer.class));
			Env.setContext(Env.getCtx(), Env.LANGUAGE, ContextManager.getDefaultLanguage(language));
			if(!Util.isEmpty(claims.getBody().getId())) {
				Env.setContext(Env.getCtx(), "#AD_Session_ID", Integer.parseInt(claims.getBody().getId()));
			} else {
				Env.setContext (Env.getCtx(), "#AD_Session_ID", 0);
			}
		} catch (Exception e) {
			new AdempiereException(e);
		}
		return true;
	}
}
