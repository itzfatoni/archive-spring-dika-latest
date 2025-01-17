package com.juaracoding.service;

import com.juaracoding.config.OtherConfig;
import com.juaracoding.core.IService;
import com.juaracoding.dto.report.ReportAksesDTO;
import com.juaracoding.dto.response.RespAksesDTO;
import com.juaracoding.dto.response.RespAksesDTO;
import com.juaracoding.dto.response.RespMenuDTO;
import com.juaracoding.dto.validasi.ValAksesDTO;
import com.juaracoding.model.Akses;
import com.juaracoding.model.Akses;
import com.juaracoding.model.Menu;
import com.juaracoding.repo.AksesRepo;
import com.juaracoding.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.apache.commons.io.FileUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  Platform Code : 001
 *  Modul Code : 003
 */
@Service
public class AksesService implements IService<Akses> {

    @Autowired
    private AksesRepo aksesRepo;

    @Autowired
    private ModelMapper modelMapper ;

    @Autowired
    private TransformToDTO transformToDTO;

    @Autowired
    private PdfGenerator pdfGenerator;

    @Autowired
    private SpringTemplateEngine springTemplateEngine;
    
    private StringBuilder sBuild = new StringBuilder();


    @Override
    public ResponseEntity<Object> save(Akses menu, HttpServletRequest request) {
        if(menu==null){
            return GlobalFunction.validasiGagal("OBJECT NULL","FV001003002",request);
        }
        try {
            aksesRepo.save(menu);
        }catch (Exception e){
            LoggingFile.exceptionStringz("AksesService","save",e,OtherConfig.getFlagLogging());
            return GlobalFunction.dataGagalDisimpan("FE001003002",request);
        }
        return GlobalFunction.dataBerhasilDisimpan(request);
    }

    // localhost:8080/api/group-menu/12

    @Override
    @Transactional
    public ResponseEntity<Object> update(Long id, Akses akses, HttpServletRequest request) {
       Optional<Akses> opAkses =  aksesRepo.findById(id);
       if(!opAkses.isPresent()){
           return GlobalFunction.dataTidakDitemukan(request);
       }
       try {
           Akses aksesDB = opAkses.get();
           aksesDB.setNama(akses.getNama());
           aksesDB.setMenuList(akses.getMenuList());
       }catch (Exception e){
           LoggingFile.exceptionStringz("AksesService","update",e,OtherConfig.getFlagLogging());
           return GlobalFunction.dataGagalDiubah("FE001003011",request);
       }

       return GlobalFunction.dataBerhasilDiubah(request);
    }

    @Override
    public ResponseEntity<Object> delete(Long id, HttpServletRequest request) {
        Optional<Akses> opAkses =  aksesRepo.findById(id);
        if(!opAkses.isPresent()){
            return GlobalFunction.dataTidakDitemukan(request);
        }
        try {
            aksesRepo.deleteById(id);
        }catch (Exception e){
            LoggingFile.exceptionStringz("AksesService","delete",e,OtherConfig.getFlagLogging());
            return GlobalFunction.dataGagalDihapus("FE001003021",request);
        }
        return GlobalFunction.dataBerhasilDihapus(request);
    }

    @Override
    public ResponseEntity<Object> findAll(Pageable pageable, HttpServletRequest request) {
        Page<Akses> page = null;
        List<Akses> list = null;
        try{
            page = aksesRepo.findAll(pageable);
            list = page.getContent();
            if(list.isEmpty()){
                return GlobalFunction.dataTidakDitemukan(request);
            }
        }catch (Exception e){
            return GlobalFunction.tidakDapatDiproses("FE001003031",request);
        }
        return transformToDTO.
                transformObject(new HashMap<>(),
                        convertToListRespAksesDTO(list), page,null,null,null ,request);
    }

    @Override
    public ResponseEntity<Object> findById(Long id, HttpServletRequest request) {
        Optional<Akses> opAkses= aksesRepo.findById(id);
        if(!opAkses.isPresent()){
            return GlobalFunction.dataTidakDitemukan(request);
        }
        return GlobalFunction.dataByIdDitemukan(convertToAksesDTO(opAkses.get()),request);
    }

    @Override
    public ResponseEntity<Object> findByParam(Pageable pageable,String columnName, String value, HttpServletRequest request) {
        Page<Akses> page = null;
        List<Akses> list = null;
        try{
            switch (columnName) {
                case "nama": page = aksesRepo.findByNamaContainingIgnoreCase(pageable,value);break;
                default:page = aksesRepo.findAll(pageable);break;
            }
            list = page.getContent();
            if(list.isEmpty()){
                return GlobalFunction.dataTidakDitemukan(request);
            }
        }catch (Exception e){
            return GlobalFunction.tidakDapatDiproses("FE001003051",request);
        }
        return transformToDTO.
                transformObject(new HashMap<>(),
                        convertToListRespAksesDTO(list), page,columnName,value,null ,request);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> uploadDataExcel(MultipartFile multipartFile, HttpServletRequest request) {
        String message = "";
        if (!ExcelReader.hasWorkBookFormat(multipartFile)) {return GlobalFunction.contentTypeWorkBook("FV001003061",request);}

        try {
            List lt  = new ExcelReader(multipartFile.getInputStream(),"Sheet1").getDataMap();
            if(lt.isEmpty()){
                return GlobalFunction.dataWorkBookKosong("FV001003062",request);
            }
            //KARENA DATA LIST MAP<String,String> maka harus di convert menjadi Entity
            aksesRepo.saveAll(convertListWorkBookToListEntity(lt,1L));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return GlobalFunction.tidakDapatDiproses("FE001003061",request);
        }
        return GlobalFunction.dataBerhasilDisimpan(request);
    }

    public List<Akses> convertListWorkBookToListEntity(List<Map<String, String>> workBookData, Long userId){
        List<Akses> list = new ArrayList<>();
        for (int i = 0; i < workBookData.size(); i++) {
            Map<String, String> map = workBookData.get(i);
            Akses akses = new Akses();
            akses.setNama(map.get("nama"));
            list.add(akses);
        }
        return list;
    }

    @Override
    public void downloadReportExcel(String filterBy, String value, HttpServletRequest request, HttpServletResponse response) {
        List<Akses> menuList = null;
        switch (filterBy){
            case "nama": menuList = aksesRepo.findByNamaContainingIgnoreCase(value);break;
            default:menuList = aksesRepo.findAll();break;
        }
        List<RespAksesDTO> listRespAkses = convertToListRespAksesDTO(menuList);
        if (listRespAkses.isEmpty()) {
            GlobalFunction.manualResponse(response,GlobalFunction.dataTidakDitemukan(request));
            return;
        }
        sBuild.setLength(0);
        String headerKey = "Content-Disposition";
        sBuild.setLength(0);

        String headerValue = sBuild.append(OtherConfig.getHowToDownloadReport()).append("; filename=menu_").
                append( new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss.SSS").format(new Date())).//audit trails lewat nama file nya
                        append(".xlsx").toString();//buat excel
        response.setHeader(headerKey, headerValue);
        response.setContentType("application/octet-stream");

        String [] strHeaderArr = {"ID","NAMA MENU"};
        String[][] strBody = new String[listRespAkses.size()][strHeaderArr.length];
        String strIdAkses = "";// VARIABLE UNTUK MEMFILTER DATA NYA TERLEBIH DAHULU
        String strNamaAkses = "";// VARIABLE UNTUK MEMFILTER DATA NYA TERLEBIH DAHULU
        for (int i = 0; i < listRespAkses.size(); i++) {
            strIdAkses = listRespAkses.get(i).getId() == null ? "-" : String.valueOf(listRespAkses.get(i).getId());//null handling
            strNamaAkses = listRespAkses.get(i).getNama() == null ? "-" : listRespAkses.get(i).getNama();//null handling
            strBody[i][0] = strIdAkses;
            strBody[i][1] = strNamaAkses;
        }
        new ExcelWriter(strBody, strHeaderArr,"sheet-1", response);
    }

    /**
     * Untuk kedepan nya
     * jika ada Query dengan data yang banyak dan ingin diimport via pdf
     * maka batasi data tersebut minimal 5000 untuk ukuran 10 kolom
     * kalau sampai lewat 15 kolom kurangi lagi agar proses nya tidak membebani server
     *
     * @param column
     * @param value
     * @param request
     * @param response
     */
    public void generateToPDF(String column, String value, HttpServletRequest request, HttpServletResponse response){
        Map<String,Object> payloadJwt = GlobalFunction.claimsTokenBody(request);
        List<Akses> aksesList = null;
        switch (column){
            /** model penulisan untuk membatasi data pada function generateToPDF adalah sbb :
             * case "alamat": userList = userRepo.findByNamaLengkapContainingIgnoreCase(value,pageableDefault).getContent();break;
             * pageableDefault sudah di declare contoh nya di deklarasi public variable, untuk membatasi data minimal 5000 data yang akan tercetak di PDF
             */
            case "nama": aksesList = aksesRepo.findByNamaContainingIgnoreCase(value);break;
            default:aksesList = aksesRepo.findAll();break;
        }
        List<ReportAksesDTO> listReportAkses = convertToReportAksesDTO(aksesList);
        if (listReportAkses.isEmpty()) {
            GlobalFunction.manualResponse(response,GlobalFunction.dataTidakDitemukan(request));
            return;
        }
        Map<String,Object> map = new HashMap<>();
        String strHtml = null;
        Context context = new Context();
        Map<String,Object> mapColumnName = GlobalFunction.convertClassToObject(new ReportAksesDTO());
        List<String> listTampungSebentar = new ArrayList<>();
        List<String> listHelper = new ArrayList<>();
        for (Map.Entry<String,Object> entry : mapColumnName.entrySet()) {
            listTampungSebentar.add(GlobalFunction.camelToStandar(entry.getKey()));
            listHelper.add(entry.getKey());
        }
        Map<String,Object> mapTampung = null;
        List<Map<String,Object>> listMap = new ArrayList<>();
        for (int i = 0; i < listReportAkses.size(); i++) {
            mapTampung = GlobalFunction.convertClassToObject(listReportAkses.get(i),null);
            listMap.add(mapTampung);
        }
        map.put("listKolom",listTampungSebentar);
        map.put("listContent",listMap);
        map.put("listHelper",listHelper);
        map.put("timestamp",GlobalFunction.formatingDateDDMMMMYYYY());
        map.put("username",payloadJwt.get("namaLengkap"));
        map.put("totalData",listReportAkses.size());
        map.put("title","REPORT AKSES");
        context.setVariables(map);
        strHtml = springTemplateEngine.process("global-report",context);
        pdfGenerator.htmlToPdf(strHtml,"akses",response);
    }

    public void generateToPDFManual(String column, String value, HttpServletRequest request, HttpServletResponse response){
        Map<String,Object> payloadJwt = GlobalFunction.claimsTokenBody(request);
        List<Akses> aksesList = null;
        Context context = new Context();
        switch (column){
            case "nama": aksesList = aksesRepo.findByNamaContainingIgnoreCase(value);break;
            default:aksesList = aksesRepo.findAll();break;
        }
        List<ReportAksesDTO> listReportAkses = convertToReportAksesDTO(aksesList);
        if (listReportAkses.isEmpty()) {
            GlobalFunction.manualResponse(response,GlobalFunction.dataTidakDitemukan(request));
            return;
        }
        Map<String,Object> map = new HashMap<>();
        String strHtml = null;
        map.put("listContent",listReportAkses);
        map.put("timestamp",GlobalFunction.formatingDateDDMMMMYYYY());
        map.put("username",payloadJwt.get("namaLengkap"));
        map.put("totalData",listReportAkses.size());
        map.put("title","REPORT AKSES");
        context.setVariables(map);
        strHtml = springTemplateEngine.process("akses/aksesreport",context);
        pdfGenerator.htmlToPdf(strHtml,"akses",response);
    }

    public RespAksesDTO convertToAksesDTO(Akses groupAkses){
        return modelMapper.map(groupAkses, RespAksesDTO.class);
    }

    /** khusus mapping untuk report */
    public List<ReportAksesDTO> convertToReportAksesDTO(List<Akses> listAkses){
        return modelMapper.map(listAkses, new TypeToken<List<ReportAksesDTO>>(){}.getType());
    }

    public Akses convertToEntity(ValAksesDTO groupAksesDTO){
        return modelMapper.map(groupAksesDTO, Akses.class);
    }

    public List<RespAksesDTO> convertToListRespAksesDTO(List<Akses> groupAksesList){
        return modelMapper.map(groupAksesList,new TypeToken<List<RespAksesDTO>>(){}.getType());
    }
}