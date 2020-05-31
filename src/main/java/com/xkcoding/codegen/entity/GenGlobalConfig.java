package com.xkcoding.codegen.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenGlobalConfig {
    private GenConfig config=new GenConfig();;
    private TableEntity tableEntity=new TableEntity();
    private String voPackageName;
    private String mapperName;
    private String selectQuery;
    private String insertQuery;
    private String updateQuery;
    private String deleteQuery;

    public  GenGlobalConfig init()
    {
        mapperName=tableEntity.getLowerClassName()+"Mapper";
        selectQuery=" Select"+tableEntity.getCaseClassName()+"Query";
        insertQuery=" Select"+tableEntity.getCaseClassName()+"Query";
        updateQuery=" Select"+tableEntity.getCaseClassName()+"Query";
        deleteQuery=" Select"+tableEntity.getCaseClassName()+"Query";
        return this;
    }
    //
//    private String DoName;
//    private String caseDoName;
//    private String NewClassName;
//    private List<TableEntity>  slaves=new ArrayList<>();


}
