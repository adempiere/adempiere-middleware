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
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSession;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.model.MADToken;
import org.spin.model.MADTokenDefinition;
import org.spin.server.setup.SetupLoader;
import org.spin.util.IThirdPartyAccessGenerator;

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
		byte[] keyBytes = Decoders.BASE64.decode(SetupLoader.getInstance().getServer().getAdempiere_token());
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
        JwtParser parser = Jwts.parserBuilder().setSigningKey(SetupLoader.getInstance().getServer().getAdempiere_token()).build();
        Jws<Claims> claims = parser.parseClaimsJws(tokenValue);
        token = new MADToken(Env.getCtx(), 0, null);
		token.setAD_User_ID(Integer.parseInt(claims.getBody().getId()));
		token.setAD_Role_ID(Integer.parseInt(claims.getBody().getAudience()));
		token.setAD_Org_ID(Integer.parseInt(claims.getBody().getSubject()));
		if(!Util.isEmpty(claims.getBody().getIssuer())) {
			Env.setContext(Env.getCtx(), "#AD_Session_ID", Integer.parseInt(claims.getBody().getIssuer()));
		} else {
			Env.setContext (Env.getCtx(), "#AD_Session_ID", 0);
		}
		//	Is Ok
		return true;
	}
}
