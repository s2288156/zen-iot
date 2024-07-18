import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */
packageName = "org.zen.iot.dao.model;"  //这里要换成自己项目 实体的包路径
typeMapping = [
        (~/(?i)int/)               : "Integer",  //数据库类型和Jave类型映射关系
        (~/(?i)float|double|real/) : "Double",
        (~/(?i)decimal/)           : "Money",
        (~/(?i)bool|boolean/)      : "Boolean",
        (~/(?i)datetime|timestamp/): "LocalDateTime",
        (~/(?i)date/)              : "LocalDate",
        (~/(?i)/)                  : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
}

def generate(table, dir) {
    // ====================================== 文件名称
    def className = javaName(table.getName().drop(1), true)
    def fields = calcFields(table)
    new File(dir, className + "Entity" + ".java").withPrintWriter("utf-8") { out -> generate(out, table, className, fields) }
}

def generate(out, table, className, fields) {
    // ====================================== 表名称
    def tableName = table.getName()
    // ====================================== className
    def cName = className - "T" + "Entity"
    out.println "package $packageName"
    out.println ""
    out.println "import jakarta.persistence.*;"
    out.println "import lombok.*;"
    out.println ""

    for (it in fields) {
        if (isNotBaseTime(it) && it.type == "LocalDate") {
            out.println "import java.time.LocalDate;"
            break
        }
    }
    for (it in fields) {
        if (isNotBaseTime(it) && it.type == "LocalDateTime") {
            out.println "import java.time.LocalDateTime;"
            break
        }
    }

    out.println ""
    out.println "/**"
    out.println " * @author wcy-auto"
    out.println " **/"
    out.println "@NoArgsConstructor"
    out.println "@AllArgsConstructor"
    out.println "@Getter"
    out.println "@Setter"
    out.println "@ToString"
    out.println "@Entity"
    out.println "@Table(name = \"$tableName\")"
    for (it in fields) {
        if (it.type == "Money") {
            out.println "@TableName(autoResultMap = true)"
            break
        }
    }
    out.println "public class $cName extends BaseEntity {"
    out.println ""

    /*if ((tableName + "_id").equalsIgnoreCase(fields[0].colum) || "id".equalsIgnoreCase(fields[0].colum)) {
        out.println "    @Id"
        out.println "    @GeneratedValue(strategy=GenerationType.IDENTITY)"
    }*/

    fields.each() {
        if (it.name != "id" && it.name != "createDate" && it.name != "updateDate" && it.name != "createTime" && it.name != "updateTime") {
            if (it.comment != "" && it.comment != null) {
                out.println "    /**"
                out.println "     * ${it.comment}"
                out.println "     **/"
            }
            if (it.annos != "") out.println "  ${it.annos}"
            out.println "    @Column(name = \"${it.colum}\")"
            if (it.type == "Money") {
                out.println "    @TableField(typeHandler = MoneyTypeHandler.class)"
            }

            out.println "    private ${it.type} ${it.name};"
            out.println ""
        }
    }
    out.println "}"
}

def isNotBaseTime(it) {
    return it.name != "createDate" && it.name != "updateDate" && it.name != "createTime" && it.name != "updateTime"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name   : javaName(col.getName(), false),
                           colum  : col.getName(),
                           type   : typeStr,
                           comment: col.getComment(),
                           annos  : ""]]
    }
}

def javaName(str, capitalize) {
    def s = str.split(/(?<=[^\p{IsLetter}])/).collect { Case.LOWER.apply(it).capitalize() }
            .join("").replaceAll(/[^\p{javaJavaIdentifierPart}]/, "_").replaceAll(/_/, "")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
