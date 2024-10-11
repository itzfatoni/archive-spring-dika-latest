package com.juaracoding.service;

import com.juaracoding.config.OtherConfig;
import com.juaracoding.core.IService;
import com.juaracoding.dto.validasi.ValMenuDTO;
import com.juaracoding.model.Menu;
import com.juaracoding.repo.MenuRepo;
import com.juaracoding.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 *  Platform Code : 002
 *  Modul Code : 002
 *  FV -> Failed Validation
 *  FE -> Failed Error
 */
@Service
public class MenuService implements IService<Menu> {

    @Autowired
    private MenuRepo menuRepo;

    @Autowired
    private ModelMapper modelMapper ;

    @Autowired
    private TransformToDTO transformToDTO;
    private StringBuilder sBuild = new StringBuilder();


    @Override
    public ResponseEntity<Object> save(Menu menu, HttpServletRequest request) {

        if(menu==null){
            return GlobalFunction.validasiGagal("OBJECT NULL","FV002002002",request);
        }

        try {
            menuRepo.save(menu);
        }catch (Exception e){
            LoggingFile.exceptionStringz("MenuService","save",e,OtherConfig.getFlagLogging());
            return GlobalFunction.dataGagalDisimpan("FE002002002",request);
        }

        return GlobalFunction.dataBerhasilDisimpan(request);
    }

    // localhost:8080/api/group-menu/12

    @Override
    @Transactional
    public ResponseEntity<Object> update(Long id, Menu menu, HttpServletRequest request) {
       Optional<Menu> opMenu =  menuRepo.findById(id);
       if(!opMenu.isPresent()){
           return GlobalFunction.dataTidakDitemukan(request);
       }
       try {
           Menu menuDB = opMenu.get();
           menuDB.setNama(menu.getNama());
       }catch (Exception e){
           LoggingFile.exceptionStringz("MenuService","update",e,OtherConfig.getFlagLogging());
           return GlobalFunction.dataGagalDisimpan("FE002002011",request);
       }

       return GlobalFunction.dataBerhasilDiubah(request);
    }

    @Override
    public ResponseEntity<Object> delete(Long id, HttpServletRequest request) {
        Optional<Menu> opMenu =  menuRepo.findById(id);
        if(!opMenu.isPresent()){
            return GlobalFunction.dataTidakDitemukan(request);
        }
        try {
            menuRepo.deleteById(id);
        }catch (Exception e){
            LoggingFile.exceptionStringz("MenuService","delete",e,OtherConfig.getFlagLogging());
            return GlobalFunction.dataGagalDisimpan("FE002002021",request);
        }
        return GlobalFunction.dataBerhasilDihapus(request);
    }

    @Override
    public ResponseEntity<Object> findAll(Pageable pageable, HttpServletRequest request) {
        Page<Menu> page = null;
        List<Menu> list = null;
        try{
            page = menuRepo.findAll(pageable);
            list = page.getContent();
            if(list.isEmpty()){
                return GlobalFunction.dataTidakDitemukan(request);
            }
        }catch (Exception e){
            return GlobalFunction.tidakDapatDiproses("FE002002031",request);
        }
        return transformToDTO.
                transformObject(new HashMap<>(),
                        convertToListRespMenuDTO(list), page,null,null,null ,request);
    }

    @Override
    public ResponseEntity<Object> findById(Long id, HttpServletRequest request) {
        Optional<Menu> opMenu= menuRepo.findById(id);
        if(!opMenu.isPresent()){
            return GlobalFunction.dataTidakDitemukan(request);
        }
        return GlobalFunction.dataByIdDitemukan(convertToMenuDTO(opMenu.get()),request);
    }

    @Override
    public ResponseEntity<Object> findByParam(Pageable pageable,String columnName, String value, HttpServletRequest request) {
        Page<Menu> page = null;
        List<Menu> list = null;
        try{
            switch (columnName) {
                case "nama": page = menuRepo.findByNamaContainsIgnoreCase(pageable,value);break;
                default:page = menuRepo.findAll(pageable);break;
            }
            list = page.getContent();
            if(list.isEmpty()){
                return GlobalFunction.dataTidakDitemukan(request);
            }
        }catch (Exception e){
            return GlobalFunction.tidakDapatDiproses("FE002002051",request);
        }
        return transformToDTO.
                transformObject(new HashMap<>(),
                        convertToListRespMenuDTO(list), page,columnName,value,null ,request);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> uploadDataExcel(MultipartFile multipartFile, HttpServletRequest request) {
        String message = "";
        if (!ExcelReader.hasWorkBookFormat(multipartFile)) {return GlobalFunction.contentTypeWorkBook("FV002002061",request);}

        try {
            List lt  = new ExcelReader(multipartFile.getInputStream(),"Sheet1").getDataMap();
            if(lt.isEmpty()){
                return GlobalFunction.dataWorkBookKosong("FV002002062",request);
            }
            //KARENA DATA LIST MAP<String,String> maka harus di convert menjadi Entity
            menuRepo.saveAll(convertListWorkBookToListEntity(lt,1L));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return GlobalFunction.tidakDapatDiproses("FE002002061",request);
        }
        return GlobalFunction.dataBerhasilDisimpan(request);
    }

    public List<Menu> convertListWorkBookToListEntity(List<Map<String, String>> workBookData, Long userId){
        List<Menu> list = new ArrayList<>();
        for (int i = 0; i < workBookData.size(); i++) {
            Map<String, String> map = workBookData.get(i);
            Menu menu = new Menu();
            menu.setNama(map.get("nama"));
            list.add(menu);
        }
        return list;
    }

    @Override
    public void downloadReportExcel(String filterBy, String value, HttpServletRequest request, HttpServletResponse response) {
        List<Menu> menuList = null;
        switch (filterBy){
            case "name": menuList = menuRepo.findByNamaContainsIgnoreCase(value);break;
            default:menuList = menuRepo.findAll();break;
        }
        List<com.juaracoding.dto.response.RespMenuDTO> listRespMenu = convertToListRespMenuDTO(menuList);
        if (listRespMenu.isEmpty()) {
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
        String[][] strBody = new String[listRespMenu.size()][strHeaderArr.length];
        String strIdMenu = "";// VARIABLE UNTUK MEMFILTER DATA NYA TERLEBIH DAHULU
        String strNamaMenu = "";// VARIABLE UNTUK MEMFILTER DATA NYA TERLEBIH DAHULU
        for (int i = 0; i < listRespMenu.size(); i++) {
            strIdMenu = listRespMenu.get(i).getId() == null ? "-" : String.valueOf(listRespMenu.get(i).getId());//null handling
            strNamaMenu = listRespMenu.get(i).getNama() == null ? "-" : listRespMenu.get(i).getNama();//null handling
            strBody[i][0] = strIdMenu;
            strBody[i][1] = strNamaMenu;
        }
        new ExcelWriter(strBody, strHeaderArr,"sheet-1", response);
    }

    public com.juaracoding.dto.response.RespMenuDTO convertToMenuDTO(Menu groupMenu){
        return modelMapper.map(groupMenu, com.juaracoding.dto.response.RespMenuDTO.class);
    }

    public Menu convertToEntity(ValMenuDTO groupMenuDTO){
        return modelMapper.map(groupMenuDTO, Menu.class);
    }

    public List<com.juaracoding.dto.response.RespMenuDTO> convertToListRespMenuDTO(List<Menu> groupMenuList){
        return modelMapper.map(groupMenuList,new TypeToken<List<com.juaracoding.dto.response.RespMenuDTO>>(){}.getType());
    }
}