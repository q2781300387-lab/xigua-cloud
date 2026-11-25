package com.xigua.admin.controller;

import com.xigua.common.core.base.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName AdminBehaviorController
 * @Description 管理端异常行为监测：高频发言 / 深夜消息 / 敏感词消息 / 超长消息
 * @Author xigua
 * @Date 2026/10/16 19:30
 */
@Slf4j
@Tag(name = "管理端异常行为监测接口")
@RequestMapping("/behavior/anomalies")
@RestController
public class AdminBehaviorController {

    @Autowired
    private DataSource dataSource;

    private static final String[] SENSITIVE_WORDS = {
            "转账", "兼职", "刷单", "赌博", "中奖", "加微信", "色情", "贷款", "代考", "枪手"
    };

    @Operation(summary = "异常行为监测总览")
    @GetMapping
    public R<Map<String, Object>> anomalies() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            // 1. 近24小时消息总数 / 活跃用户数
            result.put("total24h", queryLong(conn, "SELECT COUNT(*) FROM xg_chat_message WHERE del_flag=0 AND create_time > NOW() - INTERVAL 24 HOUR"));
            result.put("activeUsers24h", queryLong(conn, "SELECT COUNT(DISTINCT sender_id) FROM xg_chat_message WHERE del_flag=0 AND create_time > NOW() - INTERVAL 24 HOUR"));

            // 2. 深夜消息数（23:00-05:00）
            result.put("nightMsgs24h", queryLong(conn,
                    "SELECT COUNT(*) FROM xg_chat_message WHERE del_flag=0 AND create_time > NOW() - INTERVAL 24 HOUR AND (HOUR(create_time) >= 23 OR HOUR(create_time) < 5)"));

            // 3. 近1小时高频发言 TOP5
            result.put("highFreq", queryHighFreq(conn));

            // 4. 敏感词消息
            result.put("sensitiveMsgs", querySensitive(conn));

            // 5. 超长消息（>300字）
            result.put("longMsgs", queryLongMsgs(conn));
        } catch (Exception e) {
            log.error("查询异常行为监测失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
        return R.ok(result, "查询成功");
    }

    private long queryLong(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private List<Map<String, Object>> queryHighFreq(Connection conn) throws Exception {
        String sql = "SELECT sender_id, COUNT(*) cnt FROM xg_chat_message " +
                "WHERE del_flag=0 AND create_time > NOW() - INTERVAL 1 HOUR " +
                "GROUP BY sender_id ORDER BY cnt DESC LIMIT 5";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("senderId", rs.getString("sender_id"));
                row.put("count", rs.getInt("cnt"));
                list.add(row);
            }
        }
        return list;
    }

    private List<Map<String, Object>> querySensitive(Connection conn) throws Exception {
        StringBuilder sb = new StringBuilder(
                "SELECT id, sender_id, message, create_time FROM xg_chat_message WHERE del_flag=0 AND (");
        for (int i = 0; i < SENSITIVE_WORDS.length; i++) {
            if (i > 0) sb.append(" OR ");
            sb.append("message LIKE ?");
        }
        sb.append(") ORDER BY create_time DESC LIMIT 10");
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < SENSITIVE_WORDS.length; i++) {
                ps.setString(i + 1, "%" + SENSITIVE_WORDS[i] + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getString("id"));
                    row.put("senderId", rs.getString("sender_id"));
                    row.put("message", rs.getString("message"));
                    row.put("createTime", rs.getString("create_time"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    private List<Map<String, Object>> queryLongMsgs(Connection conn) throws Exception {
        String sql = "SELECT id, sender_id, message, create_time FROM xg_chat_message " +
                "WHERE del_flag=0 AND CHAR_LENGTH(message) > 300 ORDER BY create_time DESC LIMIT 5";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getString("id"));
                row.put("senderId", rs.getString("sender_id"));
                String msg = rs.getString("message");
                row.put("message", msg != null && msg.length() > 80 ? msg.substring(0, 80) + "…" : msg);
                row.put("createTime", rs.getString("create_time"));
                list.add(row);
            }
        }
        return list;
    }
}
