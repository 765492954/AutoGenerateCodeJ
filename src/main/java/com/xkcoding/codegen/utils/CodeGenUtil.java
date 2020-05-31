package com.xkcoding.codegen.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.setting.dialect.Props;
import com.google.common.collect.Lists;
import com.xkcoding.codegen.constants.GenConstants;
import com.xkcoding.codegen.entity.ColumnEntity;
import com.xkcoding.codegen.entity.GenConfig;
import com.xkcoding.codegen.entity.GenGlobalConfig;
import com.xkcoding.codegen.entity.TableEntity;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.text.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <p>
 * 代码生成器   工具类
 * </p>
 *
 * @package: com.xkcoding.codegen.utils
 * @description: 代码生成器   工具类
 * @author: yangkai.shen
 * @date: Created in 2019-03-22 09:27
 * @copyright: Copyright (c) 2019
 * @version: V1.0
 * @modified: yangkai.shen
 */
@Slf4j
@UtilityClass
public class CodeGenUtil {

    private final String ENTITY_JAVA_VM = "Entity.java.vm";
    private final String MAPPER_JAVA_VM = "Mapper.java.vm";
    private final String SERVICE_JAVA_VM = "Service.java.vm";
    private final String SERVICE_IMPL_JAVA_VM = "ServiceImpl.java.vm";
    private final String CONTROLLER_JAVA_VM = "Controller.java.vm";
    private final String MAPPER_XML_VM = "Mapper.xml.vm";
    private final String API_JS_VM = "api.js.vm";
    private final String CONTROLLER_JAVA_DE = "DeleteVo.java.vm";
    private final String CONTROLLER_JAVA_SE = "SelectVo.java.vm";
    private final String CONTROLLER_JAVA_IN = "InsertVo.java.vm";
    private final String CONTROLLER_JAVA_UP = "UpdateVo.java.vm";
    private final String CONTROLLER_JAVA_RE = "ResultVo.java.vm";
    private final String MAPPER_JAVA_VM_EXT = "MapperExt.java.vm";
    private final String MAPPER_XML_VM_EXT  = "MapperExt.xml.vm";



    private List<String> getTemplates() {
        List<String> templates = new ArrayList<>();
      //  templates.add("template/Entity.java.vm");
//        templates.add("template/Mapper.java.vm");
//        templates.add("template/Mapper.xml.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/ServiceImpl.java.vm");
        templates.add("template/Controller.java.vm");
        templates.add("template/DeleteVo.java.vm");
        templates.add("template/SelectVo.java.vm");
        templates.add("template/InsertVo.java.vm");
        templates.add("template/UpdateVo.java.vm");
        templates.add("template/ResultVo.java.vm");
        templates.add("template/MapperExt.java.vm");
        templates.add("template/MapperExt.xml.vm");
        //templates.add("template/api.js.vm");
        return templates;
    }

    /**
     * 生成代码
     */
    public void generatorCodeSave(GenConfig genConfig, Entity table, List<Entity> columns, ZipOutputStream zip) throws IllegalAccessException {
        //配置信息
        Props props = getConfig();
        boolean hasBigDecimal = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.getStr("tableName"));

        if (StrUtil.isNotBlank(genConfig.getComments())) {
            tableEntity.setComments(genConfig.getComments());
        } else {
            tableEntity.setComments(table.getStr("tableComment"));
        }

        String tablePrefix;
        if (StrUtil.isNotBlank(genConfig.getTablePrefix())) {
            tablePrefix = genConfig.getTablePrefix();
        } else {
            tablePrefix = props.getStr("tablePrefix");
        }

        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), tablePrefix);
        tableEntity.setCaseClassName(className);
        tableEntity.setLowerClassName(StrUtil.lowerFirst(className));

        //列信息
        List<ColumnEntity> columnList = Lists.newArrayList();
        for (Entity column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.getStr("columnName"));
            columnEntity.setDataType(column.getStr("dataType"));
            columnEntity.setComments(column.getStr("columnComment"));
            columnEntity.setExtra(column.getStr("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());

            attrName=lowerLast(attrName);

            columnEntity.setCaseAttrName(attrName);
            columnEntity.setLowerAttrName(StrUtil.lowerFirst(attrName));
            columnEntity.setUperAttrName(StrUtil.upperFirst(attrName));

            //列的数据类型，转换成Java类型
            String attrType = props.getStr(columnEntity.getDataType(), "unknownType");
            columnEntity.setAttrType(attrType);

            //判断是否包含IsDeleted

            if(attrName.toLowerCase().equals("IsDeleted".toLowerCase()))
            {
                columnEntity.setAttrType("Integer");
            }

            if (!hasBigDecimal && "BigDecimal".equals(attrType)) {
                hasBigDecimal = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.getStr("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columnList.add(columnEntity);
        }
        tableEntity.setColumns(columnList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        //封装模板数据
        Map<String, Object> map = new HashMap<>(16);
        map.put("tableName", tableEntity.getTableName());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getCaseClassName());
        map.put("classname", tableEntity.getLowerClassName());
        map.put("pathName", tableEntity.getLowerClassName().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("datetime", DateUtil.now());
        map.put("year", DateUtil.year(new Date()));

        if(StrUtil.isBlank(genConfig.getRequest().getVoPackageName()))
        {
            genConfig.getRequest().setVoPackageName(tableEntity.getLowerClassName().toLowerCase());
        }

        if (StrUtil.isNotBlank(genConfig.getComments())) {
            map.put("comments", genConfig.getComments());
        } else {
            map.put("comments", tableEntity.getComments());
        }

        if (StrUtil.isNotBlank(genConfig.getAuthor())) {
            map.put("author", genConfig.getAuthor());
        } else {
            map.put("author", props.getStr("author"));
        }

        if (StrUtil.isNotBlank(genConfig.getModuleName())) {
            map.put("moduleName", genConfig.getModuleName());
        } else {
            map.put("moduleName", props.getStr("moduleName"));
            genConfig.setModuleName(props.getStr("moduleName"));
        }

        if (StrUtil.isNotBlank(genConfig.getPackageName())) {
            map.put("package", genConfig.getPackageName());
            map.put("mainPath", genConfig.getPackageName());
        } else {
            map.put("package", props.getStr("package"));
            map.put("mainPath", props.getStr("mainPath"));
            genConfig.setPackageName(props.getStr("package"));
        }

        GenGlobalConfig priConfig=GenGlobalConfig.builder()
                .config(genConfig)
                .tableEntity(tableEntity)
                .voPackageName(genConfig.getRequest().getVoPackageName())
                .build().init();
        mapAddObj(map,priConfig);


        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            if (StrUtil.isBlank(template)) {
               break;
            }
            var fileName=getFileName(template,genConfig );
            if (StrUtil.isBlank(fileName)) {
                break;
            }
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, CharsetUtil.UTF_8);
            tpl.merge(context, sw);

            BufferedWriter out = null;
            try {
               // File file = new    File("I:/genecode/1/" + getFileName(template,genConfig ));
                File file = new    File( fileName);
                // 如果文件夹不存在则创建
                if (!file.getParentFile().exists()&&!file.isDirectory()){
                    file.getParentFile().mkdirs();

                }

                OutputStream outputStream = new FileOutputStream(file);

             IoUtil.write(outputStream,true,sw.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 生成代码
     */
    public void generatorCode(GenConfig genConfig, Entity table, List<Entity> columns, ZipOutputStream zip) {
        //配置信息
        Props props = getConfig();
        boolean hasBigDecimal = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.getStr("tableName"));

        if (StrUtil.isNotBlank(genConfig.getComments())) {
            tableEntity.setComments(genConfig.getComments());
        } else {
            tableEntity.setComments(table.getStr("tableComment"));
        }

        String tablePrefix;
        if (StrUtil.isNotBlank(genConfig.getTablePrefix())) {
            tablePrefix = genConfig.getTablePrefix();
        } else {
            tablePrefix = props.getStr("tablePrefix");
        }

        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), tablePrefix);
        tableEntity.setCaseClassName(className);
        tableEntity.setLowerClassName(StrUtil.lowerFirst(className));

        //列信息
        List<ColumnEntity> columnList = Lists.newArrayList();
        for (Entity column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.getStr("columnName"));
            columnEntity.setDataType(column.getStr("dataType"));
            columnEntity.setComments(column.getStr("columnComment"));
            columnEntity.setExtra(column.getStr("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setCaseAttrName(attrName);
            columnEntity.setLowerAttrName(StrUtil.lowerFirst(attrName));

            //列的数据类型，转换成Java类型
            String attrType = props.getStr(columnEntity.getDataType(), "unknownType");
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && "BigDecimal".equals(attrType)) {
                hasBigDecimal = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.getStr("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columnList.add(columnEntity);
        }
        tableEntity.setColumns(columnList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);


        //封装模板数据
        Map<String, Object> map = new HashMap<>(16);
        map.put("tableName", tableEntity.getTableName());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getCaseClassName());
        map.put("classname", tableEntity.getLowerClassName());
        map.put("pathName", tableEntity.getLowerClassName().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("datetime", DateUtil.now());
        map.put("year", DateUtil.year(new Date()));

        if (StrUtil.isNotBlank(genConfig.getComments())) {
            map.put("comments", genConfig.getComments());
        } else {
            map.put("comments", tableEntity.getComments());
        }

        if (StrUtil.isNotBlank(genConfig.getAuthor())) {
            map.put("author", genConfig.getAuthor());
        } else {
            map.put("author", props.getStr("author"));
        }

        if (StrUtil.isNotBlank(genConfig.getModuleName())) {
            map.put("moduleName", genConfig.getModuleName());
        } else {
            map.put("moduleName", props.getStr("moduleName"));
            genConfig.setModuleName(props.getStr("moduleName"));
        }

        if (StrUtil.isNotBlank(genConfig.getPackageName())) {
            map.put("package", genConfig.getPackageName());
            map.put("mainPath", genConfig.getPackageName());
        } else {
            map.put("package", props.getStr("package"));
            map.put("mainPath", props.getStr("mainPath"));
            genConfig.setPackageName(props.getStr("package"));
        }
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, CharsetUtil.UTF_8);
            tpl.merge(context, sw);

            try {
                //添加到zip
                zip.putNextEntry(new ZipEntry(Objects.requireNonNull(getFileName2(template, tableEntity.getCaseClassName(), map
                        .get("package")
                        .toString(), map.get("moduleName").toString()))));
                IoUtil.write(zip, StandardCharsets.UTF_8, false, sw.toString());
                IoUtil.close(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    private String columnToJava(String columnName) {
        if(columnName.contains("_"))
        {
            return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
        }
        return  columnName;

    }

    /**
     * 表名转换成Java类名
     */
    private String tableToJava(String tableName, String tablePrefix) {
        if (StrUtil.isNotBlank(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    private Props getConfig() {
        Props props = new Props("generator.properties");
        props.autoLoad(true);
        return props;
    }

    /**
     * 获取文件名
     */
    private String getFileName2(String template, String className, String packageName, String moduleName) {
        // 包路径
        String packagePath = GenConstants.SIGNATURE + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        // 资源路径
        String resourcePath = GenConstants.SIGNATURE + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
        // api路径
        String apiPath = GenConstants.SIGNATURE + File.separator + "api" + File.separator;

        if (StrUtil.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
        }

        if (template.contains(ENTITY_JAVA_VM)) {
            return packagePath + "entity" + File.separator + className + ".java";
        }

        if (template.contains(CONTROLLER_JAVA_DE)) {
            return packagePath + "Vo" + File.separator + className + "Vo.java";
        }
        if (template.contains(CONTROLLER_JAVA_SE)) {
            return packagePath + "Select" + File.separator + className + "Vo.java";
        }
        if (template.contains(CONTROLLER_JAVA_IN)) {
            return packagePath + "Insert" + File.separator + className + "Vo.java";
        }
        if (template.contains(CONTROLLER_JAVA_UP)) {
            return packagePath + "Update" + File.separator + className + "Vo.java";
        }

        if (template.contains(MAPPER_JAVA_VM)) {
            return packagePath + "mapper" + File.separator + className + "Mapper.java";
        }

        if (template.contains(SERVICE_JAVA_VM)) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if (template.contains(SERVICE_IMPL_JAVA_VM)) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }

        if (template.contains(CONTROLLER_JAVA_VM)) {
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }

        if (template.contains(MAPPER_XML_VM)) {
            return resourcePath + "mapper" + File.separator + className + "Mapper.xml";
        }

        if (template.contains(API_JS_VM)) {
            return apiPath + className.toLowerCase() + ".js";
        }

        return null;
    }

    private String getFileName(String template,GenConfig config) {
        // 包路径
      //  String packagePath = config.getSavePath();
        // 资源路径
     //   String resourcePath = config.getSavePath();
        // api路径
    //    String apiPath = "";
        // 包路径
        String packagePath = config.getSavePath()+ File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        // 资源路径
        String resourcePath = config.getSavePath() + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
        // api路径
        String apiPath = config.getSavePath() + File.separator + "api" + File.separator;

        var packageName=config.getPackageName();

        var moduleName=config.getModuleName();

        if (StrUtil.isNotBlank(config.getPackageName())) {
            packagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
        }

        String className=config.getRequest().getTableName();

        String voPre="query" + File.separator+config.getRequest().getVoPackageName() + File.separator;
        String voPre2="vo" + File.separator+config.getRequest().getVoPackageName() + File.separator;

//        if (template.contains(ENTITY_JAVA_VM)) {
//            return   packagePath + "entity" + className + ".java";
//        }

        if (template.contains(CONTROLLER_JAVA_DE)) {
            return  packagePath + voPre+"Delete" +  className + "Query.java";
        }
        if (template.contains(CONTROLLER_JAVA_SE)) {
            return  packagePath + voPre+"Select" +   className + "Query.java";
        }
        if (template.contains(CONTROLLER_JAVA_IN)) {
            return   packagePath + voPre+"Insert" +   className + "Query.java";
        }
        if (template.contains(CONTROLLER_JAVA_UP)) {
            return   packagePath + voPre+"Update" +   className + "Query.java";
        }
        if (template.contains(CONTROLLER_JAVA_RE)) {
            return   packagePath + voPre2 +   className + "Vo.java";
        }

//        if (template.contains(MAPPER_JAVA_VM)) {
//            return    packagePath + className + "Mapper.java";
//        }

        if (template.contains(SERVICE_JAVA_VM)) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if (template.contains(SERVICE_IMPL_JAVA_VM)) {

            return packagePath + "service" + File.separator +  "impl" + File.separator + className + "ServiceImpl.java";
        }

        if (template.contains(CONTROLLER_JAVA_VM)) {
            return     packagePath + "controller" + File.separator + className + "Controller.java";
        }

//        if (template.contains(MAPPER_XML_VM)) {
//            return   className + "Mapper.xml";
//        }

//        if (template.contains(API_JS_VM)) {
//            return apiPath + className.toLowerCase() + ".js";
//        }

        if (template.contains(MAPPER_JAVA_VM_EXT)) {
            return packagePath + "mapper" + File.separator + className + "ExtMapper.java";
        }

        if (template.contains(MAPPER_XML_VM_EXT)) {
            return resourcePath + "mapper" + File.separator + className + "ExtMapper.xml";
        }

        return null;
    }


    /**
     * 将Object对象里面的属性和值转化成Map对象
     *
     * @param obj
     * @return
     * @throws IllegalAccessException
     */
    public static Map<String, Object> objectToMap(Object obj) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<String,Object>();
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object value = field.get(obj);
            map.put(fieldName, value);
        }
        return map;
    }


    /**
     * 将Object对象里面的属性和值转化成Map对象
     *
     * @param obj
     * @return
     * @throws IllegalAccessException
     */
    public static Map<String, Object> mapAddObj(Map<String, Object> map,Object obj) throws IllegalAccessException {

        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object value = field.get(obj);
            map.put(fieldName, value);
        }
        return map;
    }

    //替换最后一个字符为小写
    public static String lowerLast(CharSequence str) {
        if (null == str) {
            return null;
        } else {
            if (str.length() > 0) {
                char lastChar = str.charAt(str.length()-1);
                if (Character.isUpperCase(lastChar)) {
                    return    StrUtil.sub(str, 0,str.length()-1)+Character.toLowerCase(lastChar);
                }
            }

            return str.toString();
        }
    }
}
