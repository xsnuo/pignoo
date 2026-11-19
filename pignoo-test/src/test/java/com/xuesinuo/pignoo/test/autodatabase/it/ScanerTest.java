package com.xuesinuo.pignoo.test.autodatabase.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.autodatabase.EntityScanConfig;
import com.xuesinuo.pignoo.autodatabase.EntityScaner;
import com.xuesinuo.pignoo.autodatabase.exception.ScanException;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.exception.DataSourceException;
import com.xuesinuo.pignoo.test.autodatabase.scanerentity.annotation.AdscAnn;
import com.xuesinuo.pignoo.test.autodatabase.scanerentity.breaking.AdscBreak;
import com.xuesinuo.pignoo.test.autodatabase.scanerentity.child.AdscChild;
import com.xuesinuo.pignoo.test.autodatabase.scanerentity.child.sub.AdscChildSub;
import com.xuesinuo.pignoo.test.autodatabase.scanerentity.filter.AdscFilter;
import com.xuesinuo.pignoo.test.autodatabase.scanerentity.mode.AdscMode;

/**
 * {@link EntityScaner} 的集成用例：真实执行 DDL，逐级钉死 {@link EntityScanConfig.BuildMode} 到底做了什么
 * <p>
 * Integration cases for {@link EntityScaner}: DDL is really executed, pinning down what every
 * {@link EntityScanConfig.BuildMode} level actually does
 * <p>
 * 用例只操作自建的 {@code adsc_} 前缀临时表，实体放在 {@code ..autodatabase.scanerentity} 的各个子包里，
 * 每个用例独占一个包，保证扫描范围不会误伤其它用例
 * <p>
 * Only self-built {@code adsc_} prefixed temp tables are touched; entities live in dedicated subpackages
 * under {@code ..autodatabase.scanerentity}, one package per case, so the scan range stays isolated
 */
@SpringBootTest
public class ScanerTest {

    public final DataSource dataSource;

    public ScanerTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @AfterEach
    public void dropTempTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (var con = dataSource.getConnection();
                var ps = con.prepareStatement("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 'adsc_%'");
                var rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        for (String table : tables) {
            ddl("DROP TABLE IF EXISTS `" + table + "`");
        }
    }

    private void ddl(String... sqls) throws SQLException {
        try (var con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            for (String sql : sqls) {
                con.createStatement().execute(sql);
            }
            con.commit();
        }
    }

    /**
     * 表的列名到列类型，按列顺序排列；表不存在时返回空集合
     * <p>
     * Column name to column type in ordinal order; empty when the table does not exist
     */
    private Map<String, String> columns(String table) throws SQLException {
        Map<String, String> columns = new LinkedHashMap<>();
        try (var con = dataSource.getConnection();
                var ps = con.prepareStatement("SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION")) {
            ps.setString(1, table);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.put(rs.getString(1), rs.getString(2).toLowerCase());
                }
            }
        }
        return columns;
    }

    private long rowCount(String table) throws SQLException {
        try (var con = dataSource.getConnection();
                var ps = con.prepareStatement("SELECT COUNT(*) FROM `" + table + "`");
                var rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void scan(Class<?> anchor, EntityScanConfig.BuildMode buildMode, boolean breakRunning, boolean annotationClassOnly) {
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setBuildMode(buildMode);
        entityScanConfig.setBreakRunning(breakRunning);
        entityScanConfig.setAnnotationClassOnly(annotationClassOnly);
        entityScanConfig.setClassesForScanPackage(new Class[] { anchor });
        new EntityScaner(dataSource, new PignooConfig(), entityScanConfig).scan();
    }

    /**
     * BuildMode 四级共用的初始结构：adsc_mode_change 少一列（extra）、多一列（legacy_col）、
     * 有一列类型不符（qty），adsc_mode_create 整表缺失，四种建议一次凑齐
     * <p>
     * Shared fixture for the four BuildMode cases: adsc_mode_change misses one column, has one redundant
     * column and one type-mismatched column, while adsc_mode_create is missing entirely
     */
    private void resetModeFixture() throws SQLException {
        ddl("DROP TABLE IF EXISTS `adsc_mode_create`", "DROP TABLE IF EXISTS `adsc_mode_change`", """
                CREATE TABLE `adsc_mode_change` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `name` varchar(255) NULL,
                  `qty` varchar(255) NULL,
                  `legacy_col` varchar(255) NULL,
                  PRIMARY KEY (`id`)
                )
                """, "INSERT INTO `adsc_mode_change` (`name`, `qty`, `legacy_col`) VALUES ('n1', '7', 'legacy-data')");
    }

    @Test
    public void carefullyExecutesNothing() throws SQLException {
        resetModeFixture();

        scan(AdscMode.class, EntityScanConfig.BuildMode.CAREFULLY, false, true);

        assertThat(columns("adsc_mode_create")).as("谨慎模式不应建表").isEmpty();
        Map<String, String> changed = columns("adsc_mode_change");
        assertThat(changed.keySet()).as("谨慎模式既不应加列也不应删列，表结构应与扫描前完全一致")
                .containsExactlyInAnyOrder("id", "name", "qty", "legacy_col");
        assertThat(changed.get("qty")).as("谨慎模式不应改列类型").isEqualTo("varchar(255)");
    }

    @Test
    public void safelyExecutesAddOnly() throws SQLException {
        resetModeFixture();

        scan(AdscMode.class, EntityScanConfig.BuildMode.SAFELY, false, true);

        assertThat(columns("adsc_mode_create").keySet()).as("安全模式应建表").containsExactlyInAnyOrder("id", "name");
        Map<String, String> changed = columns("adsc_mode_change");
        assertThat(changed).as("安全模式应加上实体里多出的列").containsKey("extra");
        assertThat(changed.get("extra")).as("加列应按实体字段类型建").isEqualTo("varchar(255)");
        assertThat(changed).as("安全模式不应删列").containsKey("legacy_col");
        assertThat(changed.get("qty")).as("安全模式不应改列类型").isEqualTo("varchar(255)");
        assertThat(changed.keySet()).as("安全模式除了加列不应有别的改动")
                .containsExactlyInAnyOrder("id", "name", "qty", "legacy_col", "extra");
    }

    @Test
    public void usabilityExecutesAddAndUpdate() throws SQLException {
        resetModeFixture();

        scan(AdscMode.class, EntityScanConfig.BuildMode.USABILITY, false, true);

        assertThat(columns("adsc_mode_create").keySet()).as("易用模式应建表").containsExactlyInAnyOrder("id", "name");
        Map<String, String> changed = columns("adsc_mode_change");
        assertThat(changed).as("易用模式应加列").containsKey("extra");
        assertThat(changed.get("qty")).as("易用模式应把类型不符的列改成 int").startsWith("int");
        assertThat(changed).as("易用模式不应删列").containsKey("legacy_col");
        assertThat(changed.keySet()).as("易用模式除了加列和改列不应有别的改动")
                .containsExactlyInAnyOrder("id", "name", "qty", "legacy_col", "extra");
    }

    @Test
    public void radicallyExecutesAllIncludingDropColumn() throws SQLException {
        resetModeFixture();
        assertThat(columns("adsc_mode_change")).as("扫描前 legacy_col 应当存在").containsKey("legacy_col");

        assertThatCode(() -> scan(AdscMode.class, EntityScanConfig.BuildMode.RADICALLY, true, true))
                .as("激进模式下所有问题都能自动处理，即使 breakRunning=true 也不应被打断")
                .doesNotThrowAnyException();

        assertThat(columns("adsc_mode_create").keySet()).as("激进模式应建表").containsExactlyInAnyOrder("id", "name");
        Map<String, String> changed = columns("adsc_mode_change");
        assertThat(changed).as("激进模式应加列").containsKey("extra");
        assertThat(changed.get("qty")).as("激进模式应把类型不符的列改成 int").startsWith("int");
        assertThat(changed).as("激进模式应删掉实体里没有的列，这一列上的数据会随之丢失").doesNotContainKey("legacy_col");
        assertThat(changed.keySet()).as("激进模式执行完，表结构应与实体完全一致")
                .containsExactlyInAnyOrder("id", "name", "qty", "extra");
        assertThat(rowCount("adsc_mode_change")).as("删列只丢该列的数据，不应丢掉整行").isEqualTo(1);
    }

    @Test
    public void breakRunningTrueThrowsAndExecutesNothing() throws SQLException {
        ddl("DROP TABLE IF EXISTS `adsc_break_bad`", "DROP TABLE IF EXISTS `adsc_break_good`");

        assertThatThrownBy(() -> scan(AdscBreak.class, EntityScanConfig.BuildMode.RADICALLY, true, true),
                "存在无法自动处理的问题且 breakRunning=true 时应抛出 ScanException")
                .isInstanceOf(ScanException.class)
                .hasMessageContaining("Database check failed");

        assertThat(columns("adsc_break_bad")).as("含无法映射字段的实体不应被建表").isEmpty();
        assertThat(columns("adsc_break_good")).as("打断运行时，同一批次里本可执行的建议也不应被执行").isEmpty();
    }

    @Test
    public void breakRunningFalseContinues() throws SQLException {
        ddl("DROP TABLE IF EXISTS `adsc_break_bad`", "DROP TABLE IF EXISTS `adsc_break_good`");

        assertThatCode(() -> scan(AdscBreak.class, EntityScanConfig.BuildMode.RADICALLY, false, true))
                .as("存在无法自动处理的问题但 breakRunning=false 时不应抛异常")
                .doesNotThrowAnyException();

        assertThat(columns("adsc_break_bad")).as("无法映射的字段修不了，该实体仍不应被建表").isEmpty();
        assertThat(columns("adsc_break_good").keySet()).as("breakRunning=false 应继续把可执行的建议执行掉")
                .containsExactlyInAnyOrder("id", "name");
    }

    @Test
    public void annotationClassOnlyScansAnnotatedClassOnly() throws SQLException {
        ddl("DROP TABLE IF EXISTS `adsc_ann_marked`", "DROP TABLE IF EXISTS `adsc_ann_plain`");

        scan(AdscAnn.class, EntityScanConfig.BuildMode.RADICALLY, false, true);

        assertThat(columns("adsc_ann_marked").keySet()).as("annotationClassOnly=true 时带 @Table 的类应被扫到")
                .containsExactlyInAnyOrder("id", "name");
        assertThat(columns("adsc_ann_plain")).as("annotationClassOnly=true 时不带 @Table 的类不应被扫到").isEmpty();
    }

    @Test
    public void annotationClassOnlyFalseScansAllClasses() throws SQLException {
        ddl("DROP TABLE IF EXISTS `adsc_ann_marked`", "DROP TABLE IF EXISTS `adsc_ann_plain`");

        scan(AdscAnn.class, EntityScanConfig.BuildMode.RADICALLY, false, false);

        assertThat(columns("adsc_ann_marked").keySet()).as("annotationClassOnly=false 时带 @Table 的类同样应被扫到")
                .containsExactlyInAnyOrder("id", "name");
        assertThat(columns("adsc_ann_plain").keySet()).as("annotationClassOnly=false 时不带 @Table 的类也应被扫到，表名按驼峰转下划线推导")
                .containsExactlyInAnyOrder("id", "name");
    }

    @Test
    public void onlyPublicTopLevelAndPublicStaticInnerClassScanned() throws SQLException {
        ddl("DROP TABLE IF EXISTS `adsc_filter_top`", "DROP TABLE IF EXISTS `adsc_filter_static`",
                "DROP TABLE IF EXISTS `adsc_filter_member`", "DROP TABLE IF EXISTS `adsc_filter_package`");

        scan(AdscFilter.class, EntityScanConfig.BuildMode.RADICALLY, false, true);

        assertThat(columns("adsc_filter_top").keySet()).as("public 顶层类应被识别").containsExactlyInAnyOrder("id", "name");
        assertThat(columns("adsc_filter_static").keySet()).as("public 静态内部类应被识别").containsExactlyInAnyOrder("id", "name");
        assertThat(columns("adsc_filter_member")).as("非静态内部类不应被识别").isEmpty();
        assertThat(columns("adsc_filter_package")).as("非 public 的顶层类不应被识别").isEmpty();
    }

    @Test
    public void emptyScanTargetThrowsDataSourceException() {
        EntityScanConfig emptyConfig = new EntityScanConfig();
        assertThatThrownBy(() -> new EntityScaner(dataSource, new PignooConfig(), emptyConfig).scan(),
                "packages 与 classesForScanPackage 都为空数组时应抛出 DataSourceException")
                .isInstanceOf(DataSourceException.class)
                .hasMessageContaining("Packages empty");

        EntityScanConfig nullConfig = new EntityScanConfig();
        nullConfig.setPackages(null);
        nullConfig.setClassesForScanPackage(null);
        assertThatThrownBy(() -> new EntityScaner(dataSource, new PignooConfig(), nullConfig).scan(),
                "packages 与 classesForScanPackage 都为 null 时应抛出 DataSourceException")
                .isInstanceOf(DataSourceException.class)
                .hasMessageContaining("Packages empty");

        EntityScanConfig blankConfig = new EntityScanConfig();
        blankConfig.setPackages(new String[] { "  " });
        assertThatThrownBy(() -> new EntityScaner(dataSource, new PignooConfig(), blankConfig).scan(),
                "packages 只有空白串时应被视为空，抛出 DataSourceException")
                .isInstanceOf(DataSourceException.class)
                .hasMessageContaining("Packages empty");
    }

    /**
     * scanChildPackages 用例专用的扫描：其余开关固定，只留递归开关作为变量
     * <p>
     * Dedicated scan for the scanChildPackages cases: every other switch is fixed so the recursion flag
     * is the only variable
     */
    private void scanRecursively(Class<?> anchor, boolean scanChildPackages) {
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setBuildMode(EntityScanConfig.BuildMode.RADICALLY);
        entityScanConfig.setBreakRunning(false);
        entityScanConfig.setAnnotationClassOnly(true);
        entityScanConfig.setScanChildPackages(scanChildPackages);
        entityScanConfig.setClassesForScanPackage(new Class[] { anchor });
        new EntityScaner(dataSource, new PignooConfig(), entityScanConfig).scan();
    }

    @Test
    public void scanChildPackagesTrueScansSubPackage() throws SQLException {
        ddl("DROP TABLE IF EXISTS `adsc_child_parent`", "DROP TABLE IF EXISTS `adsc_child_sub`");

        scanRecursively(AdscChild.class, true);

        assertThat(columns("adsc_child_parent").keySet()).as("锚点包自身的实体应被扫到").containsExactlyInAnyOrder("id", "name");
        assertThat(columns("adsc_child_sub").keySet()).as("scanChildPackages=true 时子包里的实体也应被扫到")
                .containsExactlyInAnyOrder("id", "name");
    }

    @Test
    public void scanChildPackagesFalseSkipsSubPackage() throws SQLException {
        ddl("DROP TABLE IF EXISTS `adsc_child_parent`", "DROP TABLE IF EXISTS `adsc_child_sub`");

        scanRecursively(AdscChild.class, false);

        assertThat(columns("adsc_child_parent").keySet()).as("scanChildPackages=false 时锚点包自身的实体仍应被扫到")
                .containsExactlyInAnyOrder("id", "name");
        assertThat(columns("adsc_child_sub")).as("scanChildPackages=false 时子包里的实体不应被扫到").isEmpty();

        scanRecursively(AdscChildSub.class, false);

        assertThat(columns("adsc_child_sub").keySet()).as("同一个实体直接以子包为锚点就能被扫到，上一步扫不到只可能是递归开关的作用")
                .containsExactlyInAnyOrder("id", "name");
    }
}
