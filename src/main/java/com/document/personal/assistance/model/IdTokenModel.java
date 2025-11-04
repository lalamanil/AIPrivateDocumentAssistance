package com.document.personal.assistance.model;
/**
@author ANIL LALAM
**/
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IdTokenModel {

	@JsonProperty("iss")
	private String iss;
	@JsonProperty("azp")
	private String azp;
	@JsonProperty("aud")
	private String aud;
	@JsonProperty("sub")
	private String sub;
	@JsonProperty("email")
	private String email;
	@JsonProperty("email_verified")
	private boolean email_verified;
	@JsonProperty("at_hash")
	private String at_hash;
	@JsonProperty("name")
	private String name;
	@JsonProperty("picture")
	private String picture;
	@JsonProperty("given_name")
	private String given_name;
	@JsonProperty("family_name")
	private String family_name;
	@JsonProperty("iat")
	private long iat;
	@JsonProperty("exp")
	private long exp;

	public String getIss() {
		return iss;
	}

	public void setIss(String iss) {
		this.iss = iss;
	}

	public String getAzp() {
		return azp;
	}

	public void setAzp(String azp) {
		this.azp = azp;
	}

	public String getAud() {
		return aud;
	}

	public void setAud(String aud) {
		this.aud = aud;
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isEmail_verified() {
		return email_verified;
	}

	public void setEmail_verified(boolean email_verified) {
		this.email_verified = email_verified;
	}

	public String getAt_hash() {
		return at_hash;
	}

	public void setAt_hash(String at_hash) {
		this.at_hash = at_hash;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public String getGiven_name() {
		return given_name;
	}

	public void setGiven_name(String given_name) {
		this.given_name = given_name;
	}

	public String getFamily_name() {
		return family_name;
	}

	public void setFamily_name(String family_name) {
		this.family_name = family_name;
	}

	public long getIat() {
		return iat;
	}

	public void setIat(long iat) {
		this.iat = iat;
	}

	public long getExp() {
		return exp;
	}

	public void setExp(long exp) {
		this.exp = exp;
	}

}
