package com.datacoper.maven.ui;

import java.awt.BorderLayout;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.datacoper.cooperate.arquitetura.client.layout.VerticalFlowLayout;
import com.datacoper.cooperate.arquitetura.common.beans.BeanUtil;
import com.datacoper.cooperate.arquitetura.common.util.DateUtil;
import com.datacoper.cooperate.arquitetura.common.util.Entry;
import com.datacoper.maven.enums.Company;
import com.datacoper.maven.enums.EnumDCModule;
import com.datacoper.maven.metadata.TemplateAttributeModel;
import com.datacoper.maven.metadata.TemplateModel;
import com.datacoper.maven.metadata.TemplateModelDetail;
import com.datacoper.maven.util.ColumnNameResolver;
import com.datacoper.maven.util.StringUtil;
import com.datacoper.testes.persistence.PersistenceProperties;
import com.datacoper.testes.persistence.PersistenceProperties.DBType;

import se.gustavkarlsson.gwiz.AbstractWizardPage;

public class PanelCRUDAttributes extends AbstractCRUDPanelWizard {
	private static final long serialVersionUID = 1L;
	
	private PanelCRUDClasses panelCRUDClasses;
	
	private Map<String, TableAttributes> tablesAttributes = new HashMap<String, TableAttributes>();
	
	private JPanel container = new JPanel();
	
	public PanelCRUDAttributes(TemplateModel templateModel) {
		super(templateModel);
		
		VerticalFlowLayout verticalLayout = new VerticalFlowLayout();
		verticalLayout.setVgap(2);
		container.setLayout(verticalLayout);
		
		setLayout(new BorderLayout());
		add(new JScrollPane(container), BorderLayout.CENTER);
		
		panelCRUDClasses = new PanelCRUDClasses(templateModel);
		
	}

	public void init() {
		TemplateModel templateModel = getTemplateModel();
		String entityName = templateModel.getEntityName();
		
		container.removeAll();
		tablesAttributes.clear();
		
		if(entityName != null) {
			
			TableAttributes tableAttributes = createAndAddTableAttribute(entityName, templateModel.getCompany());
			
			PersistenceProperties persistenceProperties = new PersistenceProperties(DBType.PHYSICAL, templateModel.getProjectParentFile().getAbsolutePath()+File.separator);
			
			try (Connection connection = DriverManager.getConnection(persistenceProperties.url, persistenceProperties.user, persistenceProperties.password)){
				
				populateAttributes(entityName, connection, tableAttributes);
				
				for (TemplateModelDetail entityDetail: templateModel.getDetails()) {
					
					TableAttributes tableAttributesDetail = createAndAddTableAttribute(entityDetail.getEntityName(), templateModel.getCompany());
					
					populateAttributes(entityDetail.getEntityName(), connection, tableAttributesDetail);
					
				}
				
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		
		}
		
	}

	private TableAttributes createAndAddTableAttribute(String entityName, Company company) {
		TableAttributes tableAttributes = new TableAttributes(company);
		
		container.add(new JLabel(entityName));
		container.add(new JScrollPane(tableAttributes));
		
		tablesAttributes.put(entityName, tableAttributes);
		
		return tableAttributes;
		
	}

	private void populateAttributes(String entityName, Connection connection, TableAttributes tableAttributes) throws SQLException {
		try(Statement statement = connection.createStatement()){
		
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			
			ResultSet rs = databaseMetaData.getImportedKeys(null, null, entityName.toUpperCase());
			
			ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(null, null, entityName.toUpperCase());
			
			primaryKeys.next();
			
			String primaryKey =  primaryKeys.getString("COLUMN_NAME");
			
			Map<String, String> mapForeignTables = new HashMap<>();
			
			while(rs.next()) {
				String fkColumnName = rs.getString("FKCOLUMN_NAME");
				String pkTableName = rs.getString("PKTABLE_NAME");
				mapForeignTables.put(fkColumnName, pkTableName);
			}
			
			ResultSet resultSet = statement.executeQuery("select * from "+entityName+" where 0 = 1");
			
			ResultSetMetaData metaData = resultSet.getMetaData();
			
			int columnCount = metaData.getColumnCount();
			
			ColumnNameResolver columnNameResolver = new ColumnNameResolver();
			
			for (int i = 1; i <= columnCount; i++) {
				String columnName = metaData.getColumnName(i);
				
				if(!isPrimaryKey(primaryKey, columnName)) {
					Entry<String, String> revolverFieldAndLabel = columnNameResolver.revolverFieldAndLabel(columnName);
					String attributeName = revolverFieldAndLabel.getKey();
					String attributeLabel = StringUtil.isNotNullOrEmpty(revolverFieldAndLabel.getValue()) ? revolverFieldAndLabel.getValue() : attributeName;
					String columnType = metaData.getColumnClassName(i);
					
					Boolean nullable = metaData.isNullable(i) == 1;
					
					int precision = metaData.getPrecision(i);
					int scale = metaData.getScale(i);
					
					columnType = resolveColumnClassName(columnType, precision ,scale);
					
					String mask = getMascaraDefault(columnType);
					
					String fkTableName = mapForeignTables.get(columnName);
					
					EnumDCModule enumDCModule = null;
					
					if(fkTableName != null) {
						attributeName = StringUtil.lowerFirstCharacter(attributeName.substring(2)); //remover o prefixo "id"
						columnType = StringUtil.upperFirstCharacter(columnNameResolver.revolverFieldAndLabel(fkTableName).getKey());
						enumDCModule = EnumDCModule.from(getTemplateModel().getModuleName());
					}
					
					boolean updatable = isUpdatable(attributeName);
					
					tableAttributes.addRow(columnName, attributeName, attributeLabel, columnType, enumDCModule, null, !nullable, mask, precision, scale, updatable);
					
				}
				
			}
			
		}
	}
	
	private boolean isUpdatable(String attributeName) {
		attributeName = attributeName.toUpperCase();
		return  !attributeName.equals("CODIGO") &&
				!attributeName.equals("IDGRUPOEMPRESARIAL") &&
				!attributeName.equals("IDFILIAL") &&
				!attributeName.equals("IDEMPRESA") &&
				!attributeName.equals("IDUSUARIO") &&
				!attributeName.contains("DATA") &&
				!attributeName.contains("FLAG");
		
	}

	private String resolveColumnClassName(String columnClassName, int precision, int scale) {
		if(BigDecimal.class.getName().equals(columnClassName) && !(scale > 0)) {
			
			if(precision == 1) {
				return Boolean.class.getName();
			}
			return Long.class.getName();
		}
		return columnClassName;
	}

	private Class<?> tryCreateClass(String className){
		try {
			return BeanUtil.createClass(className);
		}catch (Exception e) {
			return null;
		}
	}
	
	private String getMascaraDefault(String columnClassName) {
		
		Class<?> typeClass = tryCreateClass(columnClassName);
		
		if(typeClass != null) {
			if(Date.class.isAssignableFrom(typeClass)) {
				return DateUtil.DDMMYYYYHHMMSS.toPattern();
			}
		}
		return null;
	}

	private boolean isPrimaryKey(String primaryKeyName, String columnName) {
		return primaryKeyName.toLowerCase().equals(columnName.toLowerCase());
	}

	@Override
	protected AbstractWizardPage getNextPage() {
		return panelCRUDClasses;
	}

	@Override
	protected boolean isCancelAllowed() {
		return true;
	}

	@Override
	protected boolean isPreviousAllowed() {
		return true;
	}

	@Override
	protected boolean isNextAllowed() {
		return true;
	}

	@Override
	protected boolean isFinishAllowed() {
		return false;
	}

	@Override
	void onNext() {
		
		TemplateModel templateModel = getTemplateModel();
		
		Set<TemplateAttributeModel> attributes = tablesAttributes.get(templateModel.getEntityName()).getAsTemplateAttributeModel(templateModel);		
		templateModel.setAttributes(attributes);
		
		Set<TemplateModelDetail> details = templateModel.getDetails();
		
		for (TemplateModelDetail templateModelDetail : details) {
			TableAttributes tableAttributesDetail = tablesAttributes.get(templateModelDetail.getEntityName());
			
			templateModelDetail.setAttributes(tableAttributesDetail.getAsTemplateAttributeModel(templateModelDetail));
			
		}
		
		panelCRUDClasses.init();
	}

	@Override
	void onFinish() {
	}

}
