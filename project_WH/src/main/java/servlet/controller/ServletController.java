package servlet.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import egovframework.rte.psl.dataaccess.util.EgovMap;
import servlet.model.SggDTO;
import servlet.service.ServletService;

@Controller
public class ServletController {
	@Resource(name = "ServletService")
	private ServletService servletService;
	
	@RequestMapping(value = "/main.do")
	public String mainTest(ModelMap model) throws Exception {
		System.out.println("sevController.java - mainTest()");
		
		String str = servletService.addStringTest("START! ");
		model.addAttribute("resultStr", str);
		
		List<EgovMap> tl_sdList = servletService.selectList();
		model.addAttribute("tl_sdList", tl_sdList);
		
		return "main/main";
	}
	
	@RequestMapping(value = "testPage.do")
	public String map() throws Exception {
		System.out.println("testPage Controller 진입");
		return "test/testPage";
	}
	
	// 탄소지도 메뉴 페이지 호출 controller
	@RequestMapping(value = "position.do")
	public String position(Model model) throws Exception {
		System.out.println("position Controller 진입");
		
		List<SggDTO> sd_nm_list = servletService.sd_nm_list();			// 시도 이름 반환
		
		model.addAttribute("sd_nm_list", sd_nm_list);
		
		return "classification/position";
	}
	
	// 데이터 업로드 메뉴 페이지 호출 클릭 controller
	@RequestMapping(value = "upload.do")
	public String upload() throws Exception {
		System.out.println("upload Controller 진입");
		return "classification/upload";
	}
	
	// 통계 메뉴 페이지 호출 controller
	@RequestMapping(value = "statistics.do")
	public String statistics(Model model) throws Exception {
		System.out.println("statistics Controller 진입");
		
		List<SggDTO> sd_nm_list = servletService.sd_nm_list();
		
		model.addAttribute("sd_nm_list", sd_nm_list);
		
		return "classification/statistics";
	}
	
	
	// 시도이름으로 시군구 이름 리턴
	@RequestMapping(value = "sgg_list.do")
	@ResponseBody
	public List<SggDTO> sgg_list(SggDTO sgg){
		System.out.println("sgg_list.do 진입");
		System.out.println("sd_nm(Controller):"+sgg.getSd_nm());
		
		String sd_nm = sgg.getSd_nm();
		List<SggDTO> sgg_list = servletService.get_sgg_nm(sd_nm);
		
		System.out.println("sgg_list:"+sgg_list);
		
		return sgg_list;
	}

	
	// sgg 코드네임을 산출해서 geoserver 출력
	@RequestMapping(value = "return_sgg_cd.do")
	public String return_sgg_cd(SggDTO sd, Model model) {
		System.out.println("return_sgg_cd 진입");
		System.out.println("legend_type:"+sd.getLegend_type());
		String legend_type = sd.getLegend_type();									// legend 유형
		
		DecimalFormat df = new DecimalFormat();
		
		String sgg_nm = sd.getSgg_nm();
		
		SggDTO return_value = servletService.return_sgg_cd(sgg_nm);					// sgg_cd 반환
		System.out.println("sgg_cd:"+return_value.getSgg_cd());
		
		List<SggDTO> center = servletService.getCenter_sgg(sd);
		System.out.println("center_x:"+ center.get(0).getGps_x());
		System.out.println("center_y:"+ center.get(0).getGps_y());
		
		int bjd_cnt = servletService.return_bjd_cnt(return_value.getSgg_cd());		// bjd 갯수
		System.out.println("bjd_cnt:"+bjd_cnt);
		
		List<SggDTO> legend = servletService.get_legend_bjd(return_value);			// bjd 범례
		System.out.println("legend1:"+legend.get(0).getBjd_legend_val());
		System.out.println("legend_size:"+legend.size());
		
		List<String> cdb_jenksbins = new ArrayList<String>();
		
		if(legend_type.equals("natural")) {
			if(legend.size() > 1) {
				cdb_jenksbins.add(legend.get(0).getBjd_legend_val().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",")+" 이하");
				for(int i=0; i<legend.size(); i++) {
					int j = i + 1;
					if(j < legend.size()) {
						cdb_jenksbins.add(legend.get(i).getBjd_legend_val().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",")+" ~ "+legend.get(j).getBjd_legend_val().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
					}
				}
			}else {
			cdb_jenksbins.add(legend.get(0).getBjd_legend_val().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",")+" 이하");
			}
		}else{				// legend_type = "equal"
			int max_index = legend.size() - 1;
			int equal_num = Integer.parseInt(legend.get(max_index).getBjd_legend_val()) / 5;
			cdb_jenksbins.add("0 ~ " + df.format(equal_num));
			cdb_jenksbins.add(df.format(equal_num) + " ~ " + df.format(equal_num*2));
			cdb_jenksbins.add(df.format(equal_num*2) + " ~ " + df.format(equal_num*3));
			cdb_jenksbins.add(df.format(equal_num*3) + " ~ " + df.format(equal_num*4));
			cdb_jenksbins.add(df.format(equal_num*4) + " ~ " + legend.get(max_index).getBjd_legend_val().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
		}
		
		
		String[] colors = {"#fcd0d0", "#fab6b6", "#ff8585", "#ff4747", "#ff0000"};
		model.addAttribute("backgroundColor", colors);
		
		model.addAttribute("legend_type", legend_type);
		model.addAttribute("jenksbins", cdb_jenksbins);
		
		model.addAttribute("center_x", center.get(0).getGps_x());
		model.addAttribute("center_y", center.get(0).getGps_y());
		model.addAttribute("sgg_cd", return_value.getSgg_cd());
		
		return "result/sgg_map";
	}
	
	// sd 코드네임을 산출해서 geoserver 출력
	@RequestMapping(value = "return_sd_cd.do")
	public String return_sd_cd(SggDTO sd, Model model) {
		System.out.println("return_sd_cd 진입");
		System.out.println("legend_type:"+sd.getLegend_type());
		
		DecimalFormat df = new DecimalFormat("###,###");
		
		String sd_nm = sd.getSd_nm();
		String legend_type = sd.getLegend_type();
		
		System.out.println("sgg_count(controller):"+sd.getSgg_count());
		
		List<SggDTO> center = servletService.getCenter(sd);
		System.out.println("center_x:"+ center.get(0).getGps_x());
		System.out.println("center_y:"+ center.get(0).getGps_y());
		
		
		
		if(sd.getSgg_count()>=5) {
			sd.setSgg_count(5);
		}else {
			sd.setSgg_count(sd.getSgg_count());
		}
		
		List<SggDTO> legend = servletService.get_legend(sd);
		System.out.println("legend1:"+legend.get(0).getVal());
		System.out.println("legend_size:"+legend.size());
		
		List<String> cdb_jenksbins = new ArrayList<String>();
		
		if(legend_type.equals("natural")) {
			if(legend.size() > 1) {
				cdb_jenksbins.add(legend.get(0).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",")+" 이하");
				for(int i=0; i<legend.size(); i++) {
					int j = i + 1;
					if(j < legend.size()) {
						cdb_jenksbins.add(legend.get(i).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",")+" ~ "+legend.get(j).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
					}
				}
			}else {
			cdb_jenksbins.add(legend.get(0).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",") + " 이하");
			}
		}else{				// legend_type = "equal"
			int max_index = legend.size() - 1;
			int equal_num = Integer.parseInt(legend.get(max_index).getVal()) / 5;
			cdb_jenksbins.add("0 ~ " + df.format(equal_num));
			cdb_jenksbins.add(df.format(equal_num) + " ~ " + df.format(equal_num*2));
			cdb_jenksbins.add(df.format(equal_num*2) + " ~ " + df.format(equal_num*3));
			cdb_jenksbins.add(df.format(equal_num*3) + " ~ " + df.format(equal_num*4));
			cdb_jenksbins.add(df.format(equal_num*4) + " ~ " + legend.get(max_index).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
		}
		
		String[] colors = {"#fcd0d0", "#fab6b6", "#ff8585", "#ff4747", "#ff0000"};
		model.addAttribute("backgroundColor", colors);
		
		model.addAttribute("legend_type", legend_type);
		
		model.addAttribute("jenksbins", cdb_jenksbins);
		model.addAttribute("legend", legend);
		
		model.addAttribute("center_x", center.get(0).getGps_x());
		model.addAttribute("center_y", center.get(0).getGps_y());
		model.addAttribute("sd_nm", sd_nm);
		
		return "result/sd_map";
	}
	
	
	// 시도 전체지도
	@RequestMapping(value = "position_sd_full.do")
	public String position_sd_full(SggDTO carbon, Model model) throws Exception {
		DecimalFormat df = new DecimalFormat("###,###");		// int 형의 수표현
		
		System.out.println("position_sd_full Controller 진입");
		System.out.println("legend_type:"+carbon.getLegend_type());
		
		String legend_type = carbon.getLegend_type();
		
		List<SggDTO> legend = servletService.get_legend_sd_all();
		
		List<String> cdb_jenksbins = new ArrayList<String>();
		
		if(legend_type.equals("natural")) {
			if(legend.size() > 1) {
				cdb_jenksbins.add(legend.get(0).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",")+" 이하");
				for(int i=0; i<legend.size(); i++) {
					int j = i + 1;
					if(j < legend.size()) {
						cdb_jenksbins.add(legend.get(i).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",") + " ~ " + legend.get(j).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
					}
				}
			}else {
				cdb_jenksbins.add(legend.get(0).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",")+" 이하");
			}
		}else {			// legend_type = "equal"
			int max_index = legend.size() - 1;
			long equal_num = Long.parseLong(legend.get(max_index).getVal()) / 5;
			cdb_jenksbins.add("0 ~ " + df.format(equal_num));
			cdb_jenksbins.add(df.format(equal_num) + " ~ " + df.format(equal_num*2));
			cdb_jenksbins.add(df.format(equal_num*2) + " ~ " + df.format(equal_num*3));
			cdb_jenksbins.add(df.format(equal_num*3) + " ~ " + df.format(equal_num*4));
			cdb_jenksbins.add(df.format(equal_num*4) + " ~ " + legend.get(max_index).getVal().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
			
		}
		
		String[] colors = {"#fcd0d0", "#fab6b6", "#ff8585", "#ff4747", "#ff0000"};
		model.addAttribute("backgroundColor", colors);
	
		model.addAttribute("legend_type", carbon.getLegend_type());
		model.addAttribute("jenksbins", cdb_jenksbins);
		
		return "result/position_sd_full";
	}
	
	
	// 우리나라 전체지도 (vworld)
	@RequestMapping(value = "position_full.do")
	public String position_full(Model model) throws Exception {
		System.out.println("position_full Controller 진입");
		return "result/position_full";
	}
	
	
	// 시도 통계
	@RequestMapping(value = "statistics_chart_main.do")
	public String statistics_chart_main(SggDTO region, Model model) throws Exception {
		System.out.println("statistics_chart_main Controller 진입");
		
		List<SggDTO> sd_inform = servletService.get_sd_inform();
		System.out.println("region1:"+sd_inform.get(0).getSd_nm());
		int sd_cnt = sd_inform.size();
		System.out.println("sd_cnt:"+sd_cnt);
		
		List<String> sd_nm = new ArrayList<>();
		List<String> sd_usage = new ArrayList<>();
		
		for(int i=0; i<sd_cnt; i++) {
			sd_nm.add(sd_inform.get(i).getSd_nm());
			sd_usage.add(sd_inform.get(i).getSd_usage());
		}
		
		for(int i=0; i < sd_inform.size(); i++) {
			sd_inform.get(i).setSd_usage(sd_inform.get(i).getSd_usage().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
		}

		System.out.println("sd_nm_list:"+sd_nm);
		System.out.println("sd_usage_list:"+sd_usage);
		
		model.addAttribute("sd_inform", sd_inform);
		model.addAttribute("sd_nm", sd_nm);
		model.addAttribute("sd_usage", sd_usage);
		model.addAttribute("sd_cnt", sd_cnt);
		
		return "result/chart_main";
	}
	
	
	
	// 시군구 통계
	@RequestMapping(value = "statistics_search_sd.do")
	public String statistics_search_sd(SggDTO stat, Model model) throws Exception {
		System.out.println("statistics_search_sd Controller 진입");
		
		DecimalFormat df = new DecimalFormat("###,###");
		
		String sd_nm = stat.getSd_nm();
		System.out.println("sd_nm:"+sd_nm);
		
		List<SggDTO> sgg_inform = servletService.get_sgg_inform(stat);
		System.out.println("sgg_nm1:"+sgg_inform.get(0).getSgg_nm());
		System.out.println("sgg_nm1:"+sgg_inform.get(0).getSgg_usage());
		
		// 이름(name)을 가나다순으로 정렬하는 Comparator 생성
        Comparator<SggDTO> nameComparator = Comparator.comparing(SggDTO::getSgg_nm);
        // 정렬
        Collections.sort(sgg_inform, nameComparator);
		
		int sgg_cnt = sgg_inform.size();
		System.out.println("sgg 갯수:"+sgg_inform.size());
		
		int sd_nm_length = sd_nm.length() + 1;			// 시군구 이름만 추출하기 위한 시도 이름길이
		
		List<String> sgg_nm = new ArrayList<>();
		List<String> sgg_usage = new ArrayList<>();
		
		
		for(int i=0; i<sgg_inform.size(); i++) {
			sgg_usage.add(sgg_inform.get(i).getSgg_usage());
		}
		
		
		if(sgg_inform.size()>1) {
			for(int i=0; i<sgg_inform.size(); i++) {
				sgg_inform.get(i).setSgg_nm(sgg_inform.get(i).getSgg_nm().substring(sd_nm_length));
				sgg_inform.get(i).setSgg_usage(sgg_inform.get(i).getSgg_usage().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
			}
		}else {
			sgg_inform.get(0).setSgg_nm(sgg_inform.get(0).getSgg_nm());
			sgg_inform.get(0).setSgg_usage(sgg_inform.get(0).getSgg_usage().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
		}
		
		for(int i=0; i<sgg_inform.size(); i++) {
			sgg_nm.add(sgg_inform.get(i).getSgg_nm());
		}
        
		System.out.println("sgg_nm_list:"+sgg_nm);
		System.out.println("sgg_usage_list:"+sgg_usage);
		
		model.addAttribute("sgg_cnt", sgg_cnt);
		model.addAttribute("sgg_inform", sgg_inform);	// chart2 출력
		model.addAttribute("sgg_nm", sgg_nm);			// chart1 출력 sgg_nm
		model.addAttribute("sgg_usage", sgg_usage);		// chart1 출력 sgg_usage
		
		return "result/chart_search";
	}
	
	
	@Autowired
	private ServletContext servletContext; 
	
	// 파일 업로드
	@RequestMapping(value = "geoFileUpload.do")
	@ResponseBody
	public int geoFileUpload(MultipartFile geofile, Model model, HttpServletRequest request){
		System.out.println("geoFileUpload.do 진입");
		
		if (geofile.isEmpty()) {
            System.out.println("업로드 파일 없음");
        }

        try {
            // 파일 업로드 처리
            String uploadPath = servletContext.getRealPath("/upload");
            String fileName = geofile.getOriginalFilename();
            String filePath = uploadPath + File.separator + fileName;
            System.out.println("uploadPath : " + uploadPath);
            System.out.println("fileName : " + fileName);
            System.out.println("filePath : " + filePath);
            geofile.transferTo(new File(filePath));
            String fileContent = readFileAsString(geofile);
            System.out.println("파일 내용: " + fileContent);
            

        } catch (IOException e) {
            e.printStackTrace();
        }
		return 1;
	}
	
	public String readFileAsString(MultipartFile geofile) throws IOException {
	       // 업로드된 파일의 InputStream을 가져옵니다.
	       InputStream inputStream = geofile.getInputStream();

	       try {
	           // InputStream을 사용하여 파일의 내용을 String으로 변환합니다.
	           String fileContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
	           return fileContent;
	       } finally {
	           // InputStream을 닫습니다.
	           inputStream.close();
	       }
	}
}	 
