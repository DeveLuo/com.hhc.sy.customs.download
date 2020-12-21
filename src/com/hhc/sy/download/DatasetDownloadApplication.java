package com.hhc.sy.download;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

import com.hhc.sy.custonms.util.MyRacDatasetFile;

import swing2swt.layout.BorderLayout;

public class DatasetDownloadApplication {

	private List<MyRacDatasetFile> datasetFiles;
	protected Shell shell;

	public DatasetDownloadApplication(List<MyRacDatasetFile> datasetFiles) {
		super();
		this.datasetFiles = datasetFiles;
	}

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			DatasetDownloadApplication window = new DatasetDownloadApplication(null);
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(720, 400);
		shell.setText("数据集下载");
		shell.setLayout(new BorderLayout(10, 10));
		shell.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		
		new DatasetDownloadComposite(datasetFiles, shell, SWT.NONE);
		
	}

}
