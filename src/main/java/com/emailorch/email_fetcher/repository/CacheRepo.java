package com.emailorch.email_fetcher.repository;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

@Repository
public class CacheRepo {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public CacheRepo(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public void saveStream(String id, InputStream in) {
        String sql = "INSERT INTO cache (id, content) VALUES (?, ?)";

      jdbcTemplate.execute(sql,(PreparedStatement ps)->{
          ps.setString(1,id);
          ps.setBinaryStream(2,in);
          return ps.executeUpdate();
      });
    }
    public Long getDataLength(String id) {
        // OCTET_LENGTH returns the size of the binary data in bytes
        String sql = "SELECT OCTET_LENGTH(content) FROM cache WHERE id = ?";

        try {
            return jdbcTemplate.queryForObject(sql,Long.class,id);
        } catch (EmptyResultDataAccessException e) {
            return 0L; // ID not found in cache
        }
    }
    public void returnUploadStream(String id , Consumer<InputStream> consumer) throws SQLException {
        String sql = "SELECT content FROM cache WHERE id = ?";
        byte[] data = jdbcTemplate.queryForObject(sql,
                (rs, rowNum) -> rs.getBytes("content"), id);

        try (InputStream in = new ByteArrayInputStream(data)) {
            consumer.accept(in);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        jdbcTemplate.execute("delete from public.cache");
    }
}
