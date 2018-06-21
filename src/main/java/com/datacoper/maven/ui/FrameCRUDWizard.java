package com.datacoper.maven.ui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.datacoper.maven.enums.EnumProject;
import com.datacoper.maven.metadata.TemplateModel;

import se.gustavkarlsson.gwiz.AbstractWizardPage;
import se.gustavkarlsson.gwiz.WizardController;

public class FrameCRUDWizard extends JFrameWizard {
	private static final long serialVersionUID = 1L;

	public FrameCRUDWizard(String projectParentPath) {
		
		File projectParentFile = getAndValidateProjectFile(projectParentPath);
		
		String moduleName = getModuleName(projectParentFile);
		setTitle(moduleName);
		
		TemplateModel templateModel = new TemplateModel(moduleName, projectParentFile);
		
		WizardController controller = new WizardController(this);
		
		AbstractWizardPage startPage = new PanelCRUDEntity(templateModel);
		controller.startWizard(startPage);
		
		getNextButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AbstractWizardPage currentPage = controller.getCurrentPage();
				if(currentPage instanceof AbstractCRUDPanelWizard) {
					((AbstractCRUDPanelWizard)currentPage).onNext();
				}
			}
		});
		
		JButton finishButton = getFinishButton();
		
		finishButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AbstractWizardPage currentPage = controller.getCurrentPage();
				if(currentPage instanceof AbstractCRUDPanelWizard) {
					((AbstractCRUDPanelWizard)currentPage).onFinish();					
				}
			}
		});
		
	}

	private static void setLookAndFeel() {
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		}
	}
	
	private File getAndValidateProjectFile(String projectParent) {
		File projectParentFile = new File(projectParent);
		if(!projectParentFile.exists()) {
			throw new RuntimeException("Parent project folder not exits: "+projectParent);
		}
		
		if(!projectParent.endsWith(EnumProject.PARENT.getSuffix())) {
			throw new RuntimeException("Invalid Parent project: "+projectParent);
		}
		
		return projectParentFile;
	}
	
	private String getModuleName(File projectParentFile) {
		
		String projectParentName = projectParentFile.getName();
		
		return projectParentName.replace(EnumProject.PARENT.getSuffix(), "");
	}

	public static void main(String[] args) {
		
		setLookAndFeel();
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					String projectParentPath = System.getProperty("projectParentPath", "/home/thiago/dev/projetos/Cooperalfa/Homolog/CooperateEE/BusinessCore-Parent");
					
					FrameCRUDWizard frame = new FrameCRUDWizard(projectParentPath);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
