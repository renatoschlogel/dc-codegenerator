package com.datacoper.maven.metadata;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

public class TemplateAttributeModel {

	private String name;

	private String type;
	
	private String typeSimpleName;
	
	private String typePackage;
	
	private String label;

	private String mask;

	private int precision;

	private int scale;

	private boolean required;
	
	private boolean updatable;

	public TemplateAttributeModel(String name, String type, String label, String mask, int precision, int scale,
			boolean required, boolean updatable) {
		
		Objects.requireNonNull(name);
		Objects.requireNonNull(type);
		Objects.requireNonNull(label);
		
		this.name = name;
		this.label = label;
		this.mask = mask;
		this.precision = precision;
		this.scale = scale;
		this.required = required;
		this.updatable = updatable;
		
		this.type = type;
		
		int lastIndexOfPoint = type.lastIndexOf(".");
		
		this.typeSimpleName  = type.substring(lastIndexOfPoint+1, type.length());
		
		this.typePackage = type.substring(0, lastIndexOfPoint);
		
	}

	public String getType() {
		return type;
	}
	
	public String getTypePackage() {
		return typePackage;
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public String getMask() {
		return mask;
	}

	public int getPrecision() {
		return precision;
	}

	public int getScale() {
		return scale;
	}

	public boolean isRequired() {
		return required;
	}
	
	public boolean isUpdatable() {
		return updatable;
	}
	
	public boolean isNumber() {
		return type.equals(Integer.class.getName()) ||
				type.equals(Long.class.getName()) ||
				type.equals(int.class.getName()) ||
				type.equals(long.class.getName());
	}
	
	public boolean isDate() {
		return type.equals(Date.class.getName()) ||
				type.equals(java.sql.Date.class.getName()) ||
				type.equals(Timestamp.class.getName());
	}
	
	public boolean isText() {
		return type.equals(String.class.getName()) ||
				type.equals(Character.class.getName());
	}
	
	public boolean isDecimal() {
		return type.equals(Double.class.getName()) ||
				type.equals(double.class.getName()) ||
				type.equals(BigDecimal.class.getName());
	}
	
	public boolean isBoolean() {
		return type.equals(boolean.class.getName()) ||
				type.equals(Boolean.class.getName());
	}
	
	public String getTypeSimpleName() {
		return typeSimpleName;
	}
	
	public String getFrontType() {
		if(isText()) {
			return "text";
		}
		if(isNumber() || isDecimal()) {
			return "number";
		}
		
		if(isDate()) {
			return "date";
		}
		
		if(isBoolean()) {
			return "boolean";
		}
		
		return "seletor";
	}
	
}
