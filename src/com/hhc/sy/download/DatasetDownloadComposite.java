package com.hhc.sy.download;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

import com.hhc.sy.custonms.util.MyRacDatasetFile;
import com.teamcenter.rac.kernel.TCException;

import nebula.Dialog;
import swing2swt.layout.BorderLayout;
import swing2swt.layout.FlowLayout;

public class DatasetDownloadComposite extends Composite {
	private Table contextTable;
	private Text pathText;
	private Composite typeComposite;
	List<MyRacDatasetFile> datasetFiles;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public DatasetDownloadComposite(List<MyRacDatasetFile> datasetFiles, Composite parent, int style) {
		super(parent, style);
		FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
		fillLayout.marginHeight = 5;
		fillLayout.marginWidth = 5;
		fillLayout.spacing = 10;
		setLayout(fillLayout);
		
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayout(new BorderLayout(5, 5));
		
		typeComposite = new Composite(composite, SWT.BORDER);
		typeComposite.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
		typeComposite.setLayoutData(BorderLayout.NORTH);
		
//		Button btnCheckButton = new Button(typeComposite, SWT.CHECK);
//		btnCheckButton.setText("excel");
//		
//		Button btnCheckButton_1 = new Button(typeComposite, SWT.CHECK);
//		btnCheckButton_1.setText("dwg");
		
		Composite downloadComposite = new Composite(composite, SWT.NONE);
		FormLayout fl_downloadComposite = new FormLayout();
		downloadComposite.setLayout(fl_downloadComposite);
		downloadComposite.setLayoutData(BorderLayout.SOUTH);
		
		pathText = new Text(downloadComposite, SWT.BORDER);
		FormData fd_text = new FormData();
//		fd_text.right = new FormAttachment(0, 358);
		fd_text.top = new FormAttachment(0, 7);
		fd_text.bottom = new FormAttachment(100, -37);
		fd_text.left = new FormAttachment(0);
		pathText.setLayoutData(fd_text);
		
		Button loadPathButton = new Button(downloadComposite, SWT.NONE);
		fd_text.right = new FormAttachment(loadPathButton, -6);
		FormData fd_loadPathButton = new FormData();
		fd_loadPathButton.left = new FormAttachment(100, -65);
		fd_loadPathButton.bottom = new FormAttachment(100, -37);
		fd_loadPathButton.top = new FormAttachment(0, 7);
//		fd_loadPathButton.left = new FormAttachment(0, 364);
		fd_loadPathButton.right = new FormAttachment(100);
		loadPathButton.setLayoutData(fd_loadPathButton);
		loadPathButton.setText("浏览...");
		
		Composite composite_1 = new Composite(downloadComposite, SWT.NONE);
		composite_1.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
		FormData fd_composite_1 = new FormData();
		fd_composite_1.right = new FormAttachment(loadPathButton, 0, SWT.RIGHT);
		fd_composite_1.left = new FormAttachment(0);
		fd_composite_1.top = new FormAttachment(pathText, 7);
		fd_composite_1.bottom = new FormAttachment(100);
		composite_1.setLayoutData(fd_composite_1);
		
		final Button downloadButton = new Button(composite_1, SWT.NONE);
		downloadButton.setText("  下载  ");
		downloadButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				final TableItem[] items = contextTable.getItems();
				downloadButton.setImage(createImage(getDisplay(), "loading.gif"));
				downloadButton.setEnabled(false);
				new Thread() {
					public void run() {
						for (final TableItem tableItem : items) {
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									DatasetTableItem dti = ((DatasetTableItem)tableItem);
									MyRacDatasetFile datasetFile = (MyRacDatasetFile) dti.getData();
									if(dti.isSelected()) {
										try {
											datasetFile.download(pathText.getText(), true);
											dti.setBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
										} catch (TCException e) {
											e.printStackTrace();
											dti.setBackground(SWTResourceManager.getColor(SWT.COLOR_RED));
											Dialog.error("下载失败", e.getMessage());
										}
									}
									contextTable.setSelection(tableItem);
								}
							});
						}
						
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								Dialog.inform("提示", "下载成功");
								downloadButton.setImage(null);
								downloadButton.setEnabled(true);
							}
						});
					}
				}.start();
				
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		Button cancelButton = new Button(composite_1, SWT.NONE);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getShell().dispose();
			}
		});
		cancelButton.setText("  取消  ");
		loadPathButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DirectoryDialog fd = new DirectoryDialog(DatasetDownloadComposite.this.getShell(), SWT.OPEN);
//				fd.setFilterPath(System.getProperty("JAVA.HOME"));
				String path = fd.open();
				pathText.setText(path);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}
		});
		
		contextTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
		contextTable.setHeaderVisible(true);
		contextTable.setLinesVisible(true);
		contextTable.setLayoutData(BorderLayout.CENTER);
		
		for(int i = 0; i < DatasetTableItem.columnNames.length; i++) {
			TableColumn tblclmnNewColumn = new TableColumn(contextTable, SWT.NONE);
			tblclmnNewColumn.setWidth(DatasetTableItem.columnWidths[i]);
			tblclmnNewColumn.setText(DatasetTableItem.columnNames[i]);
		}
		
		setDatasetFile(datasetFiles);
		contextTable.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(contextTable.getSelectionCount() > 0) {
					DatasetTableItem dti = (DatasetTableItem) contextTable.getSelection()[0];
					dti.setSelected(!dti.isSelected());
				}
			}
		});
	}
	
	public void setDatasetFile(List<MyRacDatasetFile> datasetFiles) {
		
		this.datasetFiles = datasetFiles;
		if(contextTable.getItemCount() > 0) {
			for (TableItem tableItem : contextTable.getItems()) {
				tableItem.dispose();
			}
		}
		Control[] typeControls = typeComposite.getChildren();
		if(typeControls != null && typeControls.length > 0) {
			for (Control control : typeControls) {
				control.dispose();
			}
		}
		
		Set<String> types = new HashSet<String>();
		for (MyRacDatasetFile myRacDatasetFile : datasetFiles) {
			new DatasetTableItem(myRacDatasetFile,contextTable, SWT.NONE);
			if(!types.contains(myRacDatasetFile.getFileExt())) {
				types.add(myRacDatasetFile.getFileExt());
				addDatasetType(myRacDatasetFile.getFileExt());
			}
		}
	}

	public void addDatasetType(String typeName) {
		Button btnCheckButton = new Button(typeComposite, SWT.CHECK);
		btnCheckButton.setText(typeName);
		btnCheckButton.setSelection(true);
		btnCheckButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				Control[] typeControls = typeComposite.getChildren();
				if(typeControls != null && typeControls.length > 0) {
					List<String> selectedButton = new ArrayList<>();
					for (Control control : typeControls) {
						Button btn = (Button)control;
						if(btn.getSelection()) {
							selectedButton.add(btn.getText());
						}
					}
					
					for (TableItem tableItem : contextTable.getItems()) {
						DatasetTableItem dti = ((DatasetTableItem)tableItem);
						MyRacDatasetFile datasetFile = (MyRacDatasetFile) dti.getData();
						if(selectedButton.contains(datasetFile.getFileExt())) {
							dti.setSelected(true);
						}else {
							dti.setSelected(false);
						}
					}
					
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}
		});
	}
	
	@Override
	protected void checkSubclass() {
	}
	
	protected Image createImage(Display display, String imagName) {
		InputStream is = this.getClass().getResourceAsStream("/resources/"+imagName);
		return is == null ? null : new Image(display,  is);
	}
}

class DatasetTableItem extends TableItem{
	
	public static final String[] columnNames = new String[] {"文件名","类型","来自","大小"};
	public static final int[] columnWidths = new int[] {300,80,280,100};
	
	private boolean selected = true;

	public DatasetTableItem(MyRacDatasetFile datasetFile, Table parent, int style) {
		super(parent, style);
		load(datasetFile);
		updateSelectedState();
	}
	
	public void load(MyRacDatasetFile datasetFile) {
		setData(datasetFile);
		String parentName = datasetFile.getParent() == null ? "" : datasetFile.getParent().toStringLabel();
		setText(new String[] {datasetFile.getOriginalFileName(), datasetFile.getFileExt(), parentName, datasetFile.getFileSize()});
	}
	
	public void setSelected(boolean isSelected) {
		this.selected = isSelected;
		updateSelectedState();
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public void updateSelectedState() {
		setImage(createImage(getDisplay(), selected ? "check1_16.png" : "check0_16.png"));
	}
	
	@Override
	protected void checkSubclass() {
	}
	
	protected Image createImage(Display display, String imagName) {
		InputStream is = this.getClass().getResourceAsStream("/resources/"+imagName);
		return is == null ? null : new Image(display,  is);
	}
}
