package com.hhc.sy.custonms.util;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.teamcenter.rac.aif.kernel.InterfaceAIFComponent;
import com.teamcenter.rac.aifrcp.AIFUtility;
import com.teamcenter.rac.commands.refresh.RefreshOperation;
import com.teamcenter.rac.kernel.TCComponent;
import com.teamcenter.rac.kernel.TCComponentFolder;
import com.teamcenter.rac.kernel.TCComponentTaskInBox;
import com.teamcenter.rac.kernel.TCException;
import com.teamcenter.rac.kernel.TCPreferenceService;
import com.teamcenter.rac.kernel.TCPreferenceService.TCPreferenceLocation;
import com.teamcenter.rac.kernel.TCSession;
import com.teamcenter.services.loose.core.DataManagementService;
import com.teamcenter.services.loose.core._2008_06.DataManagement.CreateIn;
import com.teamcenter.services.loose.core._2008_06.DataManagement.CreateResponse;

/**
 * 刷新操作调用本方法执行流程审批记录逻辑
 * 本方法的调用需：
 * 1.替换com.teamcenter.rac.common包中的RefreshOperation类(替换类在本包)
 * 2.修改com.teamcenter.rac.common包中的MANIFEST.MF文件，在Require-Bundle:后添加com.hhc.sy.customs.download;bundle-version="1.0.0"
 * @author DEVE
 *
 */
public class TaskApprovalRecordUtil {

	TCSession session;
	TCPreferenceService preferenceService;
	String user_id;
	String task_types = "EPMPerformSignoffTask;EPMDoTask;EPMConditionTask";
	Calendar calendar = Calendar.getInstance();
	public static String PROJECT_DEFAULT_MUN = "PROJECT_DEFAULT_MUN";
	public static String PROJECT_MORE_MUN = "PROJECT_MORE_MUN";
	public static String TASK_DEFAULT_MUN = "TASK_DEFAULT_MUN";
	public static String TASK_MORE_MUN = "TASK_MORE_MUN";
	String property_folder = "sy6_task_folder"; // TypeReference/S7_CustomFolder
	String folderType = "SY6_TaskFolder";
	String property_type = "sy6_type"; // String 类型
	String property_more = "sy6_more"; // TypeReference/S7_CustomFolder
	String property_today = "sy6_today"; // TypeReference/S7_CustomFolder
	String property_year = "sy6_year"; // int 类型
	String property_parent = "sy6_parent"; // TypeReference/S7_CustomFolder
	String property_standard = "sy6_standard"; // TypeReference/S7_CustomFolder
	String property_other = "sy6_other"; // TypeReference/S7_CustomFolder
	String property_contents = "contents";
	
	
	public TaskApprovalRecordUtil() throws TCException {
		session = (TCSession) AIFUtility.getDefaultSession();
		user_id = session.getUser().getUserId();
		preferenceService = session.getPreferenceService();
	}
	
	public void customRefresh(RefreshOperation operation) throws Exception {
		Class<?> cls = operation.getClass();
		Field[] fields = cls.getDeclaredFields();
		InterfaceAIFComponent m_target = null;
		InterfaceAIFComponent[] m_theTargets = null;
		for (Field field : fields) {
            field.setAccessible(true);
            String name = field.getName();
            if ("m_target".equals(name)) {
            	m_target = (InterfaceAIFComponent) field.get(operation);
			} else if ("m_theTargets".equals(name)) {
				m_theTargets = (InterfaceAIFComponent[]) field.get(operation);
			}
		}
		
		if (m_target != null) {
			refreshFolder(m_target);
		}
		
		if (m_theTargets != null) {
			refreshFolders(m_theTargets);
		}
		System.out.println("customRefresh Comptete.");
	}
	
	public void refreshTaskInbox(TCComponentTaskInBox taskInbox) throws TCException {
		System.out.println("任务箱数据更新[" + taskInbox + "]");
		TCComponentFolder taskFolder = (TCComponentFolder) taskInbox.getReferenceProperty(property_folder);
		if (taskFolder == null || !"流程审批记录".equals(taskFolder.getProperty(property_type))) {
			taskFolder = createFolder(session, folderType, "流程审批记录", "流程审批记录", null);
			taskInbox.setReferenceProperty(property_folder, taskFolder);
		}
//		refreshFolder(taskFolder);
	}
	
	public void refreshFolders(InterfaceAIFComponent[] coms) throws TCException {
		if (coms != null) {
			for (InterfaceAIFComponent folder : coms) {
				refreshFolder(folder);
			}
		}
	}
	
	public void refreshFolder(InterfaceAIFComponent com) throws TCException {
		if (com instanceof TCComponentTaskInBox) {
			refreshTaskInbox((TCComponentTaskInBox) com);
			return;
		} 
		if (com instanceof TCComponentFolder) {
			refreshFolder((TCComponentFolder) com);
		}
	}
	
	public void refreshFolder(TCComponentFolder folder) throws TCException {
		if (folderType.equals(folder.getType())) {
			String type = folder.getProperty(property_type);
			switch (type) {
			case "流程审批记录":
				refreshTasksCompleteFolder(folder);
				break;
			case "今天":
				refreshTodayFolder(folder);
				break;
			case "年份":
				refreshYearFolder(folder);
				break;
			case "更多":
				refreshMoreFolder(folder);
				break;
			case "项目":
				refreshProjectFolder(folder);
				break;
			case "标准制修订":
				refreshStandardFolder(folder);
				break;
			case "其他流程":
				refreshOtherFolder(folder);
				break;
			default:
				break;
			}
		}
	}
	
	// 更新流程审批记录文件夹下结构
	public void refreshTasksCompleteFolder(TCComponentFolder folder) throws TCException {
		
		System.out.println("流程审批记录数据更新[" + folder + "]");
		
		//获取今天文件夹
		TCComponentFolder today = (TCComponentFolder) folder.getReferenceProperty(property_today);
		if (today == null) {
			today = createFolder(session, folderType, "今天", "今天", null);
			folder.setReferenceProperty(property_today, today);
		}
		refreshTodayFolder(today);
		
		// 获取更多年份文件夹
		TCComponentFolder more = (TCComponentFolder) folder.getReferenceProperty(property_more);
		if (more == null || !"更多".equals(more.getProperty(property_type))) {
			more = createFolder(session, folderType, "更多", "更多", null);
			more.setReferenceProperty(property_parent, folder);
			folder.setReferenceProperty(property_more, more);
		}
		
		// 更新更多文件夹内容
		TCComponent[] more_years = more.getReferenceListProperty(property_contents);
		TCComponent[] default_years = folder.getReferenceListProperty(property_contents);
		TCComponent[] dates = concatComs(default_years, more_years);
		Map<Integer, TCComponentFolder> dateFolders = new HashMap<>();
		for (TCComponent dateCom : dates) {
			if (folderType.equals(dateCom.getType())) {
				int year = dateCom.getIntProperty(property_year);
				dateFolders.put(year, (TCComponentFolder) dateCom);
			}
		}
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int endYear = calendar.get(Calendar.YEAR);
		int startYear = 2016;
		int years = endYear - startYear + 1;
		if (years > 10) {
			years = 10;
		}
		// 更新内容文件夹
		int default_year = 3;
		if (years < 3) {
			default_year = years;
		}
				
		TCComponentFolder[] all_years = new TCComponentFolder[years];
		for (int i = 0; i < years; i++) {
			TCComponentFolder dateFolder = dateFolders.get(endYear);
			if (dateFolder == null) {
				dateFolder = createFolder(session, folderType, endYear + "年", "年份",endYear);
				dateFolder.setIntProperty("s7_year", endYear);
			}
			all_years[i] = dateFolder;
			calendar.add(Calendar.YEAR, -1); // 年份-1
			endYear = calendar.get(Calendar.YEAR);
		}
		
		more_years = new TCComponentFolder[years - default_year];
		for (int i = 0; i < more_years.length; i++) {
			more_years[i] = all_years[i + default_year];
		}
		if (more_years != null && more_years.length > 0) {
			more.setRelated(property_contents,more_years);
		}
		
		
		TCComponentFolder[] contents = new TCComponentFolder[default_year];
		for (int i = 0; i < contents.length; i++) {
			contents[i] = all_years[i];
		}
		folder.setRelated(property_contents, contents);
		
		// 更新年份文件夹内容
		for (int i = 0; i < all_years.length; i++) {
			refreshYearFolder(all_years[i]);
		}
	}
	
	// 获取日期
	public String getDateString(Date date) {
		if (date == null) {
			return "";
		}
		SimpleDateFormat format = new SimpleDateFormat("yyyy-M-dd");
		return format.format(date);
		
	}
	
	// 更新年份数据内容
	public void refreshYearFolder(TCComponentFolder folder) throws TCException {
		
		System.out.println("年份数据更新[" + folder + "]");
		int year = folder.getIntProperty(property_year);
		
		// 获取标准制修订文件夹
		TCComponentFolder standard = (TCComponentFolder) folder.getReferenceProperty(property_standard);
		if (standard == null) {
			standard = createFolder(session, folderType, "标准制修订", "标准制修订", year);
			folder.setReferenceProperty(property_standard, standard);
		}
		refreshStandardFolder(standard);
		
		// 获取其他流程文件夹
		TCComponentFolder other = (TCComponentFolder) folder.getReferenceProperty(property_other);
		if (other == null) {
			other = createFolder(session, folderType, "其他流程", "其他流程", year);
			folder.setReferenceProperty(property_other, other);
		}
		refreshOtherFolder(other);
		
		// 获取年份的更多文件夹
		TCComponentFolder project_more = (TCComponentFolder) folder.getReferenceProperty(property_more);
		if (project_more == null|| !"更多".equals(project_more.getProperty(property_type))) {
			project_more = createFolder(session, folderType, "更多", "更多", null);
			project_more.setReferenceProperty(property_parent, folder);
			folder.setReferenceProperty(property_more, project_more);
		}
				
		// 更新更多文件夹内容
		TCComponent[] default_projects = folder.getReferenceListProperty(property_contents);
		TCComponent[] more_projects = folder.getReferenceListProperty(property_contents);
		TCComponent[] dates = concatComs(default_projects, more_projects);
		Map<String, TCComponentFolder> project_folders = new HashMap<>();
		for (TCComponent dateCom : dates) {
			String type =dateCom.getProperty(property_type);
			if ("项目".equals(type)) {
				String name = dateCom.getProperty("object_name"); // 项目名称
				project_folders.put(name, (TCComponentFolder) dateCom);
			}
		}
		
		String[] enters = new String[] {"创建时间晚于", "创建时间早于"};
		String[] values = new String[] {year + "-1-01 00:00", (year + 1) + "-1-01 00:00"};
		List<TCComponent> projects = queryProjects(enters, values);
		Integer project_defult_num = getPreferenceIntegerValue(PROJECT_DEFAULT_MUN, 3);
		Integer project_more_num = getPreferenceIntegerValue(PROJECT_MORE_MUN, 10);
		if(project_defult_num > projects.size()){
			project_defult_num = projects.size();
		}
		int all_projects_num = projects.size();
		if (projects.size() > project_more_num + project_defult_num) {
			all_projects_num = project_more_num + project_defult_num;
		}
		project_more_num = all_projects_num - project_defult_num;
		
		TCComponentFolder[] all_projects = new TCComponentFolder[all_projects_num];
		TCComponentFolder projectFolder = null;
		for (int i = 0; i < all_projects.length; i++) {
			TCComponent project = projects.get(i);
			String name = project.getProperty("project_name");
			projectFolder = project_folders.get(name);
			if (projectFolder == null) {
				projectFolder = createFolder(session, folderType, name,"项目", null);
				projectFolder.setIntProperty(property_year, year);
			}
			all_projects[i] = projectFolder;
		}
		// 存储默认显示项目文件夹数据
		TCComponentFolder[] contents = new TCComponentFolder[project_defult_num];
		for (int i = 0; i < contents.length; i++) {
			contents[i] = all_projects[i];
		}
		folder.setRelated(property_contents,contents);
		// 	存储更多项目文件夹数据	
		more_projects = new TCComponent[project_more_num];
		for (int i = 0; i < more_projects.length; i++) {
			more_projects[i] = all_projects[i + project_defult_num];
		}
		project_more.setRelated(property_contents,more_projects);

		for (int i = 0; i < more_projects.length; i++) {
			refreshProjectFolder(all_projects[i]);
		}
				
	}
	
	// 获取首选项值
	public Integer getPreferenceIntegerValue(String preference_name, int defaultValue) throws TCException {
		Integer value = preferenceService.getIntegerValue(preference_name);
		if(value == null) {
			preferenceService.setIntegerValueAtLocation(preference_name, defaultValue, TCPreferenceLocation.USER_LOCATION);
			value = preferenceService.getIntegerValue(preference_name);
		}
		return value;
	}
	
	// 刷新今天文件夹数据
	public void refreshTodayFolder(TCComponentFolder folder) throws TCException {
		System.out.println("今天数据更新[" + folder + "]");
		Date date = new Date();
		calendar.setTime(date);
		Date start = calendar.getTime();
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		Date end = calendar.getTime();
		String[] enters = new String[]{"执行者ID","类型","结束时间起","结束时间止"};
		String[] values = new String[]{user_id, task_types, getDateString(start) + " 00:00", getDateString(end) + " 00:00"};
		List<TCComponent> tasks = queryCommonTasks(enters, values);
		TCComponent[] coms = new TCComponent[tasks.size()];
		tasks.toArray(coms);
		folder.setRelated(property_contents, coms);
	}
	
	// 刷新项目文件夹数据
	public void refreshProjectFolder(TCComponentFolder folder) throws TCException {
		System.out.println("项目数据更新[" + folder + "]");
		String project_name = folder.getProperty("object_name");
		String[] enters = new String[]{"类型","任务所属项目名称","状态值"};
		String[] values = new String[]{task_types ,project_name,"8"};
		List<TCComponent> tasks = queryCommonTasks(enters, values);
		refreshTasksFolder(folder, tasks);
	}
	
	// 刷新标准制修订文件夹数据
	public void refreshStandardFolder(TCComponentFolder folder) throws TCException {
		System.out.println("标准制修订数据更新[" + folder + "]");
		int year = folder.getIntProperty(property_year);
		String[] enters = new String[]{"执行者ID","类型","结束时间起","结束时间止","模板名称"};
		String[] values = new String[]{user_id,task_types, year + "-1-01 00:00", (year + 1) + "-1-01 00:00","标准制修订流程;标准废除流程"};
		List<TCComponent> tasks = queryCommonTasks(enters, values);
		refreshTasksFolder(folder, tasks);
	}
	
	// 刷新其他流程文件夹数据
	public void refreshOtherFolder(TCComponentFolder folder) throws TCException {
		System.out.println("其他流程数据更新[" + folder + "]");
		int year = folder.getIntProperty(property_year);
		String[] enters = new String[]{"执行者ID","类型","结束时间起","结束时间止","非标准流程","非产品设计项目流程","非技术研究项目流程"};
		String[] values = new String[]{user_id,task_types,year + "-1-01 00:00", (year + 1) + "-1-01 00:00","标准制修订流程","产品设计项目*","技术项目*"};
		List<TCComponent> tasks = queryOtherTasks(enters, values);
		refreshTasksFolder(folder, tasks);
	}
	
	// 刷新流程数据文件夹
	public void refreshTasksFolder(TCComponentFolder folder, List<TCComponent> tasks) throws TCException {
		if(tasks == null || tasks.size() == 0) {
			return;
		}
		Integer task_defult_num = getPreferenceIntegerValue(TASK_DEFAULT_MUN, 3);
		Integer task_more_num = getPreferenceIntegerValue(TASK_MORE_MUN, 10);
		if(task_defult_num > tasks.size()) {
			task_defult_num = tasks.size();
		}
		int all_task_num = tasks.size();
		if(all_task_num > task_more_num + task_defult_num) {
			all_task_num = task_more_num + task_defult_num;
		}
		task_more_num = all_task_num - task_defult_num;
		
		TCComponent more = folder.getReferenceProperty(property_more);
		if (more == null || !"更多".equals(more.getProperty(property_type))) {
			more = createFolder(session, folderType, "更多", "更多", null);
			more.setReferenceProperty(property_parent, folder);
			folder.setReferenceProperty(property_more, more);
		}
		
		TCComponent[] contents = new TCComponent[task_defult_num];
		for (int i = 0; i < contents.length; i++) {
			contents[i] = tasks.get(i);
		}
		folder.setRelated(property_contents, contents);
		
		TCComponent[] tasks_more = new TCComponent[task_more_num];
		for (int i = 0; i < tasks_more.length; i++) {
			tasks_more[i] = tasks.get(i + task_defult_num);
		}
		more.setRelated(property_contents, tasks_more);
	}
	
	// 刷新更多文件夹数据
	public void refreshMoreFolder(TCComponentFolder more) throws TCException {
		TCComponent folder = more.getReferenceProperty(property_parent);
		if (folder != null && folder instanceof TCComponentFolder) {
			refreshFolder((TCComponentFolder) folder);
		}
	}
	public void sysoutQueryInput(String[] enters, String[] values) {
		String msg = "";
		for(int i = 0; i < enters.length; i++){
			msg += enters[i] + "=" +values[i] + ";";
		}
		System.out.println(msg);
		
	}
	
	// 查询项目
	public List<TCComponent> queryProjects(String[] enters, String[] values) throws TCException {
		try {
			sysoutQueryInput(enters, values);
			TCComponent[] projects = session.search("生益_项目查询", enters, values);
			List<TCComponent> coms = new ArrayList<>();
			for(TCComponent project : projects){
				coms.add(project);
			}
			if(coms.size() > 0){
				Collections.sort(coms, new SortProject());
			}
			return coms;
		} catch (Exception e) {
			throw new TCException(e);
		}
	}
	
	// 查询其他流程任务
	public List<TCComponent> queryOtherTasks(String[] enters, String[] values) throws TCException {
		try {
			sysoutQueryInput(enters, values);
			TCComponent[] tasks = session.search("生益_其它类流程任务查询", enters, values);
			List<TCComponent> coms = new ArrayList<>();
			for(TCComponent task : tasks){
				coms.add(task);
			}
			if(coms.size() > 0){
				Collections.sort(coms, new SortTask());
			}
			return coms;
		} catch (Exception e) {
			throw new TCException(e);
		}
	}
	
	// 查询标准制修订流程任务
	public List<TCComponent> queryCommonTasks(String[] enters, String[] values) throws TCException {
		try {
			sysoutQueryInput(enters, values);
			TCComponent[] tasks = session.search("生益_普通流程任务查询", enters, values);
			List<TCComponent> coms = new ArrayList<>();
			for(TCComponent task : tasks){
				coms.add(task);
			}
			if(coms.size() > 0){
				Collections.sort(coms, new SortTask());
			}
			return coms;
		} catch (Exception e) {
			throw new TCException(e);
		}
	}
	
	
	
	// 数组合并
	public TCComponent[] concatComs(TCComponent[] coms1, TCComponent[] coms2) {
		TCComponent[] coms = new TCComponent[coms1.length + coms2.length];
		for (int i = 0; i < coms1.length; i++) {
			coms[i] = coms1[i];
		}
		for (int i = 0; i < coms2.length; i++) {
			coms[coms1.length + i] = coms2[i];
		}
		return coms;
	}
	
	// 创建文件夹
	public TCComponentFolder createFolder(TCSession session,String folderType, String folderName, String type, Integer year) throws TCException {
		try {
			DataManagementService dm = DataManagementService.getService(session.getSoaConnection());
			CreateIn folderDef = new CreateIn();
			folderDef.data.boName = folderType;
			folderDef.data.stringProps.put("object_name", folderName);
			folderDef.data.stringProps.put("object_desc", "");
			folderDef.data.stringProps.put(property_type, type);
			if(year != null) {
				folderDef.data.intProps.put(property_year, BigInteger.valueOf(year));
			}
			
			CreateIn createin[] = new CreateIn[1];
			createin[0] = folderDef;
			CreateResponse response = dm.createObjects(createin);
			if (response == null || response.serviceData.sizeOfPartialErrors() > 0) {
				throw new TCException(response.serviceData.getPartialError(0).getMessages()[0]);
			}
			TCComponent component = (TCComponent) response.serviceData.getCreatedObject(0);
			return (TCComponentFolder)component;
		} catch (Exception e) {
			throw new TCException(e);
		}
	}
	
}
