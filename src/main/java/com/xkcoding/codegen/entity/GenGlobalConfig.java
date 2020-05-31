package com.xkcoding.codegen.entity;


import cn.hutool.core.util.StrUtil;
import lombok.*;

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
    private String selectQueryStr;
    private String insertQueryStr;
    private String updateQueryStr;
    private String deleteQueryStr;

    /**
     * 搜索条件
     */
    private List<ColumnEntity>  whereColumns=new ArrayList<>();

    /**
     * 更新字段
     */
    private List<ColumnEntity>  updateColumns=new ArrayList<>();

    /**
     * 插入字段
     */
    private List<ColumnEntity>  insertColumns=new ArrayList<>();

    private String[]  ignoreUpdateColumnStr={"",""};

    public  GenGlobalConfig init()
    {
        mapperName=tableEntity.getLowerClassName()+"Mapper";
        selectQueryStr=" Select"+tableEntity.getCaseClassName()+"Query";
        insertQueryStr=" Insert"+tableEntity.getCaseClassName()+"Query";
        updateQueryStr=" Update"+tableEntity.getCaseClassName()+"Query";
        deleteQueryStr=" Delete"+tableEntity.getCaseClassName()+"Query";
        var columns=tableEntity.getColumns();
        whereColumns=new ArrayList<>();
        for(var item: columns){
            if(item.getDataType().equals("int"))
            {
                whereColumns.add(item);
            }
            if(item.getDataType().equals("char"))
            {
                whereColumns.add(item);
            }
            if(item.getDataType().equals("varchar"))
            {
                whereColumns.add(item);
            }
        }

        List<String> ignoreUpdateColumnStr= Arrays.asList(new String[]{"is_deleted", "build_time","update_time"});
        updateColumns=new ArrayList<>();
        for(var item: columns){

                if(!hasVaule(ignoreUpdateColumnStr,item.getColumnName()))
                {
                    updateColumns.add(item);
                }
        }
        List<String> ignoreInsertColumnStr= Arrays.asList(new String[]{"id"});
        insertColumns=new ArrayList<>();
        for(var item: columns){

            if(!hasVaule(ignoreUpdateColumnStr,item.getColumnName())&&!hasVaule(ignoreInsertColumnStr,item.getColumnName()))
            {
                insertColumns.add(item);
            }
        }
        return this;
    }

    public boolean hasVaule(List<String> arr,String str)
    {
        for(var item: arr){

            if(item.toLowerCase().equals(str.toLowerCase()))
            {
               return  true;
            }

        }
        return  false;
    }
    //
//    private String DoName;
//    private String caseDoName;
//    private String NewClassName;
//    private List<TableEntity>  slaves=new ArrayList<>();


}
