package com.hhc.sy.custonms.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.teamcenter.rac.kernel.NamedReferenceContext;
import com.teamcenter.rac.kernel.TCComponent;
import com.teamcenter.rac.kernel.TCComponentDataset;
import com.teamcenter.rac.kernel.TCComponentDatasetDefinition;
import com.teamcenter.rac.kernel.TCComponentTcFile;

public class RacDatasetUtil {
	
	public static NamedReferenceContext[] getNameRefContext(TCComponentDataset dataset) throws Exception {
		
		TCComponentDatasetDefinition datasetDef = dataset.getDatasetDefinitionComponent();

	    NamedReferenceContext[] nameRefContexts = datasetDef.getNamedReferenceContexts();
	    
	    return nameRefContexts;
	}
	
	public static List<TCComponentDataset> getDatasets(TCComponent tcComponent) throws Exception{
		TCComponent[] relateds = tcComponent.getRelatedComponents();
		List<TCComponentDataset> datasets = new ArrayList<>();
		if(relateds != null && relateds.length > 0) {
			for (TCComponent related : relateds) {
				if(related instanceof TCComponentDataset) {
					datasets.add((TCComponentDataset) related);
				}
			}
		}
		return datasets;
	}
	
	/**
	 *   这个方法能拿到数据集中的所有数据集文件，它们是自定义的MyRacDatasetFile类，里面有download方法可以直接下载数据集
	 * @param dataset
	 * @return
	 * @throws Exception
	 */
	public static List<MyRacDatasetFile> getRacDatasetFile(TCComponentDataset dataset, TCComponent parent) throws Exception {
		
		TCComponent[] refList = dataset.getReferenceListProperty("ref_list");
		List<MyRacDatasetFile> racDatasetFiles = new ArrayList<>();
		if(refList != null && refList.length > 0) {
			for (TCComponent ref : refList) {
				if(ref instanceof TCComponentTcFile) {
					TCComponentTcFile tcFile = (TCComponentTcFile) ref;
					racDatasetFiles.add(new MyRacDatasetFile(tcFile, parent));
				}
			}
		}
		
		return racDatasetFiles;
	}
	
	public static String[] getTCFile(TCComponentDataset dataset, String folderName) throws Exception{
		
	      String datasetName = dataset.getProperty("object_name");

	      TCComponentDatasetDefinition datasetDef = dataset.getDatasetDefinitionComponent();

	      NamedReferenceContext[] nameRefContexts = datasetDef.getNamedReferenceContexts();
	      if ((nameRefContexts == null) || (nameRefContexts.length == 0)) return null;

	      NamedReferenceContext nf = nameRefContexts[0];
	      String namedRef = nf.getNamedReference();

	      String[] fileNames = dataset.getFileNames(namedRef);
	      if ((fileNames == null) || (fileNames.length == 0)) return null;

	      for (String fileName : fileNames) {
	        File ret = dataset.getFile(namedRef, fileName, folderName);

	        if (ret == null) {
	        	System.out.println("下载文件异常：" + folderName + "/" + fileName);
	        }

	        System.out.println(fileName + " = " + datasetName);
	      }
	      return fileNames;
	  }
}
